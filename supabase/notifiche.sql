-- Notifiche: le crea il server, così nessuno può inventarle o mandarle al posto di un altro.
-- Eventi: nuovo turno, gradi, unità, gruppi, richieste, votazioni, ordini applicati e menzioni in chat.
-- Il Comando Supremo può scrivere @tutti, i player possono menzionare solo @MATRICOLA.
-- Da eseguire una volta sola e per intero, dopo template-nemici.sql.

begin;

create table public.notifications (
    id bigint generated always as identity primary key,
    player_id uuid not null references public.players (id) on delete cascade,
    kind text not null check (kind in ('ping', 'turno', 'grado', 'unita', 'gruppo', 'richiesta', 'votazione', 'ordini')),
    title text not null,
    body text not null default '',
    created_at timestamptz not null default now(),
    read_at timestamptz
);

create index notifications_player on public.notifications (player_id, id desc);

alter table public.notifications enable row level security;
revoke all on public.notifications from anon, authenticated;

create function public.notify(p_player uuid, p_kind text, p_title text, p_body text default '') returns void
language sql security definer set search_path = '' as $$
    insert into public.notifications (player_id, kind, title, body)
        values (p_player, p_kind, left(p_title, 120), left(p_body, 200));
$$;

-- Chi legge il canale: una menzione non deve mostrare un canale privato a chi ne è fuori
create function public.channel_members(p_channel text, p_squad uuid) returns setof uuid
language sql stable security definer set search_path = '' as $$
    select p.id
    from public.players p
    left join public.game_units u on u.owner_id = p.id and u.status = 'active'
    left join public.game_units l on l.id = u.squad_of
    where p_channel in ('eventi', 'comune')
        or p.rank = 'Comando Supremo'
        or (p_channel = 'comando' and p.rank in ('Capitano', 'Caporale'))
        or (p_channel = 'gruppo' and p_squad = case p.rank when 'Caporale' then u.id when 'Soldato' then u.squad_of end)
        or (p_channel = 'squadrone' and p_squad = case p.rank
                when 'Capitano' then u.id
                when 'Caporale' then u.squad_of
                when 'Soldato' then l.squad_of
            end);
$$;

