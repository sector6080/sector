-- Nome dello squadrone, scelto dal Caporale: testo senza numeri, massimo 8 caratteri.
-- Da eseguire una volta sola e per intero, dopo chat-gerarchia-gm.sql.
-- Dopo lo script le versioni precedenti dell'app non funzionano più.

begin;

-- Il nome sta sull'unità del Caporale
alter table public.game_units
    add column squad_name text check (squad_name ~ '^[^0-9]{1,8}$');

-- Chi smette di essere Caporale perde anche il nome dello squadrone
create or replace function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    new.rank_since := now();

    if old.rank = 'Caporale' then
        update public.game_units set squad_of = null
            where squad_of in (select id from public.game_units where owner_id = new.id);
        update public.game_units set squad_name = null where owner_id = new.id;
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

create function public.set_squad_name(p_name text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_name text := trim(p_name);
begin
    if public.my_rank() is distinct from 'Caporale' then raise exception 'Solo i Caporali'; end if;
    if v_name !~ '^[^0-9]{1,8}$' then raise exception 'Nome squadrone non valido'; end if;
    update public.game_units set squad_name = v_name where id = public.my_squad();
end;
$$;

drop function public.get_team();
drop function public.gm_units();

create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, squad text, squad_name text
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
           l.matricola, coalesce(l.squad_name, u.squad_name)
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
    status text, cost integer, squad text, squad_name text, muted boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, u.cost,
               l.matricola, coalesce(l.squad_name, u.squad_name), p.muted
        from public.game_units u
        join public.players p on p.id = u.owner_id
        left join public.game_units l on l.id = u.squad_of
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

revoke all on function public.set_squad_name(text) from public, anon;
revoke all on function public.get_team() from public;
revoke all on function public.gm_units() from public, anon;

grant execute on function public.set_squad_name(text) to authenticated;
grant execute on function public.get_team() to anon, authenticated;
grant execute on function public.gm_units() to authenticated;

commit;
