-- Scheda GM: sezione Player, con tutti i player anche senza unità, PU, silenzio e cancellazione.
-- Il grado Comando Supremo resta impostabile solo dalla console.
-- Da eseguire dopo rifornimenti-in-diagonale.sql.

begin;

create function public.gm_players() returns table (
    id uuid, matricola text, rank text, pu integer, muted boolean
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select p.id, u.matricola, p.rank, p.pu, p.muted
        from public.players p
        left join public.game_units u on u.owner_id = p.id and u.status in ('pending', 'active')
        left join public.rank_salaries r on r.rank = p.rank
        order by r.position desc nulls last, u.matricola nulls last, p.created_at;
end;
$$;

create function public.gm_change_pu(p_player uuid, p_amount integer) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    update public.players set pu = greatest(pu + p_amount, 0) where id = p_player;
end;
$$;

create function public.gm_mute_player(p_player uuid, p_muted boolean) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    update public.players set muted = p_muted where id = p_player;
end;
$$;

-- Cancellando l'account spariscono a cascata player, unità, ordini, voti e notifiche
create function public.gm_delete_player(p_player uuid) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    if exists (select 1 from public.players where id = p_player and rank = 'Comando Supremo') then
        raise exception 'Comando Supremo non cancellabile';
    end if;

    -- Chi viaggiava sui suoi mezzi torna a terra
    update public.game_units set transported = false, carried_by = null
        where carried_by in (select id from public.game_units where owner_id = p_player);
    delete from auth.users where id = p_player;
end;
$$;

revoke all on function public.gm_players() from public, anon;
revoke all on function public.gm_change_pu(uuid, integer) from public, anon;
revoke all on function public.gm_mute_player(uuid, boolean) from public, anon;
revoke all on function public.gm_delete_player(uuid) from public, anon;

grant execute on function public.gm_players() to authenticated;
grant execute on function public.gm_change_pu(uuid, integer) to authenticated;
grant execute on function public.gm_mute_player(uuid, boolean) to authenticated;
grant execute on function public.gm_delete_player(uuid) to authenticated;

commit;
