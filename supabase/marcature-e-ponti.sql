-- Caselle marcate, ponti permanenti, armatura di Bunker e Sistema ECM, descrizioni uniformi.
-- Da eseguire dopo componenti-passivi.sql.

begin;

-- marks: marca la casella bersaglio; needs_mark: spara solo su caselle marcate
-- mark_damage: danni in più sulle caselle marcate; builds_bridge: costruisce un ponte sul fiume
-- still_armor: armatura in più se l'unità non si è mossa nel turno
-- aura_armor e aura_range: armatura in più agli alleati entro quelle caselle
alter table public.game_components
    add column marks boolean not null default false,
    add column needs_mark boolean not null default false,
    add column mark_damage integer not null default 0,
    add column builds_bridge boolean not null default false,
    add column still_armor integer not null default 0,
    add column aura_armor integer not null default 0,
    add column aura_range integer;

-- Il turno in cui l'unità si è spostata. Un movimento eseguito a fine turno vale per il turno che inizia.
alter table public.game_units add column moved_turn integer;

update public.game_components set marks = true, range = 4 where name = 'Faro Laser';
update public.game_components set needs_mark = true, manual = false where name = 'Supporto Aereo';
update public.game_components set mark_damage = 7 where name = 'Fuoco di Correzione';
update public.game_components set builds_bridge = true, range = 1, max_uses = 1 where name = 'Sistema Ponte Mobile';
update public.game_components set max_uses = 1 where name = 'Scaricatore EMP';
update public.game_components set still_armor = 4 where name = 'Bunker Antiproiettile';
update public.game_components set aura_armor = 1, aura_range = 3 where name = 'Sistema ECM';

-- Chi aveva già un componente diventato attivo riceve i suoi utilizzi
update public.game_units u set uses = u.uses || jsonb_build_object(c.name, c.max_uses)
    from public.game_components c
    where c.type = u.type and c.name in ('Scaricatore EMP', 'Sistema Ponte Mobile') and c.name = any(u.components);

-- Stesse parole per le stesse cose: Perf, Range, danni, armor, veicoli, a bordo, (x/y) per gli utilizzi
update public.game_components c set description = v.description
from (values
    ('Artiglieria', 'Proiettili AP', '+2 Perf (solo cannoni) | (2/2)'),
    ('Artiglieria', 'Piattaforma Stabilizzata', '+1 Perf, +1 danno'),
    ('Artiglieria', 'Sistema di Puntamento Laser', '+1 Perf, +1 Range'),
    ('Artiglieria', 'Bunker Antiproiettile', '+4 armor se non si è mosso nel turno'),
    ('Corazzata', 'Faro Laser', 'Range 4 | Marca una casella per 2 turni | (1/1)'),
    ('Corazzata', 'Scafo Rinforzato', '+4 HP'),
    ('Corazzata', 'Piattaforma Mobile', 'Trasporta 2 unità di fanteria'),
    ('Corazzata', 'Armatura Reattiva', '+3 armor, -1 movimento'),
    ('Corazzata', 'Kit da Supporto', 'Rigenera 1 HP/turno alla fanteria a bordo'),
    ('Corazzata', 'Sistema ECM', '+1 armor agli alleati entro 3 caselle'),
    ('Fanteria', 'Faro Laser', 'Range 4 | Marca una casella per 2 turni | (1/1)'),
    ('Fanteria', 'Siringa Stim', '+1 movimento per 1 turno | (2/2)'),
    ('Fanteria', 'Kit Riparazione', 'Range 1 | Ripara 8 HP ai veicoli alleati | (2/2)'),
    ('Fanteria', 'Mirino Digitale', '+1 Range, +1 Perf a lungo raggio (distanza ≥4) (solo armi da fuoco)'),
    ('Fanteria', 'Mirino Laser', '+2 Perf a corto raggio (distanza ≤3) (solo armi da fuoco)'),
    ('Fanteria', 'Mirino ACOG', '+2 Perf a lungo raggio (distanza ≥4) (solo armi da fuoco)'),
    ('Fanteria', 'Proiettili Esplosivi', '+1 danno (solo armi da fuoco)'),
    ('Fanteria', 'Trappola Esplosiva', 'Perf. 3 | 4 danni | (1/1) (attiva al passaggio)'),
    ('Fanteria', 'Drone Spotter', 'Vola su ostacoli, +2 visione | (3/3)'),
    ('Fanteria', 'Generatore di Scudi', '+2 armor'),
    ('Fanteria', 'Scaricatore EMP', 'Disabilita veicoli e componenti in area 3x3 | (1/1)'),
    ('Fanteria', 'Scudo Energizzato', '+4 armor per 1 turno | (3/3)'),
    ('Fanteria', 'Fucile a Reticolo', 'Perf. 8 | Range 5 | 14 danni | (1/1) (attraversa coperture)'),
    ('Motorizzata', 'Faro Laser', 'Range 4 | Marca una casella per 2 turni | (1/1)'),
    ('Motorizzata', 'Rampa da Carico', 'Trasporta 1 unità'),
    ('Motorizzata', 'Gru Mobile', 'Trasporta 1 unità in più (richiede Rampa da Carico) (1→2)'),
    ('Motorizzata', 'Scafo Rinforzato', '+4 HP'),
    ('Motorizzata', 'Kit Riparazione Rapida', 'Range 3 | Ripara 7 HP ai veicoli alleati | (2/2)'),
    ('Motorizzata', 'Postazione Comando Mobile', '+1 PU ogni 2 unità alleate rifornite nello stesso turno'),
    ('Motorizzata', 'Sistema Ponte Mobile', 'Range 1 | Crea un ponte permanente su un fiume | (1/1)')
) as v(type, name, description)
where c.type = v.type and c.name = v.name;

