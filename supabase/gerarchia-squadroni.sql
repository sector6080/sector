-- Nuova gerarchia: il Caporale guida un gruppo di Soldati, il Capitano sceglie quali gruppi
-- far entrare nel suo squadrone. Il gruppo entra intero, con il suo Caporale.
-- Ogni unità punta a chi sta sopra di lei: Soldato -> Caporale -> Capitano.
-- Da eseguire una volta sola e per intero, dopo richieste-squadrone.sql.
-- Dopo lo script serve la nuova versione dell'app per gestire gruppi e squadroni.

begin;

update public.game_settings
    set description = 'Soldati massimi nel gruppo di un Caporale'
    where key = 'squad_limit_corporal';
update public.game_settings
    set description = 'Unità massime nello squadrone di un Capitano, Caporali e Soldati compresi'
    where key = 'squad_limit_captain';

-- Sotto un Capitano ora ci sono solo Caporali: i Soldati scelti direttamente tornano liberi
update public.game_units u set squad_of = null
    from public.game_units l
    join public.players p on p.id = l.owner_id
    where u.squad_of = l.id and p.rank = 'Capitano';

delete from public.squad_requests r
    using public.game_units l
    join public.players p on p.id = l.owner_id
    where r.leader_id = l.id and p.rank = 'Capitano';

-- Chat: Gruppo per il Caporale e i suoi Soldati, Squadrone per tutto lo squadrone del Capitano
alter table public.chat_messages drop constraint chat_messages_channel_check;
alter table public.chat_messages add constraint chat_messages_channel_check
    check (channel in ('eventi', 'comune', 'gruppo', 'squadrone', 'comando'));

update public.chat_messages c set channel = 'gruppo'
    from public.game_units l
    join public.players p on p.id = l.owner_id
    where c.channel = 'squadrone' and c.squad_id = l.id and p.rank <> 'Capitano';

create function public.my_unit() returns uuid
language sql stable security definer set search_path = '' as $$
    select id from public.game_units where owner_id = auth.uid() and status = 'active';
$$;

-- Unità del Caporale che guida il gruppo del player
create function public.my_group() returns uuid
language sql stable security definer set search_path = '' as $$
    select case p.rank when 'Caporale' then u.id when 'Soldato' then u.squad_of end
    from public.game_units u
    join public.players p on p.id = u.owner_id
    where u.owner_id = auth.uid() and u.status = 'active';
$$;

-- Unità del Capitano che guida lo squadrone del player
create function public.my_squadron() returns uuid
language sql stable security definer set search_path = '' as $$
    select case p.rank
            when 'Capitano' then u.id
            when 'Caporale' then u.squad_of
            when 'Soldato' then l.squad_of
        end
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.game_units l on l.id = u.squad_of
    where u.owner_id = auth.uid() and u.status = 'active';
$$;

drop function public.my_squad();

-- Caporali dello squadrone e i loro Soldati, Capitano escluso
create function public.squadron_size(p_captain uuid) returns integer
language sql stable security definer set search_path = '' as $$
    select count(*)::integer from public.game_units u
    where u.status = 'active' and (
        u.squad_of = p_captain
        or u.squad_of in (
            select c.id from public.game_units c where c.squad_of = p_captain and c.status = 'active'
        )
    );
$$;

-- Vero se altre p_new unità non entrano più sotto p_leader
create function public.formation_is_full(p_leader uuid, p_new integer) returns boolean
language plpgsql stable security definer set search_path = '' as $$
declare
    v_captain_limit integer;
    v_corporal_limit integer;
    v_rank text;
    v_captain uuid;
begin
    select value into v_captain_limit from public.game_settings where key = 'squad_limit_captain';
    select value into v_corporal_limit from public.game_settings where key = 'squad_limit_corporal';
    select p.rank, u.squad_of into v_rank, v_captain
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.id = p_leader;

    if v_rank = 'Capitano' then
        return public.squadron_size(p_leader) + p_new > v_captain_limit;
    end if;
    return (select count(*) from public.game_units where squad_of = p_leader and status = 'active') + p_new > v_corporal_limit
        or (v_captain is not null and public.squadron_size(v_captain) + p_new > v_captain_limit);
