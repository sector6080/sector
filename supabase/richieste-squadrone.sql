-- Richieste per entrare negli squadroni di Caporali e Capitani.
-- Un Soldato libero chiede di entrare, chi guida lo squadrone accetta o rifiuta.
-- Da eseguire una volta sola e per intero, dopo gruppi-capitani.sql.

begin;

-- Una sola richiesta alla volta per Soldato: una nuova sostituisce la precedente
create table public.squad_requests (
    unit_id uuid primary key references public.game_units (id) on delete cascade,
    leader_id uuid not null references public.game_units (id) on delete cascade,
    created_at timestamptz not null default now()
);

create index squad_requests_leader on public.squad_requests (leader_id);

alter table public.squad_requests enable row level security;
revoke all on public.squad_requests from anon, authenticated;

-- Il cambio di grado chiude anche le richieste che non hanno più senso
create or replace function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    new.rank_since := now();

    if old.rank in ('Caporale', 'Capitano') and new.rank <> 'Capitano' then
        update public.game_units set squad_of = null
            where squad_of in (select id from public.game_units where owner_id = new.id);
        update public.game_units set squad_name = null where owner_id = new.id;
        delete from public.squad_requests
            where leader_id in (select id from public.game_units where owner_id = new.id);
    end if;
    if new.rank <> 'Soldato' then
        update public.game_units set squad_of = null where owner_id = new.id;
        delete from public.squad_requests
            where unit_id in (select id from public.game_units where owner_id = new.id);
    end if;

    update public.rank_proposals set status = 'closed', closed_at = now()
        where status = 'open'
        and unit_id in (select id from public.game_units where owner_id = new.id);
    return new;
end;
$$;

-- Chi entra in uno squadrone non ha più richieste aperte
create function public.clear_squad_request() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    delete from public.squad_requests where unit_id = new.id;
    return null;
end;
$$;

create trigger on_squad_joined after update of squad_of on public.game_units
    for each row when (new.squad_of is not null)
    execute function public.clear_squad_request();

-- Senza p_leader ritira la richiesta
create function public.request_squad(p_leader text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_leader uuid;
    v_rank text;
    v_limit integer;
begin
    select * into v_unit from public.game_units where owner_id = auth.uid() and status = 'active';
    if v_unit.id is null then raise exception 'Unita non attiva'; end if;

    if p_leader is null then
        delete from public.squad_requests where unit_id = v_unit.id;
        return;
    end if;

    if public.my_rank() is distinct from 'Soldato' then raise exception 'Solo i Soldati'; end if;
    if v_unit.squad_of is not null then raise exception 'Soldato gia in uno squadrone'; end if;

    select u.id, p.rank into v_leader, v_rank
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.matricola = p_leader and u.status = 'active' and p.rank in ('Caporale', 'Capitano');
    if v_leader is null then raise exception 'Non guida uno squadrone'; end if;

    select value into v_limit from public.game_settings
        where key = case when v_rank = 'Capitano' then 'squad_limit_captain' else 'squad_limit_corporal' end;
    if (select count(*) from public.game_units where squad_of = v_leader and status = 'active') >= v_limit then
        raise exception 'Squadrone al completo';
    end if;

    insert into public.squad_requests (unit_id, leader_id) values (v_unit.id, v_leader)
        on conflict (unit_id) do update set leader_id = excluded.leader_id, created_at = now();
end;
$$;

-- Accettando valgono gli stessi controlli di quando il Soldato viene scelto direttamente
create function public.answer_squad_request(p_matricola text, p_accept boolean) returns void
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
        where u.matricola = p_matricola and r.leader_id = public.my_squad();
    if v_unit is null then raise exception 'Richiesta non trovata'; end if;

    if p_accept then perform public.set_squad_member(p_matricola, true); end if;
    delete from public.squad_requests where unit_id = v_unit;
end;
$$;

-- Ognuno vede solo le richieste che ha fatto o che ha ricevuto
create function public.get_squad_requests() returns table (matricola text, type text, leader text)
language sql stable security definer set search_path = '' as $$
    select u.matricola, u.type, l.matricola
    from public.squad_requests r
    join public.game_units u on u.id = r.unit_id
    join public.game_units l on l.id = r.leader_id
    where u.owner_id = auth.uid() or l.owner_id = auth.uid()
    order by r.created_at;
$$;

revoke all on function public.clear_squad_request() from public, anon, authenticated;
revoke all on function public.request_squad(text) from public, anon;
revoke all on function public.answer_squad_request(text, boolean) from public, anon;
revoke all on function public.get_squad_requests() from public, anon;

grant execute on function public.request_squad(text) to authenticated;
grant execute on function public.answer_squad_request(text, boolean) to authenticated;
grant execute on function public.get_squad_requests() to authenticated;

commit;
