-- Elimina le tabelle del vecchio sito web. L'operazione cancella i dati e non si può annullare.
-- NON eseguire prima di aver salvato una copia anche di map e general, se contengono dati.
--
-- Attenzione ai nomi: public.player è la vecchia tabella del sito,
-- public.players è quella dell'app e non va toccata.
-- Senza CASCADE, lo script si ferma se qualcosa dipende ancora da queste tabelle.

begin;

drop table if exists public.units;
drop table if exists public.new_units;
drop table if exists public.player;
drop table if exists public.map;
drop table if exists public.general;

commit;
