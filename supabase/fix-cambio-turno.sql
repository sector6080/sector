-- Supabase rifiuta gli UPDATE senza WHERE quando arrivano dall'app: il cambio turno automatico
-- funzionava, quello manuale del GM no. game_turn ha una sola riga, con id sempre vero.
-- Da eseguire dopo gerarchia-squadroni.sql. Sul database attuale è già applicato.

create or replace function public.pay_and_advance(p_started_at timestamptz) returns void
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_turn set turn = turn + 1, started_at = p_started_at where id;
    update public.players p set pu = p.pu + s.pu
        from public.rank_salaries s
        where s.rank = p.rank
        and exists (select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active');
end;
$$;
