-- Sezione Game Master, turni automatici, stipendi, catalogo sul server e mappa 60x80.
-- Regola: si diventa GM solo dalla console, impostando players.rank a 'Comando Supremo'.

begin;

-- Mappa 60x80: aggiunge l'ultima riga
alter table public.game_map drop constraint game_map_y_check;
alter table public.game_map add constraint game_map_y_check check (y between 1 and 60);
alter table public.game_units drop constraint game_units_pos_y_check;
alter table public.game_units add constraint game_units_pos_y_check check (pos_y between 1 and 60);
insert into public.game_map (x, y, terrain)
    select x, 60, 'prato' from generate_series(1, 80) as x;

-- Informazioni di gioco modificabili dalla dashboard
create table public.game_settings (
    key text primary key,
    value integer not null,
    description text not null
);
insert into public.game_settings (key, value, description) values
    ('starting_pu', 4, 'PU di ogni nuovo player'),
    ('google_bonus_pu', 1, 'PU extra per chi collega Google');

create table public.rank_salaries (
    rank text primary key,
    pu integer not null check (pu >= 0),
    position integer not null
);
insert into public.rank_salaries (rank, pu, position) values
    ('Recluta', 0, 1),
    ('Soldato', 1, 2),
    ('Caporale', 2, 3),
    ('Capitano', 3, 4),
    ('Comando Supremo', 0, 5);

create table public.unit_types (
    name text primary key,
    cost integer not null check (cost >= 0),
    hp integer not null,
    movement integer not null,
    vision integer not null,
    armor integer not null,
    supplies integer not null
);

create table public.game_components (
    type text not null references public.unit_types (name) on update cascade,
    name text not null,
    cost integer not null check (cost >= 0),
    description text not null,
    hp integer not null default 0,
    movement integer not null default 0,
    vision integer not null default 0,
    armor integer not null default 0,
    supplies integer not null default 0,
    perforation integer,
    damage integer,
    hits integer,
    area_w integer,
    area_h integer,
    range integer,
    max_uses integer,
    reload_cost integer not null default 1,
    stationary boolean not null default false,
    requires text,
    primary key (type, name)
);

create table public.game_sites (
    name text primary key,
    description text not null
);
insert into public.game_sites (name, description) values
    ('Porto 1', 'Punto di arrivo dei rinforzi: qui vengono schierate le nuove unità.'),
    ('Porto 2', 'Punto di arrivo dei rinforzi: qui vengono schierate le nuove unità.'),
    ('Porto 3', 'Punto di arrivo dei rinforzi: qui vengono schierate le nuove unità.'),
    ('Raffineria 1', 'Impianto che produce carburante per la Task Force.'),
    ('Raffineria 2', 'Impianto che produce carburante per la Task Force.'),
    ('Raffineria 3', 'Impianto che produce carburante per la Task Force.'),
    ('Magazzino 1', 'Deposito: le unità adiacenti ricaricano scorte e abilità.'),
    ('Magazzino 2', 'Deposito: le unità adiacenti ricaricano scorte e abilità.'),
    ('Magazzino 3', 'Deposito: le unità adiacenti ricaricano scorte e abilità.');

insert into public.unit_types (name, cost, hp, movement, vision, armor, supplies) values
    ('Fanteria', 0, 9, 2, 3, 1, 4),
    ('Motorizzata', 1, 12, 3, 3, 2, 8),
    ('Corazzata', 4, 14, 3, 2, 3, 4),
    ('Artiglieria', 5, 8, 1, 5, 1, 4);

