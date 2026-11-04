-- Tutti si chiamano con la matricola, il grado si perde con l'unità,
-- la chat firma "grado - matricola - tipo" e la squadra mostra anche le unità in attesa.
-- Da eseguire una volta sola e per intero, dopo gm-turni-catalogo.sql.
-- Dopo lo script le versioni precedenti dell'app non funzionano più.

begin;

-- Niente più nome del player: resta solo la matricola
drop function public.get_team();
drop function public.gm_units();
drop function public.create_unit(text, text, text, text[], text);

alter table public.game_units drop column player_name;

create function public.create_unit(
    p_matricola text, p_type text, p_components text[], p_lore text
) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_type public.unit_types;
    v_found integer;
    v_cost integer;
    v_hp integer;
    v_supplies integer;
    v_uses jsonb;
    v_x integer;
    v_y integer;
begin
    if auth.uid() is null then raise exception 'Accesso richiesto'; end if;
    if public.is_gm() then raise exception 'Il Comando Supremo non puo avere unita'; end if;

    select * into v_type from public.unit_types where name = p_type;
    if not found then raise exception 'Tipo non valido'; end if;

    if cardinality(p_components) <> (select count(distinct c) from unnest(p_components) as c) then
        raise exception 'Componente duplicato';
    end if;

    select count(*), coalesce(sum(cost), 0), coalesce(sum(hp), 0), coalesce(sum(supplies), 0),
           coalesce(jsonb_object_agg(name, max_uses) filter (where max_uses is not null), '{}'::jsonb)
        into v_found, v_cost, v_hp, v_supplies, v_uses
        from public.game_components where type = p_type and name = any(p_components);
    if v_found <> cardinality(p_components) then raise exception 'Componente non disponibile'; end if;

    if exists (
        select 1 from public.game_components c
        where c.type = p_type and c.name = any(p_components) and c.requires is not null
        and not exists (select 1 from unnest(p_components) as o where lower(o) = lower(c.requires))
    ) then
        raise exception 'Prerequisito mancante';
    end if;

    v_cost := v_cost + v_type.cost;
    update public.players set pu = pu - v_cost where id = auth.uid() and pu >= v_cost;
    if not found then raise exception 'PU insufficienti'; end if;

    select m.x, m.y into v_x, v_y from public.game_map m
        where m.site like 'Porto%'
        and not exists (
            select 1 from public.game_units u
            where u.pos_x = m.x and u.pos_y = m.y and u.status in ('pending', 'active')
        )
        order by random() limit 1;
    if v_x is null then raise exception 'Porti occupati'; end if;

    insert into public.game_units (
        owner_id, matricola, type, components, lore,
        hp, supplies, uses, pos_x, pos_y, cost
    ) values (
        auth.uid(), p_matricola, p_type, p_components, p_lore,
        greatest(v_type.hp + v_hp, 1), greatest(v_type.supplies + v_supplies, 0), v_uses, v_x, v_y, v_cost
    );
end;
$$;

-- Squadra divisa per grado, dal più alto: comprende le unità in attesa.
-- L'app ne mostra sulla mappa solo le approvate. Il proprietario resta privato.
create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.rank_salaries r on r.rank = p.rank
    where u.status in ('pending', 'active')
    order by r.position desc nulls last, u.matricola;
$$;

create function public.gm_units() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, cost integer
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, u.cost
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

-- Chi perde l'unità torna Recluta e tiene i PU. Il Comando Supremo non cambia.
create function public.reset_rank_on_lost_unit() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    update public.players set rank = 'Recluta'
        where id = new.owner_id and rank <> 'Comando Supremo';
    return null;
end;
$$;

create trigger on_unit_lost after update of status on public.game_units
    for each row when (new.status = 'lost' and old.status <> 'lost')
    execute function public.reset_rank_on_lost_unit();

-- Chi oggi è senza unità attiva torna Recluta
update public.players p set rank = 'Recluta'
    where p.rank not in ('Recluta', 'Comando Supremo')
    and not exists (
        select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active'
    );

-- Firma dei messaggi: resta quella del momento dell'invio
create or replace function public.send_chat(p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
    v_author text;
begin
    select u.id, p.rank || ' - ' || u.matricola || ' - ' || u.type into v_unit, v_author
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.owner_id = auth.uid() and u.status in ('pending', 'active');
    if v_unit is null and public.is_gm() then v_author := 'Comando Supremo'; end if;
    if v_author is null then raise exception 'Serve una unita per scrivere'; end if;

    if exists (
        select 1 from public.chat_messages
        where author = v_author and sent_at > now() - interval '2 seconds'
    ) then
        raise exception 'Messaggi troppo ravvicinati';
    end if;

    insert into public.chat_messages (unit_id, author, body) values (v_unit, v_author, trim(p_body));
end;
$$;

-- Messaggi già inviati: stessa firma, tranne per chi oggi è Comando Supremo
update public.chat_messages c
    set author = p.rank || ' - ' || u.matricola || ' - ' || u.type
    from public.game_units u
    join public.players p on p.id = u.owner_id
    where c.unit_id = u.id and p.rank <> 'Comando Supremo';

revoke all on function public.create_unit(text, text, text[], text) from public, anon;
revoke all on function public.get_team() from public;
revoke all on function public.gm_units() from public, anon;
revoke all on function public.reset_rank_on_lost_unit() from public, anon, authenticated;

grant execute on function public.create_unit(text, text, text[], text) to authenticated;
grant execute on function public.get_team() to anon, authenticated;
grant execute on function public.gm_units() to authenticated;

commit;
