-- Scheda Squadra: quanti membri del Comando Supremo e Reclute non hanno un'unità.
-- Da solo il numero: chi non ha un'unità non ha nemmeno una matricola con cui comparire.
-- Senza unità si è Reclute, anche se il grado salvato è rimasto quello di prima.
-- Da eseguire dopo marcature-e-ponti.sql.

begin;

create function public.get_unitless() returns table (rank text, players integer)
language sql stable security definer set search_path = '' as $$
    select case when p.rank = 'Comando Supremo' then p.rank else 'Recluta' end, count(*)::integer
    from public.players p
    where not exists (
        select 1 from public.game_units u
        where u.owner_id = p.id and u.status in ('pending', 'active')
    )
    group by 1;
$$;

revoke all on function public.get_unitless() from public;
grant execute on function public.get_unitless() to anon, authenticated;

commit;
