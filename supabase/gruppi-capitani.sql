-- Gruppi dei Capitani: funzionano come gli squadroni dei Caporali, ma fino a 40 Soldati.
-- I due limiti stanno in game_settings e si cambiano dal Table Editor, senza aggiornare l'app.
-- Da eseguire una volta sola e per intero, dopo nome-squadrone.sql.

begin;

insert into public.game_settings (key, value, description) values
    ('squad_limit_corporal', 6, 'Soldati massimi nello squadrone di un Caporale'),
    ('squad_limit_captain', 40, 'Soldati massimi nel gruppo di un Capitano');

-- Caporali e Capitani guidano il proprio gruppo, il Soldato sta in quello di chi lo ha scelto
create or replace function public.my_squad() returns uuid
language sql stable security definer set search_path = '' as $$
    select case when p.rank in ('Caporale', 'Capitano') then u.id else u.squad_of end
    from public.game_units u
    join public.players p on p.id = u.owner_id
    where u.owner_id = auth.uid() and u.status = 'active';
$$;

-- Diventando Capitano il Caporale tiene il suo gruppo, ogni altro cambio di grado lo scioglie
create or replace function public.handle_rank_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    new.rank_since := now();

    if old.rank in ('Caporale', 'Capitano') and new.rank <> 'Capitano' then
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

create or replace function public.set_squad_member(p_matricola text, p_member boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_rank text := public.my_rank();
    v_leader uuid;
    v_limit integer;
    v_target public.game_units;
begin
    if v_rank is null or v_rank not in ('Caporale', 'Capitano') then
        raise exception 'Solo Caporali e Capitani';
    end if;
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

    select value into v_limit from public.game_settings
        where key = case when v_rank = 'Capitano' then 'squad_limit_captain' else 'squad_limit_corporal' end;
    if (select count(*) from public.game_units where squad_of = v_leader and status = 'active') >= v_limit then
        raise exception 'Squadrone al completo';
    end if;
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
    update public.game_units set squad_name = v_name where id = public.my_squad();
end;
$$;

commit;
