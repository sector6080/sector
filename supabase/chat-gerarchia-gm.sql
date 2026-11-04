-- Chat a canali con moderazione, gerarchia con squadroni e votazioni, nemici e strumenti GM.
-- Da eseguire una volta sola e per intero, dopo matricola-e-gradi.sql.
-- Dopo lo script le versioni precedenti dell'app non funzionano più.

begin;

alter table public.players
    add column rank_since timestamptz not null default now(),
    add column muted boolean not null default false;

-- Squadrone: punta all'unità del Caporale che lo guida
alter table public.game_units
    add column squad_of uuid references public.game_units (id) on delete set null;

alter table public.chat_messages
    add column channel text not null default 'comune'
        check (channel in ('eventi', 'comune', 'squadrone', 'comando')),
    add column squad_id uuid references public.game_units (id) on delete set null;

-- Proposte di cambio grado, votate dai Capitani
create table public.rank_proposals (
    id bigint generated always as identity primary key,
    unit_id uuid not null references public.game_units (id) on delete cascade,
    from_rank text not null,
    new_rank text not null,
    proposed_by uuid references public.game_units (id) on delete set null,
    status text not null default 'open' check (status in ('open', 'approved', 'rejected', 'closed')),
    created_at timestamptz not null default now(),
    closed_at timestamptz
);

create unique index one_open_proposal_per_unit on public.rank_proposals (unit_id) where status = 'open';

create table public.rank_votes (
    proposal_id bigint not null references public.rank_proposals (id) on delete cascade,
    voter_id uuid not null references public.players (id) on delete cascade,
    approve boolean not null,
    primary key (proposal_id, voter_id)
);

create table public.game_enemies (
    id bigint generated always as identity primary key,
    name text not null check (char_length(name) between 1 and 40),
    hp integer not null check (hp > 0),
    armor integer not null default 0 check (armor >= 0),
    description text not null default '' check (char_length(description) <= 500),
    pos_x integer not null check (pos_x between 1 and 80),
    pos_y integer not null check (pos_y between 1 and 60)
);

-- Queste tabelle si leggono e si scrivono solo con le funzioni qui sotto
alter table public.rank_proposals enable row level security;
alter table public.rank_votes enable row level security;
alter table public.game_enemies enable row level security;
revoke all on public.rank_proposals, public.rank_votes, public.game_enemies from anon, authenticated;

-- I canali privati non si leggono più direttamente: tutto passa da get_chat
drop policy "Tutti leggono la chat" on public.chat_messages;
revoke all on public.chat_messages from anon, authenticated;

create function public.my_rank() returns text
language sql stable security definer set search_path = '' as $$
    select rank from public.players where id = auth.uid();
$$;

-- Il Caporale guida il proprio squadrone, il Soldato sta in quello di chi lo ha scelto
create function public.my_squad() returns uuid
language sql stable security definer set search_path = '' as $$
    select case when p.rank = 'Caporale' then u.id else u.squad_of end
    from public.game_units u
    join public.players p on p.id = u.owner_id
    where u.owner_id = auth.uid() and u.status = 'active';
$$;

-- Ogni cambio di grado aggiorna la data, gli squadroni e chiude le proposte aperte
create function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    new.rank_since := now();

    if old.rank = 'Caporale' then
        update public.game_units set squad_of = null
            where squad_of in (select id from public.game_units where owner_id = new.id);
    end if;
    if new.rank <> 'Soldato' then
        update public.game_units set squad_of = null where owner_id = new.id;
    end if;

    update public.rank_proposals set status = 'closed', closed_at = now()
        where status = 'open'
        and unit_id in (select id from public.game_units where owner_id = new.id);
    return new;
end;
$$;

create trigger on_rank_change before update of rank on public.players
    for each row when (old.rank is distinct from new.rank)
    execute function public.handle_rank_change();

-- Costituzione, art. 1: un grado alla volta, e 6 giorni reali da Caporale prima di Capitano
create function public.check_rank_change(p_from text, p_since timestamptz, p_to text) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if (p_from, p_to) not in (
        ('Soldato', 'Caporale'), ('Caporale', 'Capitano'),
        ('Capitano', 'Caporale'), ('Caporale', 'Soldato')
    ) then
        raise exception 'Cambio di grado non valido';
    end if;
    if p_to = 'Capitano' and p_since > now() - interval '6 days' then
        raise exception 'Servono 6 giorni da Caporale';
    end if;
end;
$$;