-- Caselle marcate: tutti vedono il flare sulla mappa
create table public.game_marks (
    x integer not null,
    y integer not null,
    expires_turn integer not null,
    primary key (x, y)
);
alter table public.game_marks enable row level security;
revoke all on public.game_marks from anon, authenticated;
create policy "Tutti vedono le marcature" on public.game_marks for select to anon, authenticated using (true);
grant select on public.game_marks to anon, authenticated;

-- I telefoni riscaricano la mappa quando questo numero cambia
insert into public.game_settings (key, value, description)
    values ('map_version', 1, 'Aumenta quando cambia la mappa, per esempio con un ponte nuovo');

drop function public.weapon_bonus(public.game_units, public.game_components, integer);

create function public.weapon_bonus(p_unit public.game_units, p_weapon public.game_components, p_distance integer)
returns table (perf integer, damage integer, reach integer, marked integer)
language sql stable security definer set search_path = '' as $$
    select coalesce(sum(b.bonus_perf) filter (
               where p_distance >= coalesce(b.perf_from, p_distance)
               and p_distance <= coalesce(b.perf_to, p_distance)
           ), 0)::integer,
           coalesce(sum(b.bonus_damage), 0)::integer,
           coalesce(sum(b.bonus_range), 0)::integer,
           coalesce(sum(b.mark_damage), 0)::integer
    from public.game_components b
    where b.type = p_unit.type and b.name = any(p_unit.components)
    and (not b.for_firearms or p_weapon.firearm);
$$;

-- Armatura in più di questo momento: Bunker se non si è mossa nel turno, Sistema ECM degli alleati vicini.
-- Serve al GM quando risolve gli attacchi dei nemici.
create function public.armor_bonus(p_unit public.game_units) returns integer
language sql stable security definer set search_path = '' as $$
    select coalesce((
            select sum(c.still_armor)
            from public.game_components c
            where c.type = p_unit.type and c.name = any(p_unit.components)
            and p_unit.moved_turn is distinct from (select t.turn from public.game_turn t)
        ), 0)::integer
        + coalesce((
            select sum(c.aura_armor)
            from public.game_units o
            join public.game_components c on c.type = o.type and c.name = any(o.components)
            where o.status = 'active' and o.id <> p_unit.id and c.aura_armor > 0
            and abs(o.pos_x - p_unit.pos_x) + abs(o.pos_y - p_unit.pos_y) <= c.aura_range
        ), 0)::integer;
$$;

