-- Mappa di gioco e squadra visibile a tutti.
-- La mappa ha una riga per casella: un GM può cambiare terreno o struttura dal Table Editor.

begin;

create table public.game_map (
    x integer not null check (x between 1 and 80),
    y integer not null check (y between 1 and 59),
    terrain text not null check (terrain in ('mare', 'fiume', 'ponte', 'porto', 'sabbia', 'prato', 'edificio')),
    site text,
    primary key (x, y)
);

alter table public.game_map enable row level security;

create policy "Tutti vedono la mappa" on public.game_map
    for select to anon, authenticated using (true);

grant select on public.game_map to anon, authenticated;

-- Copia della scheda "mappa" del foglio Sector 6080 (master), con il bordo nero K.
-- B mare, L fiume, O ponte, A porto, S sabbia, G prato, W edificio.
do $$
declare
    v_rows text[] := string_to_array('82K/K80BK/K80BK/K80BK/K49B2A4S16B9SK/K49B2A12S9B8SK/K49B2A12S9B8SK/K48B16S5B2AB8SK/K17B2A27B18S2B3S2A9SK/K10B7S2A16B7S4B23S2A9SK/K10B7S2A4S9B4S2W4S4B34SK/K25S7B4S2W39S2WSK/K25SB51S2WSK/K77S3GK/K2S2W57S5G11S3GK/K2S2W47S3G6SW5G11S3GK/K24SW26S18G6S5GK/K12SW19S3G12S22G6S5GK/K30S5G7S38GK/K10S26G6S38GK/K9S71GK/K8S72GK/K8S58G14LK/K6S59G6L9GK/K5S56G2LO5L11GK/K5S50G6L19GK/K2L3S44G5LOL24GK/K3L2S26G9LO8L6G3L22GK/K2S2LS20G3LO2L25G4L20GK/K5L14G6L32G5L18GK/K5LO11LO2L41G2LO3L13GK/K2S3LO4L54G6L10GK/K3S66G11LK/K3S77GK/KG2S77GK/K2GS77GK/K2GS77GK/K2GS77GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/K80GK/82K', '/');
    v_match text[];
    v_terrain text;
    v_x integer;
begin
    for v_y in 2 .. array_length(v_rows, 1) - 1 loop
        v_x := 0;
        for v_match in select regexp_matches(v_rows[v_y], '(\d*)([A-Z])', 'g') loop
            for i in 1 .. coalesce(nullif(v_match[1], '')::integer, 1) loop
                v_terrain := case v_match[2]
                    when 'B' then 'mare' when 'L' then 'fiume' when 'O' then 'ponte'
                    when 'A' then 'porto' when 'S' then 'sabbia' when 'G' then 'prato'
                    when 'W' then 'edificio'
                end;
                if v_terrain is not null then
                    insert into public.game_map (x, y, terrain) values (v_x, v_y - 1, v_terrain);
                end if;
                v_x := v_x + 1;
            end loop;
        end loop;
    end loop;
end;
$$;

update public.game_map set site = 'Porto 1' where x between 18 and 19 and y between 8 and 10;
update public.game_map set site = 'Porto 2' where x between 50 and 51 and y between 4 and 6;
update public.game_map set site = 'Porto 3' where x between 70 and 71 and y between 7 and 9;
update public.game_map set site = 'Raffineria 1' where x between 37 and 38 and y between 10 and 11;
update public.game_map set site = 'Raffineria 2' where x between 78 and 79 and y between 11 and 12;
update public.game_map set site = 'Raffineria 3' where x between 3 and 4 and y between 14 and 15;
update public.game_map set site = 'Magazzino 1' where x = 61 and y = 15;
update public.game_map set site = 'Magazzino 2' where x = 25 and y = 16;
update public.game_map set site = 'Magazzino 3' where x = 13 and y = 17;

-- Mappa compatta per l'app: una stringa per riga, più le strutture
create function public.get_map() returns jsonb
language sql stable security invoker set search_path = '' as $$
    select jsonb_build_object(
        'rows', (
            select jsonb_agg(r.cells order by r.y) from (
                select y, string_agg(case terrain
                    when 'mare' then 'B' when 'fiume' then 'L' when 'ponte' then 'O'
                    when 'porto' then 'A' when 'sabbia' then 'S' when 'prato' then 'G'
                    when 'edificio' then 'W'
                end, '' order by x) as cells
                from public.game_map group by y
            ) r
        ),
        'sites', coalesce((
            select jsonb_agg(jsonb_build_object('name', s.site, 'x', s.x, 'y', s.y, 'w', s.w, 'h', s.h) order by s.site)
            from (
                select site, min(x) as x, min(y) as y, max(x) - min(x) + 1 as w, max(y) - min(y) + 1 as h
                from public.game_map where site is not null group by site
            ) s
        ), '[]'::jsonb)
    );
$$;

-- Le unità approvate sono visibili a tutti: PU, statistiche, componenti, lore e posizione.
-- Il proprietario resta privato.
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

revoke all on function public.get_map() from public;
revoke all on function public.get_team() from public;
grant execute on function public.get_map() to anon, authenticated;
grant execute on function public.get_team() to anon, authenticated;

commit;