insert into public.game_components (type, name, cost, description, hp, movement, vision, armor, supplies, perforation, damage, hits, area_w, area_h, range, max_uses, reload_cost, stationary, requires) values
    ('Fanteria', 'Pistola', 1, 'Perf. 1 | Range 2 | 1 danno | (2/2)', 0, 0, 0, 0, 0, 1, 1, 1, null, null, 2, 2, 1, false, null),
    ('Fanteria', 'Binocolo', 1, '+1 visione', 0, 0, 1, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Radio Tattica', 1, '+1 movimento', 0, 1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Mappa Tattica', 1, '+1 movimento', 0, 1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Faro Laser', 1, 'Marca una casella per 2 turni | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Fanteria', 'Siringa Stim', 1, '+1 movimento durante il turno | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 2, 1, false, null),
    ('Fanteria', 'Imbottitura Pesante', 1, '+4 HP, -1 movimento', 4, -1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Fucile d''Assalto', 2, 'Perf. 1 | Range 3 | 2 danni | (3/3)', 0, 0, 0, 0, 0, 1, 2, 1, null, null, 3, 3, 1, false, null),
    ('Fanteria', 'Fucile a Pompa', 2, 'Perf. 2 | Range 1 | 3 danni | (2/2)', 0, 0, 0, 0, 0, 2, 3, 1, null, null, 1, 2, 1, false, null),
    ('Fanteria', 'Pistola Silenziata', 2, 'Perf. 1 | Range 2 | 2 danni | (4/4)', 0, 0, 0, 0, 0, 1, 2, 1, null, null, 2, 4, 1, false, null),
    ('Fanteria', 'Kit Medico', 2, 'Cura 2 HP | (3/3)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 3, 1, false, null),
    ('Fanteria', 'Cassetta Medica', 2, 'Cura 5 HP | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Fanteria', 'Granata a Frammentazione', 2, 'Perf. 2 | Range 2 | 4 danni | (1/1)', 0, 0, 0, 0, 0, 2, 4, 1, null, null, 2, 1, 1, false, null),
    ('Fanteria', 'Granata', 2, 'Perf. 1 | Range 2 | 1 danno (area 3x3) | (1/1)', 0, 0, 0, 0, 0, 1, 1, 1, 3, 3, 2, 1, 1, false, null),
    ('Fanteria', 'Scudo Balistico', 2, '+2 armor, -1 movimento', 0, -1, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Trappola Esplosiva', 2, 'Attiva al passaggio | Perf. 3 | 4 danni | (1/1)', 0, 0, 0, 0, 0, 3, 4, 1, null, null, null, 1, 1, false, null),
    ('Fanteria', 'Kit Riparazione', 2, 'Range 1 | Ripara 8 HP a motorizzata o corazzata | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, 1, 2, 1, false, null),
    ('Fanteria', 'Zaino Tattico', 2, '+2 max rifornimenti', 0, 0, 0, 0, 2, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Proiettili Perforanti', 2, '+1 Perf (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Proiettili Esplosivi', 2, '+1 danni (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Mirino Laser', 2, '+2 Perf a corto raggio (range ≤3) (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Ottica a Doppio Zoom', 2, '+2 Range, -1 Perf (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Mirino Digitale', 2, '+1 Range, +1 Perf (range 4+) (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, 4, null, 1, false, null),
    ('Fanteria', 'Stivali Antimine', 2, 'Immunità a trappole esplosive', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'SMG', 3, 'Perf. 2 | Range 2 | 3 danni | (3/3)', 0, 0, 0, 0, 0, 2, 3, 1, null, null, 2, 3, 1, false, null),
    ('Fanteria', 'Fucile di Precisione', 3, 'Perf. 2 | Range 5 | 4 danni | (1/1) (deve stare fermo)', 0, 0, 0, 0, 0, 2, 4, 1, null, null, 5, 1, 1, true, null),
    ('Fanteria', 'Lanciagranate', 3, 'Perf. 2 | Range 4 | 3 danni (area 2x2) | (1/1)', 0, 0, 0, 0, 0, 2, 3, 1, 2, 2, 4, 1, 1, false, null),
    ('Fanteria', 'Esplosivo C4', 3, 'Perf. 4 | Range 0 | 5 danni (area 3x3) | (1/1) (deve essere piazzato)', 0, 0, 0, 0, 0, 4, 5, 1, 3, 3, 0, 1, 1, false, null),
    ('Fanteria', 'Sistema di Mira Laser', 3, '+1 Perf, +1 Range (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Mirino ACOG', 3, '+2 Perf a lungo raggio (range ≥4) (solo armi da fuoco)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Jammer Portatile', 3, 'Blocca radar in area 2x2 | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Fanteria', 'Mitragliatrice Leggera', 4, 'Perf. 2 | Range 3 | 3x2 danni | (2/2)', 0, 0, 0, 0, 0, 2, 2, 3, null, null, 3, 2, 1, false, null),
    ('Fanteria', 'Fucile da Tiro', 4, 'Perf. 3 | Range 6 | 4 danni | (1/1)', 0, 0, 0, 0, 0, 3, 4, 1, null, null, 6, 1, 1, false, null),
    ('Fanteria', 'Droni da Ricognizione', 4, '+3 visione', 0, 0, 3, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Giubbotto in Lega', 4, '+3 armor, -2 movimento', 0, -2, 0, 3, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Kit da Demolitore', 4, 'Range 1 | 9 danni | (2/2) (solo contro strutture)', 0, 0, 0, 0, 0, 0, 9, 1, null, null, 1, 2, 1, false, null),
    ('Fanteria', 'Drone Spotter', 4, 'Vola su ostacoli, +2 visione | (3 usi)', 0, 0, 2, 0, 0, null, null, null, null, null, null, 3, 1, false, null),
    ('Fanteria', 'Fucile Anticarro', 5, 'Perf. 8 | Range 3 | 6 danni | (1/1)', 0, 0, 0, 0, 0, 8, 6, 1, null, null, 3, 1, 1, false, null),
    ('Fanteria', 'Lanciafiamme', 5, 'Perf. 2 | Range 2 | 8 danni (area 2x1) | (2/2)', 0, 0, 0, 0, 0, 2, 8, 1, 2, 1, 2, 2, 1, false, null),
    ('Fanteria', 'Mortaio Leggero', 6, 'Perf. 3 | Range 6 | 4 danni (area 2x2) | (1/1) (non può muoversi)', 0, 0, 0, 0, 0, 3, 4, 1, 2, 2, 6, 1, 1, true, null),
    ('Fanteria', 'Generatore di Scudi', 6, '+2 armor, immunità a esplosivi', 0, 0, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Kit da Cecchino', 6, 'Perf. 4 | Range 7 | 6 danni | (1/1)', 0, 0, 0, 0, 0, 4, 6, 1, null, null, 7, 1, 1, false, null),
    ('Fanteria', 'Fucile ad Antimateria', 6, 'Perf. 7 | Range 5 | 8 danni | (1/1) (non può muoversi)', 0, 0, 0, 0, 0, 7, 8, 1, null, null, 5, 1, 1, true, null),
    ('Fanteria', 'Missile MANPADS', 8, 'Perf. 3 | Range 5 | 10 danni | (1/1) (solo bersagli volanti)', 0, 0, 0, 0, 0, 3, 10, 1, null, null, 5, 1, 1, false, null),
    ('Fanteria', 'Scaricatore EMP', 8, 'Disabilita veicoli/componenti in area 3x3 | (max 1 per volta)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Fanteria', 'Scudo Energizzato', 8, '+4 armor per un turno | (3/3)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 3, 1, false, null),
    ('Fanteria', 'Rifornimento Condiviso', 9, 'Range 2 | Un alleato ottiene 2 rifornimenti | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, 2, 1, 1, false, null),
    ('Fanteria', 'Mitra Avanzato', 10, 'Perf. 3 | Range 6 | 3x3 danni | (1/1)', 0, 0, 0, 0, 0, 3, 3, 3, null, null, 6, 1, 1, false, null),
    ('Fanteria', 'Lanciamissili Tattico', 10, 'Perf. 4 | Range 6 | 8 danni (area 2x2) | (1/1)', 0, 0, 0, 0, 0, 4, 8, 1, 2, 2, 6, 1, 1, false, null),
    ('Fanteria', 'Kit da Cecchino Elite', 12, 'Perf. 5 | Range 8 | 11 danni | (1/1) (non può muoversi)', 0, 0, 0, 0, 0, 5, 11, 1, null, null, 8, 1, 1, true, null),
    ('Fanteria', 'Fucile a Reticolo', 14, 'Perf. 8 | Range 5 | 14 danni | Attraversa coperture | (1/1)', 0, 0, 0, 0, 0, 8, 14, 1, null, null, 5, 1, 1, false, null),
    ('Motorizzata', 'Cassone di Carico', 1, '+3 max rifornimenti', 0, 0, 0, 0, 3, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Rampa da Carico', 1, 'Può trasportare un''unità', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Corazza Balistica', 1, '+1 armor', 0, 0, 0, 1, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Motore a Diesel', 1, '+1 movimento', 0, 1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Parti di Ricambio', 1, '+2 HP', 2, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Scarico Avanzato', 1, 'Può cedere rifornimenti anche alle unità a 2 caselle di distanza', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Portapacchi Rinforzato', 2, '+2 max rifornimenti, +1 armor', 0, 0, 0, 1, 2, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Portapacchi Ampliato', 2, '+4 max rifornimenti', 0, 0, 0, 0, 4, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Scafo Rinforzato', 2, '+4 max HP', 4, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Motore 4x4', 2, '+2 movimento', 0, 2, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Gru Mobile', 2, 'Trasporta un''unità in più (richiede Rampa da Carico) (1→2)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, 'Rampa da Carico'),
    ('Motorizzata', 'Mitragliatrice Laterale', 2, 'Perf. 1 | Range 3 | 2 danni | (3/3)', 0, 0, 0, 0, 0, 1, 2, 1, null, null, 3, 3, 1, false, null),
    ('Motorizzata', 'Mitragliatrice Pesante', 3, 'Perf. 2 | Range 4 | 3 danni | (2/2)', 0, 0, 0, 0, 0, 2, 3, 1, null, null, 4, 2, 1, false, null),
    ('Motorizzata', 'Gomme Rinforzate', 3, '+2 armor', 0, 0, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Cucina da Campo', 3, 'Rigenera 1 HP/turno alle unità a bordo', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Faro Laser', 3, 'Marca una casella per 2 turni | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Motorizzata', 'Motore V8', 4, '+4 movimento', 0, 4, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Serbatoio Ausiliario', 4, '+4 max rifornimenti', 0, 0, 0, 0, 4, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Kit Riparazione Rapida', 4, 'Range 3 | Ripara 7 HP a veicoli alleati | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, 3, 2, 1, false, null),
    ('Motorizzata', 'Torretta Ricognizione', 4, '+2 visione', 0, 0, 2, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Ospedale da Campo', 4, 'Rigenera 2 HP/turno alle unità a bordo, -1 max rifornimenti', 0, 0, 0, 0, -1, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Torretta con Cannone Rotante', 5, 'Perf. 3 | Range 4 | 4 danni | (2/2)', 0, 0, 0, 0, 0, 3, 4, 1, null, null, 4, 2, 1, false, null),
    ('Motorizzata', 'Supporto Aereo', 5, 'Perf. 2 | Range 6 | 5 danni | (1/1) (solo su caselle marcate)', 0, 0, 0, 0, 0, 2, 5, 1, null, null, 6, 1, 1, false, null),
    ('Motorizzata', 'Piattaforma Lanciarazzi', 5, 'Perf. 1 | Range 4 | 3 danni (area 2x2) | (1/1)', 0, 0, 0, 0, 0, 1, 3, 1, 2, 2, 4, 1, 1, false, null),
    ('Motorizzata', 'Gru Pesante', 7, 'Trasporta 2 unità in più (richiede Gru Mobile) (2→4)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, 'Gru Mobile'),
    ('Motorizzata', 'Sistema Ponte Mobile', 7, 'Range 1 | Crea un ponte | (max 1 per volta)', 0, 0, 0, 0, 0, null, null, null, null, null, 1, null, 1, false, null),
    ('Motorizzata', 'Postazione Comando Mobile', 7, '+1 PU ogni 2 unità alleate rifornite', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Portacontainer', 10, '+11 max rifornimenti', 0, 0, 0, 0, 11, null, null, null, null, null, null, null, 1, false, null),
    ('Motorizzata', 'Doppio Motore', 10, '+4 movimento, +2 max rifornimenti', 0, 4, 0, 0, 2, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Armatura Leggera', 1, '+1 armor', 0, 0, 0, 1, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Fari IR', 1, '+1 visione', 0, 0, 1, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Parti di Ricambio', 1, '+2 HP', 2, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Radio Potenziata', 1, '+1 visione', 0, 0, 1, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Motore a Diesel', 1, '+1 movimento', 0, 1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Mitragliatrice Leggera', 2, 'Perf. 1 | Range 3 | 2 danni | (2/2)', 0, 0, 0, 0, 0, 1, 2, 1, null, null, 3, 2, 1, false, null),
    ('Corazzata', 'Fucile a Precisione per Equipaggio', 2, 'Perf. 2 | Range 4 | 2 danni | (1/1)', 0, 0, 0, 0, 0, 2, 2, 1, null, null, 4, 1, 1, false, null),
    ('Corazzata', 'Scudi Laterali', 2, '+2 armor', 0, 0, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Scafo Rinforzato', 2, '+4 max HP', 4, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Pneumatici Rinforzati', 2, '+2 armor', 0, 0, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Motore 4x4', 2, '+2 movimento', 0, 2, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Faro Laser', 2, 'Marca una casella per 2 turni | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Corazzata', 'Mitragliatrice Pesante', 3, 'Perf. 2 | Range 4 | 3 danni | (3/3)', 0, 0, 0, 0, 0, 2, 3, 1, null, null, 4, 3, 1, false, null),
    ('Corazzata', 'Razzi HE', 3, 'Perf. 1 | Range 5 | 4 danni | (1/1)', 0, 0, 0, 0, 0, 1, 4, 1, null, null, 5, 1, 1, false, null),
    ('Corazzata', 'Piattaforma Mobile', 3, 'Può trasportare 2 unità di fanteria', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Cannone Automatico 20mm', 4, 'Perf. 2 | Range 4 | 4 danni | (2/2)', 0, 0, 0, 0, 0, 2, 4, 1, null, null, 4, 2, 1, false, null),
    ('Corazzata', 'Lanciafiamme', 4, 'Perf. 1 | Range 2 | 3 danni (area 2x2) | (2/2)', 0, 0, 0, 0, 0, 1, 3, 1, 2, 2, 2, 2, 1, false, null),
    ('Corazzata', 'Kit da Ricognizione', 4, '+2 visione', 0, 0, 2, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Sistema di Guida', 4, '+1 movimento, +1 Perf', 0, 1, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Kit da Rally', 4, '+3 movimento per 1 turno | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Corazzata', 'Mitragliatrice Frontale', 5, 'Perf. 3 | Range 4 | 2x3 danni | (2/2)', 0, 0, 0, 0, 0, 3, 3, 2, null, null, 4, 2, 1, false, null),
    ('Corazzata', 'Lanciarazzi Multiplo', 5, 'Perf. 1 | Range 4 | 3x3 danni | (1/1)', 0, 0, 0, 0, 0, 1, 3, 3, null, null, 4, 1, 1, false, null),
    ('Corazzata', 'Sistema ECM', 5, '-2 precisione ai nemici in range 3', 0, 0, 0, 0, 0, null, null, null, null, null, 3, null, 1, false, null),
    ('Corazzata', 'Armatura Reattiva', 5, '+3 armor, -1 movimento (solo cingolati)', 0, -1, 0, 3, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Kit da Supporto', 5, 'Cura 1 HP/turno alla fanteria trasportata (solo semicingolati)', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Cannone 60mm', 6, 'Perf. 4 | Range 6 | 9 danni | (1/1)', 0, 0, 0, 0, 0, 4, 9, 1, null, null, 6, 1, 1, false, null),
    ('Corazzata', 'Munizioni Esplosive', 6, '+5 danni agli edifici', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Lanciafiamme Pesante', 7, 'Perf. 2 | Range 3 | 7 danni (area 2x2) | (2/2)', 0, 0, 0, 0, 0, 2, 7, 1, 2, 2, 3, 2, 1, false, null),
    ('Corazzata', 'Cannone da 105mm', 8, 'Perf. 5 | Range 7 | 11 danni | (1/1)', 0, 0, 0, 0, 0, 5, 11, 1, null, null, 7, 1, 1, false, null),
    ('Corazzata', 'Scudo Frontale', 8, '+4 armor', 0, 0, 0, 4, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Corazzata', 'Torretta Automatica 40mm', 9, 'Perf. 3 | Range 5 | 2x6 danni | (1/1)', 0, 0, 0, 0, 0, 3, 6, 2, null, null, 5, 1, 1, false, null),
    ('Corazzata', 'Cannone da 125mm', 12, 'Perf. 4 | Range 10 | 15 danni | (1/1) (ricarica: 2 rifornimenti)', 0, 0, 0, 0, 0, 4, 15, 1, null, null, 10, 1, 2, false, null),
    ('Corazzata', 'Sistema MLRS', 15, 'Perf. 2 | Range 8 | 5x4 danni | (1/1) (ricarica: 2 rifornimenti)', 0, 0, 0, 0, 0, 2, 4, 5, null, null, 8, 1, 2, false, null),
    ('Corazzata', 'Cannone a Ioni', 20, 'Perf. 6 | Range 8 | 20 danni | (1/1) (ricarica: 3 rifornimenti)', 0, 0, 0, 0, 0, 6, 20, 1, null, null, 8, 1, 3, false, null),
    ('Artiglieria', 'Fuoco di Correzione', 1, '+7 danni se il bersaglio è su una casella marcata', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Fari di Emergenza', 1, '+1 visione', 0, 0, 1, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Mortaio Leggero', 2, 'Perf. 2 | Range 5 | 6 danni | (2/2)', 0, 0, 0, 0, 0, 2, 6, 1, null, null, 5, 2, 1, false, null),
    ('Artiglieria', 'Radar', 2, '+2 visione', 0, 0, 2, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Espansione Deposito', 3, '+1 max rifornimenti', 0, 0, 0, 0, 1, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Posizione Camuffata', 3, '+2 armor', 0, 0, 0, 2, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Proiettili AP', 3, '+2 perforazione (solo cannoni) | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 2, 1, false, null),
    ('Artiglieria', 'Proiettili HE', 3, '+10 danni (solo contro strutture) (solo cannoni) | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 2, 1, false, null),
    ('Artiglieria', 'Munizioni Incendiarie', 3, '+1 danno, infligge "Bruciatura" (1 danno/turno) | (2/2)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 2, 1, false, null),
    ('Artiglieria', 'Missile MANPADS', 4, 'Perf. 3 | Range 5 | 10 danni | (1/1) (solo bersagli volanti)', 0, 0, 0, 0, 0, 3, 10, 1, null, null, 5, 1, 1, false, null),
    ('Artiglieria', 'Sistema di Fuga Rapida', 4, '+2 movimento per 1 turno | (1/1) (non può sparare)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Artiglieria', 'Piattaforma Stabilizzata', 5, '+1 perforazione, +1 danno', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Sistema di Puntamento Laser', 5, '+1 perforazione, +1 range', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Mortai Medi', 5, 'Perf. 2 | Range 6 | 5 danni (area 2x2) | (1/1) (non può muoversi)', 0, 0, 0, 0, 0, 2, 5, 1, 2, 2, 6, 1, 1, true, null),
    ('Artiglieria', 'Cannone da 75mm', 5, 'Perf. 2 | Range 5 | 9 danni | (3/3) (non può muoversi)', 0, 0, 0, 0, 0, 2, 9, 1, null, null, 5, 3, 1, true, null),
    ('Artiglieria', 'Cannone da 105mm', 6, 'Perf. 5 | Range 8 | 11 danni | (2/2) (non può muoversi)', 0, 0, 0, 0, 0, 5, 11, 1, null, null, 8, 2, 1, true, null),
    ('Artiglieria', 'Proiettili EMP', 6, 'Disabilita l''elettronica | (1/1)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 1, false, null),
    ('Artiglieria', 'Bunker Antiproiettile', 7, '+4 armor quando è fermo', 0, 0, 0, 0, 0, null, null, null, null, null, null, null, 1, false, null),
    ('Artiglieria', 'Cannone 120mm', 7, 'Perf. 3 | Range 9 | 7 danni | (2/2) (non può muoversi)', 0, 0, 0, 0, 0, 3, 7, 1, null, null, 9, 2, 1, true, null),
    ('Artiglieria', 'Cannone a Doppio Fuoco', 7, 'Perf. 3 | Range 7 | 2x4 danni | (1/1)', 0, 0, 0, 0, 0, 3, 4, 2, null, null, 7, 1, 1, false, null),
    ('Artiglieria', 'Cannone 155mm', 8, 'Perf. 4 | Range 10 | 8 danni (area 2x2) | (1/1) (non può muoversi) (ricarica: 2 rifornimenti)', 0, 0, 0, 0, 0, 4, 8, 1, 2, 2, 10, 1, 2, true, null),
    ('Artiglieria', 'Obice Pesante', 10, 'Perf. 5 | Range 12 | 9 danni (area 3x3) | (1/1) (non può muoversi) (ricarica: 2 rifornimenti)', 0, 0, 0, 0, 0, 5, 9, 1, 3, 3, 12, 1, 2, true, null),
    ('Artiglieria', 'Cannone Fisso 185mm', 15, 'Perf. 6 | Range 11 | 15 danni (linea 5 caselle) | (1/1) (non può muoversi per i 3 turni dopo il fuoco)', 0, 0, 0, 0, 0, 6, 15, 1, null, null, 11, 1, 1, true, null),
    ('Artiglieria', 'Testate Nucleari', 20, '+5 Perf, +10 danni | (1/1) (ricarica: 3 rifornimenti)', 0, 0, 0, 0, 0, null, null, null, null, null, null, 1, 3, false, null);

alter table public.game_settings enable row level security;
alter table public.rank_salaries enable row level security;
alter table public.unit_types enable row level security;
alter table public.game_components enable row level security;
alter table public.game_sites enable row level security;

create policy "Tutti vedono le impostazioni" on public.game_settings
    for select to anon, authenticated using (true);
create policy "Tutti vedono gli stipendi" on public.rank_salaries
    for select to anon, authenticated using (true);
create policy "Tutti vedono i tipi di unita" on public.unit_types
    for select to anon, authenticated using (true);
create policy "Tutti vedono i componenti" on public.game_components
    for select to anon, authenticated using (true);
create policy "Tutti vedono le strutture" on public.game_sites
    for select to anon, authenticated using (true);

grant select on public.game_settings, public.rank_salaries, public.unit_types,
    public.game_components, public.game_sites to anon, authenticated;

-- Costo dell'unità, che serve per restituire i PU
alter table public.game_units add column cost integer not null default 0;
alter table public.game_orders add column applied_at timestamptz;

update public.game_units u set cost = coalesce((select t.cost from public.unit_types t where t.name = u.type), 0)
    + coalesce((
        select sum(c.cost) from public.game_components c
        where c.type = u.type and c.name = any(u.components)
    ), 0);

-- Le unità già approvate entrano in gioco come soldati
update public.players p set rank = 'Soldato'
    where p.rank = 'Recluta'
    and exists (select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active');

update public.game_turn set hours = 24, started_at = now();

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.players (id, pu) values (
        new.id,
        coalesce((select value from public.game_settings where key = 'starting_pu'), 4)
    );
    return new;
end;
$$;

-- La mappa ora include anche la descrizione delle strutture
create or replace function public.get_map() returns jsonb
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
            select jsonb_agg(jsonb_build_object(
                'name', s.site, 'x', s.x, 'y', s.y, 'w', s.w, 'h', s.h,
                'description', coalesce(d.description, '')
            ) order by s.site)
            from (
                select site, min(x) as x, min(y) as y, max(x) - min(x) + 1 as w, max(y) - min(y) + 1 as h
                from public.game_map where site is not null group by site
            ) s
            left join public.game_sites d on d.name = s.site
        ), '[]'::jsonb)
    );
$$;

-- La nuova unità viene calcolata dal server: costo, HP, rifornimenti, utilizzi e porto.
drop function public.create_unit(text, text, text, text[], text, integer, integer, integer, jsonb, integer, integer);

create function public.create_unit(
    p_matricola text, p_player_name text, p_type text, p_components text[], p_lore text
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
        owner_id, matricola, player_name, type, components, lore,
        hp, supplies, uses, pos_x, pos_y, cost
    ) values (
        auth.uid(), p_matricola, p_player_name, p_type, p_components, p_lore,
        greatest(v_type.hp + v_hp, 1), greatest(v_type.supplies + v_supplies, 0), v_uses, v_x, v_y, v_cost
    );
end;
$$;

-- Acquisto di un componente con i PU rimasti
create function public.buy_component(p_name text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_component public.game_components;
begin
    select * into v_unit from public.game_units
        where owner_id = auth.uid() and status in ('pending', 'active') for update;
    if v_unit.id is null then raise exception 'Serve una unita'; end if;

    select * into v_component from public.game_components where type = v_unit.type and name = p_name;
    if v_component.name is null then raise exception 'Componente non disponibile'; end if;
    if p_name = any(v_unit.components) then raise exception 'Componente gia posseduto'; end if;
    if v_component.requires is not null and not exists (
        select 1 from unnest(v_unit.components) as o where lower(o) = lower(v_component.requires)
    ) then
        raise exception 'Prerequisito mancante';
    end if;

    update public.players set pu = pu - v_component.cost where id = auth.uid() and pu >= v_component.cost;
    if not found then raise exception 'PU insufficienti'; end if;

    update public.game_units set
        components = components || p_name,
        hp = greatest(hp + v_component.hp, 1),
        supplies = greatest(supplies + v_component.supplies, 0),
        uses = case when v_component.max_uses is null then uses
            else uses || jsonb_build_object(p_name, v_component.max_uses) end,
        cost = cost + v_component.cost
        where id = v_unit.id;
end;
$$;

-- Passa al turno successivo e paga gli stipendi ai player con un'unità attiva
create function public.pay_and_advance(p_started_at timestamptz) returns void
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_turn set turn = turn + 1, started_at = p_started_at;
    update public.players p set pu = p.pu + s.pu
        from public.rank_salaries s
        where s.rank = p.rank
        and exists (select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active');
end;
$$;

-- Eseguita dal server ogni minuto
create function public.advance_turn_if_due() returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_turn public.game_turn;
begin
    select * into v_turn from public.game_turn for update;
    while v_turn.started_at + make_interval(hours => v_turn.hours) <= now() loop
        perform public.pay_and_advance(v_turn.started_at + make_interval(hours => v_turn.hours));
        select * into v_turn from public.game_turn;
    end loop;
end;
$$;

create function public.is_gm() returns boolean
language sql stable security definer set search_path = '' as $$
    select coalesce((select rank = 'Comando Supremo' from public.players where id = auth.uid()), false);
$$;

-- Il Comando Supremo non ha unità: chi riceve il grado perde quella che ha
create function public.retire_supreme_command_unit() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_units set status = 'lost'
        where owner_id = new.id and status in ('pending', 'active');
    return new;
end;
$$;

create trigger on_supreme_command_rank after update of rank on public.players
    for each row when (new.rank = 'Comando Supremo')
    execute function public.retire_supreme_command_unit();

update public.game_units u set status = 'lost'
    from public.players p
    where p.id = u.owner_id and p.rank = 'Comando Supremo' and u.status in ('pending', 'active');

-- Senza unità, il Comando Supremo scrive in chat con il proprio grado
create or replace function public.send_chat(p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
    v_author text;
begin
    select id, matricola into v_unit, v_author from public.game_units
        where owner_id = auth.uid() and status in ('pending', 'active');
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

-- Funzioni riservate ai Game Master: tutte controllano il ruolo sul server
create function public.gm_units() returns table (
    matricola text, player_name text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean,
    status text, cost integer
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, u.player_name, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status, u.cost
        from public.game_units u
        join public.players p on p.id = u.owner_id
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

create function public.gm_orders() returns table (matricola text, turn integer, orders jsonb)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, o.turn, o.orders
        from public.game_orders o
        join public.game_units u on u.id = o.unit_id
        where o.applied_at is null and u.status = 'active'
        order by o.turn, u.matricola;
end;
$$;

-- Approvando, la recluta diventa soldato. Rifiutando, tornano tutti i PU spesi.
create function public.gm_review_unit(p_matricola text, p_approve boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    select * into v_unit from public.game_units
        where matricola = p_matricola and status = 'pending' for update;
    if v_unit.id is null then raise exception 'Unita non in attesa'; end if;

    if p_approve then
        update public.game_units set status = 'active' where id = v_unit.id;
        update public.players set rank = 'Soldato' where id = v_unit.owner_id and rank = 'Recluta';
    else
        update public.game_units set status = 'rejected' where id = v_unit.id;
        update public.players set pu = pu + v_unit.cost where id = v_unit.owner_id;
    end if;
end;
$$;

create function public.gm_update_unit(p_matricola text, p_hp integer, p_supplies integer) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    update public.game_units set hp = p_hp, supplies = p_supplies
        where matricola = p_matricola and status in ('pending', 'active');
end;
$$;

-- Unità persa: il player tiene PU e grado, i componenti sono persi
create function public.gm_mark_lost(p_matricola text) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    update public.game_units set status = 'lost'
        where matricola = p_matricola and status = 'active';
end;
$$;

-- Applica il risultato degli ordini, calcolato con le stesse regole dei player
create function public.gm_apply_orders(
    p_matricola text, p_turn integer, p_hp integer, p_supplies integer, p_uses jsonb,
    p_x integer, p_y integer, p_transported boolean
) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    select id into v_unit from public.game_units where matricola = p_matricola and status = 'active';
    if v_unit is null then raise exception 'Unita non attiva'; end if;

    update public.game_units set hp = p_hp, supplies = p_supplies, uses = p_uses,
        pos_x = p_x, pos_y = p_y, transported = p_transported
        where id = v_unit;
    update public.game_orders set applied_at = now() where unit_id = v_unit and turn = p_turn;
end;
$$;

create function public.gm_next_turn() returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    perform public.pay_and_advance(now());
end;
$$;

revoke all on function public.retire_supreme_command_unit() from public, anon, authenticated;
revoke all on function public.create_unit(text, text, text, text[], text) from public, anon;
revoke all on function public.buy_component(text) from public, anon;
revoke all on function public.pay_and_advance(timestamptz) from public, anon, authenticated;
revoke all on function public.advance_turn_if_due() from public, anon, authenticated;
revoke all on function public.is_gm() from public, anon;
revoke all on function public.gm_units() from public, anon;
revoke all on function public.gm_orders() from public, anon;
revoke all on function public.gm_review_unit(text, boolean) from public, anon;
revoke all on function public.gm_update_unit(text, integer, integer) from public, anon;
revoke all on function public.gm_mark_lost(text) from public, anon;
revoke all on function public.gm_apply_orders(text, integer, integer, integer, jsonb, integer, integer, boolean) from public, anon;
revoke all on function public.gm_next_turn() from public, anon;

grant execute on function public.create_unit(text, text, text, text[], text) to authenticated;
grant execute on function public.buy_component(text) to authenticated;
grant execute on function public.is_gm() to authenticated;
grant execute on function public.gm_units() to authenticated;
grant execute on function public.gm_orders() to authenticated;
grant execute on function public.gm_review_unit(text, boolean) to authenticated;
grant execute on function public.gm_update_unit(text, integer, integer) to authenticated;
grant execute on function public.gm_mark_lost(text) to authenticated;
grant execute on function public.gm_apply_orders(text, integer, integer, integer, jsonb, integer, integer, boolean) to authenticated;
grant execute on function public.gm_next_turn() to authenticated;

commit;

-- Cambio turno automatico: il server controlla ogni minuto
create extension if not exists pg_cron with schema pg_catalog;

grant usage on schema cron to postgres;
grant all privileges on all tables in schema cron to postgres;

select cron.schedule('sector-cambio-turno', '* * * * *', 'select public.advance_turn_if_due()');