-- Votano i Capitani, tranne chi è oggetto della proposta. Serve il 50%+1.
create function public.vote_rank(p_id bigint, p_approve boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_proposal public.rank_proposals;
    v_owner uuid;
    v_voters integer;
    v_quorum integer;
    v_yes integer;
    v_no integer;
begin
    if public.my_rank() is distinct from 'Capitano' then raise exception 'Solo i Capitani'; end if;

    select * into v_proposal from public.rank_proposals where id = p_id and status = 'open' for update;
    if v_proposal.id is null then raise exception 'Proposta chiusa'; end if;

    select owner_id into v_owner from public.game_units where id = v_proposal.unit_id;
    if v_owner = auth.uid() then raise exception 'Non puoi votare su te stesso'; end if;

    insert into public.rank_votes (proposal_id, voter_id, approve) values (p_id, auth.uid(), p_approve)
        on conflict (proposal_id, voter_id) do update set approve = excluded.approve;

    select count(*) into v_voters from public.players where rank = 'Capitano' and id <> v_owner;
    v_quorum := v_voters / 2 + 1;

    select count(*) filter (where v.approve), count(*) filter (where not v.approve)
        into v_yes, v_no
        from public.rank_votes v
        join public.players p on p.id = v.voter_id
        where v.proposal_id = p_id and p.rank = 'Capitano';

    if v_yes >= v_quorum then
        update public.rank_proposals set status = 'approved', closed_at = now() where id = p_id;
        update public.players set rank = v_proposal.new_rank where id = v_owner;
    elsif v_no > v_voters - v_quorum then
        update public.rank_proposals set status = 'rejected', closed_at = now() where id = p_id;
    end if;
end;
$$;

-- Promozioni e declassamenti partono da un Capitano, che vota subito a favore
create function public.propose_rank(p_matricola text, p_new_rank text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_rank text;
    v_since timestamptz;
    v_mine uuid;
    v_id bigint;
begin
    if public.my_rank() is distinct from 'Capitano' then raise exception 'Solo i Capitani'; end if;

    select * into v_unit from public.game_units where matricola = p_matricola and status = 'active';
    if v_unit.id is null then raise exception 'Unita non attiva'; end if;
    if v_unit.owner_id = auth.uid() then raise exception 'Non puoi proporre te stesso'; end if;

    select rank, rank_since into v_rank, v_since from public.players where id = v_unit.owner_id;
    perform public.check_rank_change(v_rank, v_since, p_new_rank);

    select id into v_mine from public.game_units where owner_id = auth.uid() and status = 'active';
    insert into public.rank_proposals (unit_id, from_rank, new_rank, proposed_by)
        values (v_unit.id, v_rank, p_new_rank, v_mine)
        returning id into v_id;
    perform public.vote_rank(v_id, true);
end;
$$;

create function public.get_proposals() returns table (
    id bigint, matricola text, from_rank text, new_rank text, proposed_by text,
    votes_yes integer, votes_no integer, my_vote boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() and public.my_rank() is distinct from 'Capitano' then return; end if;
    return query
        select r.id, u.matricola, r.from_rank, r.new_rank, b.matricola,
               (select count(*)::integer from public.rank_votes v where v.proposal_id = r.id and v.approve),
               (select count(*)::integer from public.rank_votes v where v.proposal_id = r.id and not v.approve),
               (select v.approve from public.rank_votes v where v.proposal_id = r.id and v.voter_id = auth.uid())
        from public.rank_proposals r
        join public.game_units u on u.id = r.unit_id
        left join public.game_units b on b.id = r.proposed_by
        where r.status = 'open'
        order by r.id;
end;
$$;

-- Il Caporale sceglie fino a 6 Soldati liberi
create function public.set_squad_member(p_matricola text, p_member boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_leader uuid;
    v_target public.game_units;
begin
    if public.my_rank() is distinct from 'Caporale' then raise exception 'Solo i Caporali'; end if;
    v_leader := public.my_squad();
    perform 1 from public.game_units where id = v_leader for update;

    select * into v_target from public.game_units
        where matricola = p_matricola and status = 'active' for update;
    if v_target.id is null then raise exception 'Unita non attiva'; end if;

    if not p_member then
        update public.game_units set squad_of = null where id = v_target.id and squad_of = v_leader;
        return;
    end if;

    if (select rank from public.players where id = v_target.owner_id) <> 'Soldato' then
        raise exception 'Solo i Soldati';
    end if;
    if v_target.squad_of is not null then raise exception 'Soldato gia in uno squadrone'; end if;
    if (select count(*) from public.game_units where squad_of = v_leader and status = 'active') >= 6 then
        raise exception 'Squadrone al completo';
    end if;
    update public.game_units set squad_of = v_leader where id = v_target.id;
end;
$$;

-- Ultimi 100 messaggi per canale, solo dei canali che il player può leggere
create function public.get_chat() returns table (
    id bigint, channel text, author text, body text, sent_at timestamptz
)
language plpgsql stable security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_rank text := public.my_rank();
    v_squad uuid := public.my_squad();
begin
    return query
        select m.id, m.channel, m.author, m.body, m.sent_at
        from (
            select c.id, c.channel, c.author, c.body, c.sent_at,
                   row_number() over (partition by c.channel order by c.id desc) as newest
            from public.chat_messages c
            where c.channel in ('eventi', 'comune')
                or v_gm
                or (c.channel = 'comando' and v_rank in ('Capitano', 'Caporale'))
                or (c.channel = 'squadrone' and c.squad_id = v_squad)
        ) m
        where m.newest <= 100
        order by m.id;
end;
$$;

drop function public.send_chat(text);

create function public.send_chat(p_channel text, p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_rank text;
    v_muted boolean;
    v_unit uuid;
    v_author text;
    v_squad uuid;
begin
    select rank, muted into v_rank, v_muted from public.players where id = auth.uid();
    if v_muted then raise exception 'Sei stato silenziato'; end if;

    select u.id, v_rank || ' - ' || u.matricola || ' - ' || u.type into v_unit, v_author
        from public.game_units u
        where u.owner_id = auth.uid() and u.status in ('pending', 'active');
    if v_gm then v_author := 'Comando Supremo'; end if;
    if v_author is null then raise exception 'Serve una unita per scrivere'; end if;

    if p_channel = 'eventi' and not v_gm then raise exception 'Canale non disponibile'; end if;
    if p_channel = 'comando' and not v_gm and v_rank not in ('Capitano', 'Caporale') then
        raise exception 'Canale non disponibile';
    end if;
    if p_channel = 'squadrone' then
        v_squad := public.my_squad();
        if v_squad is null then raise exception 'Canale non disponibile'; end if;
    end if;

    if exists (
        select 1 from public.chat_messages
        where author = v_author and sent_at > now() - interval '2 seconds'
    ) then
        raise exception 'Messaggi troppo ravvicinati';
    end if;

    insert into public.chat_messages (unit_id, channel, squad_id, author, body)
        values (v_unit, p_channel, v_squad, v_author, trim(p_body));
end;
$$;

-- Squadra con lo squadrone di ogni unità
drop function public.get_team();
drop function public.gm_units();

create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, squad text
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, l.matricola
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.rank_salaries r on r.rank = p.rank
    left join public.game_units l on l.id = u.squad_of
    where u.status in ('pending', 'active')
    order by r.position desc nulls last, u.matricola;
$$;

create function public.gm_units() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, cost integer, squad text, muted boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, u.cost,
               l.matricola, p.muted
        from public.game_units u
        join public.players p on p.id = u.owner_id
        left join public.game_units l on l.id = u.squad_of
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

-- I nemici arrivano al telefono solo se dentro la visione comune. Il GM li vede tutti.
create function public.get_enemies() returns table (
    id bigint, name text, hp integer, armor integer, description text, pos_x integer, pos_y integer
)
language plpgsql stable security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
begin
    return query
        select e.id, e.name, e.hp, e.armor, e.description, e.pos_x, e.pos_y
        from public.game_enemies e
        where v_gm or exists (
            select 1
            from public.game_units u
            join public.unit_types t on t.name = u.type
            where u.status = 'active'
            and abs(u.pos_x - e.pos_x) + abs(u.pos_y - e.pos_y) <= greatest(t.vision + coalesce((
                select sum(c.vision) from public.game_components c
                where c.type = u.type and c.name = any(u.components)
            ), 0), 0)
        )
        order by e.id;
end;
$$;

-- Ordine diretto del Comando Supremo
create function public.gm_set_rank(p_matricola text, p_rank text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_owner uuid;
    v_rank text;
    v_since timestamptz;
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;

    select u.owner_id, p.rank, p.rank_since into v_owner, v_rank, v_since
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.matricola = p_matricola and u.status = 'active';
    if v_owner is null then raise exception 'Unita non attiva'; end if;

    perform public.check_rank_change(v_rank, v_since, p_rank);
    update public.players set rank = p_rank where id = v_owner;
end;
$$;

create function public.gm_delete_message(p_id bigint) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    delete from public.chat_messages where id = p_id;
end;
$$;

-- Il silenzio resta sul player, anche se crea un'altra unità
create function public.gm_set_muted(p_matricola text, p_muted boolean) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    update public.players p set muted = p_muted
        from public.game_units u
        where u.owner_id = p.id and u.matricola = p_matricola;
end;
$$;

create function public.gm_move_unit(p_matricola text, p_x integer, p_y integer) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    if exists (
        select 1 from public.game_units u
        where u.pos_x = p_x and u.pos_y = p_y and u.status = 'active' and u.matricola <> p_matricola
    ) or exists (
        select 1 from public.game_enemies e where e.pos_x = p_x and e.pos_y = p_y
    ) then
        raise exception 'Casella occupata';
    end if;
    update public.game_units set pos_x = p_x, pos_y = p_y
        where matricola = p_matricola and status in ('pending', 'active');
end;
$$;

-- Aggiunge o toglie un componente gratis, aggiornando utilizzi e statistiche
create function public.gm_set_component(p_matricola text, p_name text, p_owned boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_component public.game_components;
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;

    select * into v_unit from public.game_units
        where matricola = p_matricola and status in ('pending', 'active') for update;
    if v_unit.id is null then raise exception 'Unita non attiva'; end if;

    select * into v_component from public.game_components where type = v_unit.type and name = p_name;
    if v_component.name is null then raise exception 'Componente non disponibile'; end if;

    if p_owned and not p_name = any(v_unit.components) then
        update public.game_units set
            components = components || p_name,
            hp = greatest(hp + v_component.hp, 1),
            supplies = greatest(supplies + v_component.supplies, 0),
            uses = case when v_component.max_uses is null then uses
                else uses || jsonb_build_object(p_name, v_component.max_uses) end
            where id = v_unit.id;
    elsif not p_owned and p_name = any(v_unit.components) then
        update public.game_units set
            components = array_remove(components, p_name),
            hp = greatest(hp - v_component.hp, 1),
            supplies = greatest(supplies - v_component.supplies, 0),
            uses = uses - p_name
            where id = v_unit.id;
    end if;
end;
$$;

-- Senza id crea un nemico. A 0 HP viene distrutto e tolto dalla mappa.
create function public.gm_save_enemy(
    p_id bigint, p_name text, p_hp integer, p_armor integer, p_description text, p_x integer, p_y integer
) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;

    if p_id is null then
        insert into public.game_enemies (name, hp, armor, description, pos_x, pos_y)
            values (trim(p_name), p_hp, p_armor, trim(p_description), p_x, p_y);
    elsif p_hp <= 0 then
        delete from public.game_enemies where id = p_id;
    else
        update public.game_enemies set name = trim(p_name), hp = p_hp, armor = p_armor,
            description = trim(p_description), pos_x = p_x, pos_y = p_y
            where id = p_id;
    end if;
end;
$$;

create function public.gm_delete_enemy(p_id bigint) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    delete from public.game_enemies where id = p_id;
end;
$$;

revoke all on function public.my_rank() from public, anon, authenticated;
revoke all on function public.my_squad() from public, anon, authenticated;
revoke all on function public.handle_rank_change() from public, anon, authenticated;
revoke all on function public.check_rank_change(text, timestamptz, text) from public, anon, authenticated;

revoke all on function public.get_chat() from public;
revoke all on function public.get_team() from public;
revoke all on function public.get_enemies() from public;
revoke all on function public.vote_rank(bigint, boolean) from public, anon;
revoke all on function public.propose_rank(text, text) from public, anon;
revoke all on function public.get_proposals() from public, anon;
revoke all on function public.set_squad_member(text, boolean) from public, anon;
revoke all on function public.send_chat(text, text) from public, anon;
revoke all on function public.gm_units() from public, anon;
revoke all on function public.gm_set_rank(text, text) from public, anon;
revoke all on function public.gm_delete_message(bigint) from public, anon;
revoke all on function public.gm_set_muted(text, boolean) from public, anon;
revoke all on function public.gm_move_unit(text, integer, integer) from public, anon;
revoke all on function public.gm_set_component(text, text, boolean) from public, anon;
revoke all on function public.gm_save_enemy(bigint, text, integer, integer, text, integer, integer) from public, anon;
revoke all on function public.gm_delete_enemy(bigint) from public, anon;

grant execute on function public.get_chat() to anon, authenticated;
grant execute on function public.get_team() to anon, authenticated;
grant execute on function public.get_enemies() to anon, authenticated;
grant execute on function public.vote_rank(bigint, boolean) to authenticated;
grant execute on function public.propose_rank(text, text) to authenticated;
grant execute on function public.get_proposals() to authenticated;
grant execute on function public.set_squad_member(text, boolean) to authenticated;
grant execute on function public.send_chat(text, text) to authenticated;
grant execute on function public.gm_units() to authenticated;
grant execute on function public.gm_set_rank(text, text) to authenticated;
grant execute on function public.gm_delete_message(bigint) to authenticated;
grant execute on function public.gm_set_muted(text, boolean) to authenticated;
grant execute on function public.gm_move_unit(text, integer, integer) to authenticated;
grant execute on function public.gm_set_component(text, text, boolean) to authenticated;
grant execute on function public.gm_save_enemy(bigint, text, integer, integer, text, integer, integer) to authenticated;
grant execute on function public.gm_delete_enemy(bigint) to authenticated;

commit;