create or replace function public.execute_orders(p_unit uuid, p_turn integer, p_orders jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_max record;
    v_bonus record;
    v_order jsonb;
    v_type text;
    v_name text;
    v_x integer;
    v_y integer;
    v_item public.game_components;
    v_uses integer;
    v_armed boolean;
    v_distance integer;
    v_reach integer;
    v_marked boolean;
    v_moved boolean := false;
    v_still boolean := false;
    v_used text[] := '{}';
    v_report text[] := '{}';
    v_line text;
    v_why text;
begin
    select * into v_unit from public.game_units where id = p_unit and status = 'active' for update;
    if v_unit.id is null then return; end if;
    select * into v_max from public.unit_stats(v_unit.type, v_unit.components);

    for v_order in select e.value from jsonb_array_elements(p_orders) as e loop
        v_type := v_order ->> 'type';
        v_name := v_order ->> 'component';
        v_x := (v_order -> 'target' ->> 'x')::integer;
        v_y := (v_order -> 'target' ->> 'y')::integer;
        v_why := null;

        select * into v_item from public.game_components c where c.type = v_unit.type and c.name = v_name;
        v_uses := coalesce((v_unit.uses ->> v_name)::integer, 0);

        if v_type = 'MOVE' then
            v_line := 'Movimento a ' || coalesce(v_x || ',' || v_y, '?');
            if v_x is null or v_y is null then v_why := 'casella mancante';
            elsif v_unit.transported then v_why := 'sei a bordo di un mezzo';
            elsif v_moved then v_why := 'massimo 1 movimento per turno';
            elsif v_still then v_why := 'hai usato un''arma che richiede di restare fermi';
            elsif v_x = v_unit.pos_x and v_y = v_unit.pos_y then v_why := 'sei già su quella casella';
            elsif public.path_length(v_unit.pos_x, v_unit.pos_y, v_x, v_y, v_max.movement, v_unit.id) is null then
                v_why := 'troppo lontano o percorso bloccato';
            else
                v_unit.pos_x := v_x;
                v_unit.pos_y := v_y;
                v_unit.moved_turn := p_turn + 1;
                v_moved := true;
                v_line := v_line || ': fatto';
            end if;

        elsif v_type = 'ACTIVATE' then
            v_armed := v_item.perforation is not null and v_item.damage is not null;
            v_distance := abs(v_x - v_unit.pos_x) + abs(v_y - v_unit.pos_y);
            select * into v_bonus from public.weapon_bonus(v_unit, v_item, coalesce(v_distance, 0));
            v_reach := v_item.range + case when v_armed then v_bonus.reach else 0 end;
            v_marked := exists (select 1 from public.game_marks m where m.x = v_x and m.y = v_y);
            v_line := coalesce(v_name, 'Componente');
            if v_x is not null then v_line := v_line || ' su ' || v_x || ',' || v_y; end if;

            if v_item.name is null or not (v_name = any(v_unit.components)) then v_why := 'componente non posseduto';
            elsif v_item.max_uses is null then v_why := 'componente passivo';
            elsif v_name = any(v_used) then v_why := 'si attiva una volta per turno';
            elsif v_uses <= 0 then v_why := 'scarico';
            elsif v_armed and v_unit.transported then v_why := 'a bordo non si attacca';
            elsif v_item.stationary and v_moved then v_why := 'non si usa dopo essersi mossi';
            elsif v_item.range is not null and v_x is null then v_why := 'bersaglio mancante';
            elsif v_item.range is not null and v_distance > v_reach then v_why := 'bersaglio fuori portata';
            elsif v_item.needs_mark and not v_marked then v_why := 'la casella non è marcata';
            elsif v_item.builds_bridge and not exists (
                select 1 from public.game_map m where m.x = v_x and m.y = v_y and m.terrain = 'fiume'
            ) then
                v_why := 'il ponte si costruisce solo sul fiume';
            elsif v_armed and v_x is not null
                and public.line_blocked(v_unit.pos_x, v_unit.pos_y, v_x, v_y) then
                v_why := 'un''altra unità blocca il tiro';
            else
                v_used := array_append(v_used, v_name);
                v_still := v_still or v_item.stationary;
                v_unit.uses := v_unit.uses || jsonb_build_object(v_name, v_uses - 1);

                if v_item.marks then
                    insert into public.game_marks (x, y, expires_turn) values (v_x, v_y, p_turn + 2)
                        on conflict (x, y) do update
                        set expires_turn = greatest(public.game_marks.expires_turn, excluded.expires_turn);
                    v_line := v_line || ': casella marcata';
                elsif v_item.builds_bridge then
                    update public.game_map set terrain = 'ponte' where x = v_x and y = v_y;
                    update public.game_settings set value = value + 1 where key = 'map_version';
                    v_line := v_line || ': ponte costruito';
                elsif v_armed and not v_item.manual and v_x is not null then
                    v_item.perforation := greatest(v_item.perforation + v_bonus.perf, 0);
                    v_item.damage := v_item.damage + v_bonus.damage
                        + case when v_marked then v_bonus.marked else 0 end;
                    v_line := v_line || ': ' || public.fire(v_item, v_x, v_y);
                else
                    v_line := v_line || ': usato, l''effetto lo applica il Comando Supremo';
                    insert into public.notifications (player_id, kind, title, body)
                        select p.id, 'ordini', 'Effetto da applicare',
                            left(v_unit.matricola || ' da ' || v_unit.pos_x || ',' || v_unit.pos_y || ': '
                                || v_line || '. ' || v_item.description, 1000)
                        from public.players p
                        where p.rank = 'Comando Supremo';
                end if;
            end if;

        elsif v_type = 'RELOAD' then
            v_line := 'Ricarica ' || coalesce(v_name, 'componente');
            if v_item.name is null or not (v_name = any(v_unit.components)) then v_why := 'componente non posseduto';
            elsif v_item.max_uses is null then v_why := 'componente passivo';
            elsif v_uses >= v_item.max_uses then v_why := 'già carico';
            elsif v_unit.supplies < v_item.reload_cost then v_why := 'rifornimenti insufficienti';
            else
                v_unit.supplies := v_unit.supplies - v_item.reload_cost;
                v_unit.uses := v_unit.uses || jsonb_build_object(v_name, v_item.max_uses);
                v_line := v_line || ': fatta';
            end if;

        elsif v_type = 'HEAL' then
            v_line := 'Cura';
            if v_unit.supplies < 1 then v_why := 'rifornimenti insufficienti';
            elsif v_unit.hp >= v_max.hp then v_why := 'HP già al massimo';
            else
                v_unit.supplies := v_unit.supplies - 1;
                v_unit.hp := v_max.hp;
                v_line := v_line || ': fatta';
            end if;

        else
            v_line := 'Ordine ' || coalesce(v_type, '?');
            v_why := 'non si esegue a fine turno';
        end if;

        if v_why is not null then v_line := v_line || ': non eseguito, ' || v_why; end if;
        v_report := array_append(v_report, v_line);
    end loop;

    update public.game_units set hp = v_unit.hp, supplies = v_unit.supplies, uses = v_unit.uses,
        pos_x = v_unit.pos_x, pos_y = v_unit.pos_y, moved_turn = v_unit.moved_turn
        where id = p_unit;

    if cardinality(v_report) > 0 then
        insert into public.notifications (player_id, kind, title, body)
            values (v_unit.owner_id, 'ordini', 'Ordini del turno ' || p_turn,
                left(array_to_string(v_report, E'\n'), 1000));
    end if;
end;
$$;

-- Una marcatura fatta a fine turno N vale per i turni N+1 e N+2, poi sparisce
create or replace function public.pay_and_advance(p_started_at timestamptz) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_turn integer;
begin
    perform public.execute_turn_orders();
    perform public.heal_passengers();

    update public.game_turn set turn = turn + 1, started_at = p_started_at where id returning turn into v_turn;
    update public.players p set pu = p.pu + s.pu
        from public.rank_salaries s
        where s.rank = p.rank
        and exists (select 1 from public.game_units u where u.owner_id = p.id and u.status = 'active');

    insert into public.notifications (player_id, kind, title, body)
        select p.id, 'turno', 'Turno ' || v_turn,
            case when u.id is null then 'È iniziato un nuovo turno.'
                else 'È iniziato un nuovo turno. Stipendio: ' || coalesce(s.pu, 0) || ' PU.' end
        from public.players p
        left join public.game_units u on u.owner_id = p.id and u.status = 'active'
        left join public.rank_salaries s on s.rank = p.rank
        where u.id is not null or p.rank = 'Comando Supremo';

    delete from public.game_marks where expires_turn < v_turn;
    delete from public.supply_log where turn < v_turn;
    delete from public.notifications where created_at < now() - interval '30 days';
end;
$$;

-- Salire, scendere ed essere trasportati contano come movimento
create or replace function public.follow_vehicle() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_units set pos_x = new.pos_x, pos_y = new.pos_y, moved_turn = new.moved_turn
        where carried_by = new.id;
    return null;
end;
$$;

create or replace function public.board_vehicle(p_matricola text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_me public.game_units;
    v_vehicle public.game_units;
begin
    select * into v_me from public.game_units where owner_id = auth.uid() and status = 'active' for update;
    if v_me.id is null then raise exception 'Unita non attiva'; end if;
    if v_me.transported then raise exception 'Gia a bordo'; end if;
    if exists (select 1 from public.game_units u where u.carried_by = v_me.id) then
        raise exception 'Hai unita a bordo';
    end if;

    select * into v_vehicle from public.game_units where matricola = p_matricola and status = 'active' for update;
    if v_vehicle.id is null or v_vehicle.id = v_me.id or v_vehicle.transported then
        raise exception 'Mezzo non disponibile';
    end if;
    if abs(v_vehicle.pos_x - v_me.pos_x) + abs(v_vehicle.pos_y - v_me.pos_y) <> 1 then
        raise exception 'Mezzo non adiacente';
    end if;
    if public.free_seats(v_vehicle.id, v_me.type) < 1 then raise exception 'Mezzo al completo'; end if;

    update public.game_units set pos_x = v_vehicle.pos_x, pos_y = v_vehicle.pos_y,
        transported = true, carried_by = v_vehicle.id, moved_turn = (select t.turn from public.game_turn t)
        where id = v_me.id;
    perform public.notify(v_vehicle.owner_id, 'ordini', 'Unità a bordo', v_me.matricola || ' è salita sul tuo mezzo.');
end;
$$;

create or replace function public.leave_vehicle(p_x integer, p_y integer) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_me public.game_units;
begin
    select * into v_me from public.game_units where owner_id = auth.uid() and status = 'active' for update;
    if v_me.id is null then raise exception 'Unita non attiva'; end if;
    if not v_me.transported then raise exception 'Non a bordo'; end if;
    if abs(p_x - v_me.pos_x) + abs(p_y - v_me.pos_y) <> 1 then raise exception 'Casella non adiacente'; end if;
    if not public.cell_free(p_x, p_y, v_me.id) then raise exception 'Casella occupata'; end if;

    update public.game_units set pos_x = p_x, pos_y = p_y, transported = false, carried_by = null,
        moved_turn = (select t.turn from public.game_turn t)
        where id = v_me.id;
end;
$$;

drop function public.get_team();

create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean, status text,
    group_leader text, group_name text, squadron_leader text, squadron_name text, carried_by text,
    armor_bonus integer
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
           case p.rank when 'Caporale' then u.matricola when 'Soldato' then l.matricola end,
           case p.rank when 'Caporale' then u.squad_name when 'Soldato' then l.squad_name end,
           case p.rank when 'Capitano' then u.matricola when 'Caporale' then l.matricola when 'Soldato' then c.matricola end,
           case p.rank when 'Capitano' then u.squad_name when 'Caporale' then l.squad_name when 'Soldato' then c.squad_name end,
           m.matricola,
           public.armor_bonus(u)
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.rank_salaries r on r.rank = p.rank
    left join public.game_units l on l.id = u.squad_of
    left join public.game_units c on c.id = l.squad_of
    left join public.game_units m on m.id = u.carried_by
    where u.status in ('pending', 'active')
    order by r.position desc nulls last, u.matricola;
$$;

drop function public.gm_units();

create function public.gm_units() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean, status text,
    group_leader text, group_name text, squadron_leader text, squadron_name text, muted boolean,
    armor_bonus integer
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
               u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
               case p.rank when 'Caporale' then u.matricola when 'Soldato' then l.matricola end,
               case p.rank when 'Caporale' then u.squad_name when 'Soldato' then l.squad_name end,
               case p.rank when 'Capitano' then u.matricola when 'Caporale' then l.matricola when 'Soldato' then c.matricola end,
               case p.rank when 'Capitano' then u.squad_name when 'Caporale' then l.squad_name when 'Soldato' then c.squad_name end,
               p.muted,
               public.armor_bonus(u)
        from public.game_units u
        join public.players p on p.id = u.owner_id
        left join public.game_units l on l.id = u.squad_of
        left join public.game_units c on c.id = l.squad_of
        where u.status in ('pending', 'active')
        order by u.status desc, u.matricola;
end;
$$;

revoke all on function public.weapon_bonus(public.game_units, public.game_components, integer) from public, anon, authenticated;
revoke all on function public.armor_bonus(public.game_units) from public, anon, authenticated;
revoke all on function public.get_team() from public;
revoke all on function public.gm_units() from public, anon;

grant execute on function public.get_team() to anon, authenticated;
grant execute on function public.gm_units() to authenticated;

commit;