end;
$$;

-- Ogni cambio di grado fa uscire dal proprio gruppo, libera chi stava sotto e chiude le richieste
create or replace function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    new.rank_since := now();

    update public.game_units set squad_of = null where owner_id = new.id;
    update public.game_units set squad_of = null
        where squad_of in (select id from public.game_units where owner_id = new.id);
    update public.game_units set squad_name = null where owner_id = new.id;

    delete from public.squad_requests
        where unit_id in (select id from public.game_units where owner_id = new.id)
        or leader_id in (select id from public.game_units where owner_id = new.id);

    update public.rank_proposals set status = 'closed', closed_at = now()
        where status = 'open'
        and unit_id in (select id from public.game_units where owner_id = new.id);
    return new;
end;
$$;

-- Il Caporale sceglie i Soldati, il Capitano sceglie i Caporali con tutto il loro gruppo
create or replace function public.set_squad_member(p_matricola text, p_member boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_leader uuid := public.my_unit();
    v_member_rank text := case v_rank when 'Capitano' then 'Caporale' else 'Soldato' end;
    v_target public.game_units;
    v_incoming integer := 1;
begin
    if v_rank is null or v_rank not in ('Caporale', 'Capitano') then
        raise exception 'Solo Caporali e Capitani';
    end if;
    perform 1 from public.game_units where id = v_leader for update;

    select * into v_target from public.game_units
        where matricola = p_matricola and status = 'active' for update;
    if v_target.id is null then raise exception 'Unita non attiva'; end if;

    if not p_member then
        update public.game_units set squad_of = null where id = v_target.id and squad_of = v_leader;
        return;
    end if;

    if (select rank from public.players where id = v_target.owner_id) <> v_member_rank then
        raise exception 'Grado non adatto';
    end if;
    if v_target.squad_of is not null then raise exception 'Unita gia in un gruppo'; end if;

    if v_rank = 'Capitano' then
        v_incoming := 1 + (
            select count(*) from public.game_units where squad_of = v_target.id and status = 'active'
        );
    end if;
    if public.formation_is_full(v_leader, v_incoming) then raise exception 'Gruppo al completo'; end if;

    update public.game_units set squad_of = v_leader where id = v_target.id;
end;
$$;

create or replace function public.set_squad_name(p_name text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_name text := trim(p_name);
begin
    if v_rank is null or v_rank not in ('Caporale', 'Capitano') then
        raise exception 'Solo Caporali e Capitani';
    end if;
    if v_name !~ '^[^0-9]{1,8}$' then raise exception 'Nome squadrone non valido'; end if;
    update public.game_units set squad_name = v_name where id = public.my_unit();
end;
$$;

-- I Soldati chiedono a un Caporale, i Caporali a un Capitano
create or replace function public.request_squad(p_leader text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_unit public.game_units;
    v_leader uuid;
begin
    select * into v_unit from public.game_units where owner_id = auth.uid() and status = 'active';
    if v_unit.id is null then raise exception 'Unita non attiva'; end if;

    if p_leader is null then
        delete from public.squad_requests where unit_id = v_unit.id;
        return;
    end if;

    if v_rank is null or v_rank not in ('Soldato', 'Caporale') then raise exception 'Grado non adatto'; end if;
    if v_unit.squad_of is not null then raise exception 'Unita gia in un gruppo'; end if;

    select u.id into v_leader
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.matricola = p_leader and u.status = 'active'
        and p.rank = case v_rank when 'Caporale' then 'Capitano' else 'Caporale' end;
    if v_leader is null then raise exception 'Non guida un gruppo'; end if;
    if public.formation_is_full(v_leader, 1) then raise exception 'Gruppo al completo'; end if;

    insert into public.squad_requests (unit_id, leader_id) values (v_unit.id, v_leader)
        on conflict (unit_id) do update set leader_id = excluded.leader_id, created_at = now();
end;
$$;

create or replace function public.answer_squad_request(p_matricola text, p_accept boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_unit uuid;
begin
    if v_rank is null or v_rank not in ('Caporale', 'Capitano') then
        raise exception 'Solo Caporali e Capitani';
    end if;

    select r.unit_id into v_unit
        from public.squad_requests r
        join public.game_units u on u.id = r.unit_id
        where u.matricola = p_matricola and r.leader_id = public.my_unit();
    if v_unit is null then raise exception 'Richiesta non trovata'; end if;

    if p_accept then perform public.set_squad_member(p_matricola, true); end if;
    delete from public.squad_requests where unit_id = v_unit;
end;
$$;

create or replace function public.get_chat() returns table (
    id bigint, channel text, author text, body text, sent_at timestamptz
)
language plpgsql stable security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_rank text := public.my_rank();
    v_group uuid := public.my_group();
    v_squadron uuid := public.my_squadron();
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
                or (c.channel = 'gruppo' and c.squad_id = v_group)
                or (c.channel = 'squadrone' and c.squad_id = v_squadron)
        ) m
        where m.newest <= 100
        order by m.id;
end;
$$;

create or replace function public.send_chat(p_channel text, p_body text) returns void
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

    select u.id, v_rank || '(' || u.matricola || ')-' || u.type into v_unit, v_author
        from public.game_units u
        where u.owner_id = auth.uid() and u.status in ('pending', 'active');
    if v_gm then v_author := 'Comando Supremo'; end if;
    if v_author is null then raise exception 'Serve una unita per scrivere'; end if;

    if p_channel = 'eventi' and not v_gm then raise exception 'Canale non disponibile'; end if;
    if p_channel = 'comando' and not v_gm and v_rank not in ('Capitano', 'Caporale') then
        raise exception 'Canale non disponibile';
    end if;
    if p_channel in ('gruppo', 'squadrone') then
        v_squad := case p_channel when 'gruppo' then public.my_group() else public.my_squadron() end;
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

-- Squadra con il gruppo e lo squadrone di ogni unità
drop function public.get_team();
drop function public.gm_units();

create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, group_leader text, group_name text, squadron_leader text, squadron_name text
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
           case p.rank when 'Caporale' then u.matricola when 'Soldato' then l.matricola end,
           case p.rank when 'Caporale' then u.squad_name when 'Soldato' then l.squad_name end,
           case p.rank when 'Capitano' then u.matricola when 'Caporale' then l.matricola when 'Soldato' then c.matricola end,
           case p.rank when 'Capitano' then u.squad_name when 'Caporale' then l.squad_name when 'Soldato' then c.squad_name end
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.rank_salaries r on r.rank = p.rank
    left join public.game_units l on l.id = u.squad_of
    left join public.game_units c on c.id = l.squad_of
    where u.status in ('pending', 'active')
    order by r.position desc nulls last, u.matricola;
$$;

create function public.gm_units() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, cost integer, group_leader text, group_name text, squadron_leader text, squadron_name text,
    muted boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, u.cost,
               case p.rank when 'Caporale' then u.matricola when 'Soldato' then l.matricola end,
               case p.rank when 'Caporale' then u.squad_name when 'Soldato' then l.squad_name end,
               case p.rank when 'Capitano' then u.matricola when 'Caporale' then l.matricola when 'Soldato' then c.matricola end,
               case p.rank when 'Capitano' then u.squad_name when 'Caporale' then l.squad_name when 'Soldato' then c.squad_name end,
               p.muted
        from public.game_units u
        join public.players p on p.id = u.owner_id
        left join public.game_units l on l.id = u.squad_of
        left join public.game_units c on c.id = l.squad_of
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

revoke all on function public.my_unit() from public, anon, authenticated;
revoke all on function public.my_group() from public, anon, authenticated;
revoke all on function public.my_squadron() from public, anon, authenticated;
revoke all on function public.squadron_size(uuid) from public, anon, authenticated;
revoke all on function public.formation_is_full(uuid, integer) from public, anon, authenticated;
revoke all on function public.get_team() from public;
revoke all on function public.gm_units() from public, anon;

grant execute on function public.get_team() to anon, authenticated;
grant execute on function public.gm_units() to authenticated;

commit;
