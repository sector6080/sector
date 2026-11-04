-- Pulizia del database dopo l'abbandono del sito web.
-- NON eseguire prima di aver salvato una copia di player, new_units e units:
-- il piano gratuito non ha backup automatici e questo script cancella dati.
--
-- Dopo l'esecuzione nessuno può leggere o scrivere dall'esterno con la chiave
-- pubblica, finché l'app non avrà le sue tabelle e regole.

begin;

-- Stop agli inserimenti pubblici
drop policy if exists "Enable write access for all users" on public.new_units;

-- Stop alle letture pubbliche: le usava solo il sito
drop policy if exists "Enable read access for all users" on public.units;
drop policy if exists "Enable read access for all users" on public.map;

-- Via le password salvate
alter table public.player drop column if exists password;
alter table public.new_units drop column if exists password;

-- Tabella usata solo dal sito, con la sua regola di inserimento pubblico
drop table if exists public.verify_unit;

commit;
