-- Rende visibili a tutti i PU dei player con un'unità approvata.
-- Il proprietario dell'unità resta privato. Da eseguire una volta sola,
-- dopo mappa-e-squadra.sql, che per chi parte da zero contiene già questa modifica.

begin;

drop function public.get_team();

create function public.get_team() returns table (
    matricola text, player_name text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, u.player_name, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported
    from public.game_units u
    join public.players p on p.id = u.owner_id
    where u.status = 'active'
    order by u.matricola;
$$;

revoke all on function public.get_team() from public;
grant execute on function public.get_team() to anon, authenticated;

commit;