-- Nuovo turno, con lo stipendio. Le notifiche più vecchie di 30 giorni si cancellano qui.
create or replace function public.pay_and_advance(p_started_at timestamptz) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_turn integer;
begin
    update public.game_turn set turn = turn + 1, started_at = p_started_at where id returning turn into v_turn;
    update public.players p set pu = p.pu + s.pu
        from public.rank_salaries s
        where s.rank = p.rank
        and exists (select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active');

    insert into public.notifications (player_id, kind, title, body)
        select p.id, 'turno', 'Turno ' || v_turn,
            case when u.id is null then 'È iniziato un nuovo turno.'
                else 'È iniziato un nuovo turno. Stipendio: ' || coalesce(s.pu, 0) || ' PU.' end
        from public.players p
        left join public.game_units u on u.owner_id = p.id and u.status = 'active'
        left join public.rank_salaries s on s.rank = p.rank
        where u.id is not null or p.rank = 'Comando Supremo';

    delete from public.notifications where created_at < now() - interval '30 days';
end;
$$;

-- Promozioni e declassamenti. L'approvazione e la perdita dell'unità hanno la loro notifica.
create or replace function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
    v_old integer := (select position from public.rank_salaries where rank = old.rank);
    v_new integer := (select position from public.rank_salaries where rank = new.rank);
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

    if old.rank <> 'Recluta' and new.rank <> 'Recluta' then
        perform public.notify(
            new.id,
            'grado',
            case when v_new > v_old then 'Promozione' else 'Declassamento' end,
            'Il tuo grado ora è ' || trim(new.rank) || '.'
        );
    end if;
    return new;
end;
$$;

create function public.notify_unit_status() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if old.status = 'pending' and new.status = 'active' then
        perform public.notify(new.owner_id, 'unita', 'Unità approvata', new.matricola || ' è entrata in gioco.');
    elsif old.status = 'pending' and new.status = 'rejected' then
        perform public.notify(new.owner_id, 'unita', 'Unità rifiutata', new.matricola || ' non è stata approvata.');
    elsif old.status = 'active' and new.status = 'lost'
        and (select rank from public.players where id = new.owner_id) <> 'Comando Supremo' then
        perform public.notify(new.owner_id, 'unita', 'Unità persa', new.matricola || ' è stata persa. I PU restano, il grado torna Recluta.');
    end if;
    return null;
end;
$$;

create trigger on_unit_status_notify after update of status on public.game_units
    for each row when (old.status is distinct from new.status)
    execute function public.notify_unit_status();

-- Ingressi e uscite. Se un gruppo entra in uno squadrone lo sanno anche i suoi Soldati.
create function public.notify_squad_change() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
    v_leader public.game_units;
    v_label text;
begin
    select * into v_leader from public.game_units where id = coalesce(new.squad_of, old.squad_of);
    v_label := case when (select rank from public.players where id = v_leader.owner_id) = 'Capitano'
        then 'dello squadrone' else 'del gruppo' end;

    if new.squad_of is not null then
        perform public.notify(new.owner_id, 'gruppo', 'Nuovo gruppo', 'Ora fai parte ' || v_label || ' di ' || v_leader.matricola || '.');
        insert into public.notifications (player_id, kind, title, body)
            select u.owner_id, 'gruppo', 'Nuovo gruppo', 'Il tuo gruppo ora fa parte ' || v_label || ' di ' || v_leader.matricola || '.'
            from public.game_units u
            where u.squad_of = new.id and u.status = 'active';
    else
        perform public.notify(new.owner_id, 'gruppo', 'Uscita dal gruppo', 'Non fai più parte ' || v_label || ' di ' || v_leader.matricola || '.');
        insert into public.notifications (player_id, kind, title, body)
            select u.owner_id, 'gruppo', 'Uscita dal gruppo', 'Il tuo gruppo non fa più parte ' || v_label || ' di ' || v_leader.matricola || '.'
            from public.game_units u
            where u.squad_of = new.id and u.status = 'active';
    end if;
    return null;
end;
$$;

create trigger on_squad_change_notify after update of squad_of on public.game_units
    for each row when (old.squad_of is distinct from new.squad_of)
    execute function public.notify_squad_change();

create function public.notify_squad_request() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    perform public.notify(
        (select owner_id from public.game_units where id = new.leader_id),
        'richiesta',
        'Nuova richiesta',
        (select matricola from public.game_units where id = new.unit_id) || ' chiede di entrare.'
    );
    return null;
end;
$$;

create trigger on_squad_request_notify after insert or update of leader_id on public.squad_requests
    for each row execute function public.notify_squad_request();

create or replace function public.answer_squad_request(p_matricola text, p_accept boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_unit uuid;
    v_owner uuid;
begin
    if v_rank is null or v_rank not in ('Caporale', 'Capitano') then
        raise exception 'Solo Caporali e Capitani';
    end if;

    select r.unit_id, u.owner_id into v_unit, v_owner
        from public.squad_requests r
        join public.game_units u on u.id = r.unit_id
        where u.matricola = p_matricola and r.leader_id = public.my_unit();
    if v_unit is null then raise exception 'Richiesta non trovata'; end if;

    if p_accept then
        perform public.set_squad_member(p_matricola, true);
    else
        perform public.notify(v_owner, 'richiesta', 'Richiesta rifiutata', 'La tua richiesta di entrare non è stata accettata.');
    end if;
    delete from public.squad_requests where unit_id = v_unit;
end;
$$;

-- I Capitani sanno quando c'è da votare
create function public.notify_rank_proposal() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.notifications (player_id, kind, title, body)
        select p.id, 'votazione', 'Nuova votazione',
            (select matricola from public.game_units where id = new.unit_id) || ': da ' || new.from_rank || ' a ' || new.new_rank || '.'
        from public.players p
        where p.rank = 'Capitano'
        and p.id <> (select owner_id from public.game_units where id = new.unit_id)
        and p.id is distinct from (select owner_id from public.game_units where id = new.proposed_by);
    return null;
end;
$$;

create trigger on_rank_proposal_notify after insert on public.rank_proposals
    for each row execute function public.notify_rank_proposal();

create function public.notify_orders_applied() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    perform public.notify(
        (select owner_id from public.game_units where id = new.unit_id),
        'ordini',
        'Ordini applicati',
        'Il Game Master ha applicato i tuoi ordini del turno ' || new.turn || '.'
    );
    return null;
end;
$$;

create trigger on_orders_applied_notify after update of applied_at on public.game_orders
    for each row when (old.applied_at is null and new.applied_at is not null)
    execute function public.notify_orders_applied();

-- Menzioni: @tutti solo per il Comando Supremo, @MATRICOLA per tutti e al massimo 5 per messaggio
create or replace function public.send_chat(p_channel text, p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_rank text;
    v_muted boolean;
    v_unit uuid;
    v_author text;
    v_squad uuid;
    v_body text := trim(p_body);
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
        values (v_unit, p_channel, v_squad, v_author, v_body);

    if v_gm and v_body ~* '(^|\s)@tutti(\s|$)' then
        insert into public.notifications (player_id, kind, title, body)
            select m, 'ping', 'Comando Supremo', left(v_body, 200)
            from public.channel_members(p_channel, v_squad) as m
            where m <> auth.uid();
    else
        insert into public.notifications (player_id, kind, title, body)
            select distinct u.owner_id, 'ping', left(v_author, 120), left(v_body, 200)
            from (
                select distinct upper(hit[1]) as matricola
                from regexp_matches(v_body, '@([A-Za-z]{3}-[0-9]{3})', 'g') as hit
                limit 5
            ) mention
            join public.game_units u on u.matricola = mention.matricola and u.status in ('pending', 'active')
            where u.owner_id <> auth.uid()
            and u.owner_id in (select public.channel_members(p_channel, v_squad));
    end if;
end;
$$;

create function public.get_notifications() returns table (
    id bigint, kind text, title text, body text, created_at timestamptz, read boolean
)
language sql stable security definer set search_path = '' as $$
    select n.id, n.kind, n.title, n.body, n.created_at, n.read_at is not null
    from public.notifications n
    where n.player_id = auth.uid()
    order by n.id desc
    limit 50;
$$;

create function public.mark_notifications_read() returns void
language sql security definer set search_path = '' as $$
    update public.notifications set read_at = now()
        where player_id = auth.uid() and read_at is null;
$$;

-- Creazione senza componenti: il tipo si paga all'approvazione, quindi un rifiuto non costa nulla.
-- Le unità già in attesa avevano pagato alla creazione: ripartono da zero.
update public.players p set pu = p.pu + u.cost
    from public.game_units u
    where u.owner_id = p.id and u.status = 'pending';

update public.game_units u set components = '{}', uses = '{}', hp = t.hp, supplies = t.supplies
    from public.unit_types t
    where t.name = u.type and u.status = 'pending';

drop function public.create_unit(text, text, text[], text);

create function public.create_unit(p_matricola text, p_type text, p_lore text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_type public.unit_types;
    v_x integer;
    v_y integer;
begin
    if auth.uid() is null then raise exception 'Accesso richiesto'; end if;
    if public.is_gm() then raise exception 'Il Comando Supremo non puo avere unita'; end if;

    select * into v_type from public.unit_types where name = p_type;
    if not found then raise exception 'Tipo non valido'; end if;

    if (select pu from public.players where id = auth.uid()) < v_type.cost then
        raise exception 'PU insufficienti';
    end if;

    select m.x, m.y into v_x, v_y from public.game_map m
        where m.site like 'Porto%'
        and not exists (
            select 1 from public.game_units u
            where u.pos_x = m.x and u.pos_y = m.y and u.status in ('pending', 'active')
        )
        order by random() limit 1;
    if v_x is null then raise exception 'Porti occupati'; end if;

    insert into public.game_units (owner_id, matricola, type, lore, hp, supplies, pos_x, pos_y)
        values (auth.uid(), p_matricola, p_type, p_lore, v_type.hp, v_type.supplies, v_x, v_y);
end;
$$;

create or replace function public.gm_review_unit(p_matricola text, p_approve boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_cost integer;
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    select * into v_unit from public.game_units
        where matricola = p_matricola and status = 'pending' for update;
    if v_unit.id is null then raise exception 'Unita non in attesa'; end if;

    if not p_approve then
        update public.game_units set status = 'rejected' where id = v_unit.id;
        return;
    end if;

    select cost into v_cost from public.unit_types where name = v_unit.type;
    update public.players set pu = pu - v_cost where id = v_unit.owner_id and pu >= v_cost;
    if not found then raise exception 'PU insufficienti'; end if;

    update public.game_units set status = 'active' where id = v_unit.id;
    update public.players set rank = 'Soldato' where id = v_unit.owner_id and rank = 'Recluta';
end;
$$;

create or replace function public.buy_component(p_name text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_component public.game_components;
begin
    select * into v_unit from public.game_units
        where owner_id = auth.uid() and status = 'active' for update;
    if v_unit.id is null then raise exception 'Solo dopo approvazione'; end if;

    select * into v_component from public.game_components where type = v_unit.type and name = p_name;
    if v_component.name is null then raise exception 'Componente non disponibile'; end if;
    if p_name = any(v_unit.components) then raise exception 'Componente gia posseduto'; end if;
    if v_component.requires is not null and not exists (
        select 1 from unnest(v_unit.components) as o where lower(o) = lower(v_component.requires)
    ) then
        raise exception 'Prerequisito mancante';
    end if;

    update public.players set pu = pu - v_component.cost where id = auth.uid() and pu >= v_component.cost;
    if not found then raise exception 'PU insufficienti'; end if;

    update public.game_units set
        components = components || p_name,
        hp = greatest(hp + v_component.hp, 1),
        supplies = greatest(supplies + v_component.supplies, 0),
        uses = case when v_component.max_uses is null then uses
            else uses || jsonb_build_object(p_name, v_component.max_uses) end
        where id = v_unit.id;
end;
$$;

drop function public.gm_units();

create function public.gm_units() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, group_leader text, group_name text, squadron_leader text, squadron_name text,
    muted boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
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

alter table public.game_units drop column cost;

revoke all on function public.create_unit(text, text, text) from public, anon;
revoke all on function public.gm_units() from public, anon;
grant execute on function public.create_unit(text, text, text) to authenticated;
grant execute on function public.gm_units() to authenticated;

revoke all on function public.notify(uuid, text, text, text) from public, anon, authenticated;
revoke all on function public.channel_members(text, uuid) from public, anon, authenticated;
revoke all on function public.notify_unit_status() from public, anon, authenticated;
revoke all on function public.notify_squad_change() from public, anon, authenticated;
revoke all on function public.notify_squad_request() from public, anon, authenticated;
revoke all on function public.notify_rank_proposal() from public, anon, authenticated;
revoke all on function public.notify_orders_applied() from public, anon, authenticated;
revoke all on function public.get_notifications() from public, anon;
revoke all on function public.mark_notifications_read() from public, anon;

grant execute on function public.get_notifications() to authenticated;
grant execute on function public.mark_notifications_read() to authenticated;

commit;
