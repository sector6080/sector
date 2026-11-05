-- Ordini eseguiti dal server. Salire, scendere e cedere rifornimenti partono subito.
-- Movimento, attivazione, ricarica e cura partono allo scadere del turno, con le regole ricontrollate.
-- Le armi applicano da sole perforazione, armatura, danni e area. Il GM non applica più gli ordini.
-- Da eseguire dopo voti-e-chiusura.sql.

begin;

-- A bordo di quale mezzo si trova un'unità
alter table public.game_units add column carried_by uuid references public.game_units (id) on delete set null;

-- capacity: posti del mezzo; carries: solo quel tipo di unità; manual: l'effetto lo applica il GM
alter table public.game_components
    add column capacity integer not null default 0,
    add column carries text,
    add column manual boolean not null default false;

update public.game_components set capacity = 1 where name in ('Rampa da Carico', 'Gru Mobile');
update public.game_components set capacity = 2 where name = 'Gru Pesante';
update public.game_components set capacity = 2, carries = 'Fanteria' where name = 'Piattaforma Mobile';
update public.game_components set manual = true
    where name in ('Kit da Demolitore', 'Missile MANPADS', 'Supporto Aereo', 'Trappola Esplosiva');

create function public.unit_stats(p_type text, p_components text[])
returns table (hp integer, movement integer, supplies integer)
language sql stable security definer set search_path = '' as $$
    select greatest(t.hp + coalesce(sum(c.hp), 0)::integer, 1),
           greatest(t.movement + coalesce(sum(c.movement), 0)::integer, 0),
           greatest(t.supplies + coalesce(sum(c.supplies), 0)::integer, 0)
    from public.unit_types t
    left join public.game_components c on c.type = t.name and c.name = any(p_components)
    where t.name = p_type
    group by t.hp, t.movement, t.supplies;
$$;

create function public.free_seats(p_vehicle uuid, p_passenger_type text) returns integer
language sql stable security definer set search_path = '' as $$
    select coalesce(sum(c.capacity), 0)::integer
        - (select count(*)::integer from public.game_units b where b.carried_by = p_vehicle and b.status = 'active')
    from public.game_units v
    join public.game_components c on c.type = v.type and c.name = any(v.components)
    where v.id = p_vehicle and (c.carries is null or c.carries = p_passenger_type);
$$;

create function public.cell_free(p_x integer, p_y integer, p_self uuid) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists (
            select 1 from public.game_map m
            where m.x = p_x and m.y = p_y and m.terrain not in ('mare', 'fiume')
        )
        and not exists (
            select 1 from public.game_units u
            where u.status = 'active' and u.id <> p_self and u.pos_x = p_x and u.pos_y = p_y
        )
        and not exists (select 1 from public.game_enemies e where e.pos_x = p_x and e.pos_y = p_y);
$$;

-- Stesse regole di GameMap.steps nell'app: niente diagonali, acqua, unità e nemici
create function public.path_length(
    p_x1 integer, p_y1 integer, p_x2 integer, p_y2 integer, p_limit integer, p_self uuid
) returns integer
language plpgsql stable security definer set search_path = '' as $$
declare
    v_goal integer := p_x2 * 1000 + p_y2;
    v_seen integer[] := array[p_x1 * 1000 + p_y1];
    v_frontier integer[] := v_seen;
    v_next integer[];
begin
    for v_step in 1..p_limit loop
        select coalesce(array_agg(distinct n.code), '{}') into v_next
        from unnest(v_frontier) as f(code)
        cross join lateral (values
            (f.code / 1000 + 1, f.code % 1000),
            (f.code / 1000 - 1, f.code % 1000),
            (f.code / 1000, f.code % 1000 + 1),
            (f.code / 1000, f.code % 1000 - 1)
        ) as c(x, y)
        cross join lateral (select c.x * 1000 + c.y as code) as n
        where not (n.code = any(v_seen)) and public.cell_free(c.x, c.y, p_self);

        if v_goal = any(v_next) then return v_step; end if;
        exit when cardinality(v_next) = 0;
        v_seen := v_seen || v_next;
        v_frontier := v_next;
    end loop;
    return null;
end;
$$;

-- Stesse regole di GameMap.isLineBlocked nell'app
create function public.line_blocked(p_x1 integer, p_y1 integer, p_x2 integer, p_y2 integer) returns boolean
language sql stable security definer set search_path = '' as $$
    select exists (
        select 1
        from (select greatest(abs(p_x2 - p_x1), abs(p_y2 - p_y1)) as n) as l
        cross join lateral generate_series(1, l.n - 1) as i
        cross join lateral (select
            floor(p_x1 + (p_x2 - p_x1)::numeric * i / l.n + 0.5)::integer as x,
            floor(p_y1 + (p_y2 - p_y1)::numeric * i / l.n + 0.5)::integer as y
        ) as c
        where exists (select 1 from public.game_units u where u.status = 'active' and u.pos_x = c.x and u.pos_y = c.y)
           or exists (select 1 from public.game_enemies e where e.pos_x = c.x and e.pos_y = c.y)
    );
$$;

-- L'attacco fallisce se l'armatura supera la perforazione. Un'area 3x3 è centrata sul bersaglio,
-- una 2x2 parte dal bersaglio verso destra e verso il basso. Colpisce solo i nemici.
create function public.fire(p_weapon public.game_components, p_x integer, p_y integer) returns text
language plpgsql security definer set search_path = '' as $$
declare
    v_w integer := coalesce(p_weapon.area_w, 1);
    v_h integer := coalesce(p_weapon.area_h, 1);
    v_left integer := p_x - (v_w - 1) / 2;
    v_top integer := p_y - (v_h - 1) / 2;
    v_damage integer := p_weapon.damage * coalesce(p_weapon.hits, 1);
    v_enemy public.game_enemies;
    v_hits text[] := '{}';
begin
    for v_enemy in
        select * from public.game_enemies e
        where e.pos_x between v_left and v_left + v_w - 1
        and e.pos_y between v_top and v_top + v_h - 1
        order by e.id for update
    loop
        if v_enemy.armor > p_weapon.perforation then
            v_hits := array_append(v_hits, v_enemy.name || ' respinge il colpo');
        elsif v_enemy.hp <= v_damage then
            delete from public.game_enemies where id = v_enemy.id;
            v_hits := array_append(v_hits, v_enemy.name || ' distrutto');
        else
            update public.game_enemies set hp = hp - v_damage where id = v_enemy.id;
            v_hits := array_append(v_hits, v_enemy.name || ' -' || v_damage || ' HP');
        end if;
    end loop;

    if cardinality(v_hits) = 0 then return 'nessun bersaglio'; end if;
    return array_to_string(v_hits, ', ');
end;
$$;

-- Le stesse regole di OrderRules.simulate nell'app: un ordine non valido viene saltato e riportato
create function public.execute_orders(p_unit uuid, p_turn integer, p_orders jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit public.game_units;
    v_max record;
    v_order jsonb;
    v_type text;
    v_name text;
    v_x integer;
    v_y integer;
    v_item public.game_components;
    v_uses integer;
    v_armed boolean;
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
                v_moved := true;
                v_line := v_line || ': fatto';
            end if;

        elsif v_type = 'ACTIVATE' then
            v_armed := v_item.perforation is not null and v_item.damage is not null;
            v_line := coalesce(v_name, 'Componente');
            if v_x is not null then v_line := v_line || ' su ' || v_x || ',' || v_y; end if;

            if v_item.name is null or not (v_name = any(v_unit.components)) then v_why := 'componente non posseduto';
            elsif v_item.max_uses is null then v_why := 'componente passivo';
            elsif v_name = any(v_used) then v_why := 'si attiva una volta per turno';
            elsif v_uses <= 0 then v_why := 'scarico';
            elsif v_armed and v_unit.transported then v_why := 'a bordo non si attacca';
            elsif v_item.stationary and v_moved then v_why := 'non si usa dopo essersi mossi';
            elsif v_item.range is not null and v_x is null then v_why := 'bersaglio mancante';
            elsif v_item.range is not null
                and abs(v_x - v_unit.pos_x) + abs(v_y - v_unit.pos_y) > v_item.range then
                v_why := 'bersaglio fuori portata';
            elsif v_armed and v_x is not null
                and public.line_blocked(v_unit.pos_x, v_unit.pos_y, v_x, v_y) then
                v_why := 'un''altra unità blocca il tiro';
            else
                v_used := array_append(v_used, v_name);
                v_still := v_still or v_item.stationary;
                v_unit.uses := v_unit.uses || jsonb_build_object(v_name, v_uses - 1);

                if v_armed and not v_item.manual and v_x is not null then
                    v_line := v_line || ': ' || public.fire(v_item, v_x, v_y);
                else
                    v_line := v_line || ': usato, l''effetto lo applica il Comando Supremo';
                    insert into public.notifications (player_id, kind, title, body)
                        select p.id, 'ordini', 'Effetto da applicare',
                            left(v_unit.matricola || ': ' || v_line || '. ' || v_item.description, 1000)
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
        pos_x = v_unit.pos_x, pos_y = v_unit.pos_y
        where id = p_unit;

    if cardinality(v_report) > 0 then
        insert into public.notifications (player_id, kind, title, body)
            values (v_unit.owner_id, 'ordini', 'Ordini del turno ' || p_turn,
                left(array_to_string(v_report, E'\n'), 1000));
    end if;
end;
$$;

-- Chi invia prima agisce prima. Un ordine rotto non deve mai bloccare il cambio turno.
create function public.execute_turn_orders() returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_turn integer;
    v_row public.game_orders;
begin
    select turn into v_turn from public.game_turn for update;

    for v_row in
        select o.* from public.game_orders o
        join public.game_units u on u.id = o.unit_id
        where o.turn = v_turn and o.applied_at is null and u.status = 'active'
        order by o.sent_at, o.unit_id
    loop
        begin
            perform public.execute_orders(v_row.unit_id, v_turn, v_row.orders);
        exception when others then
            perform public.notify(
                (select owner_id from public.game_units where id = v_row.unit_id),
                'ordini',
                'Ordini del turno ' || v_turn,
                'Non è stato possibile eseguire i tuoi ordini.'
            );
        end;
        update public.game_orders set applied_at = now() where unit_id = v_row.unit_id and turn = v_turn;
    end loop;
end;
$$;

create or replace function public.pay_and_advance(p_started_at timestamptz) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_turn integer;
begin
    perform public.execute_turn_orders();

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

    delete from public.notifications where created_at < now() - interval '30 days';
end;
$$;

-- Il GM non applica più gli ordini: il resoconto arriva da execute_orders
drop trigger on_orders_applied_notify on public.game_orders;
drop function public.notify_orders_applied();
drop function public.gm_apply_orders(text, integer, integer, integer, jsonb, integer, integer, boolean);

create or replace function public.send_orders(p_orders jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
    v_turn integer;
begin
    if jsonb_typeof(p_orders) is distinct from 'array' or jsonb_array_length(p_orders) > 50 then
        raise exception 'Ordini non validi';
    end if;

    select id into v_unit from public.game_units
        where owner_id = auth.uid() and status = 'active';
    if v_unit is null then raise exception 'Unita non attiva'; end if;

    select turn into v_turn from public.game_turn;
    insert into public.game_orders (unit_id, turn, orders) values (v_unit, v_turn, p_orders)
        on conflict (unit_id, turn) do update set orders = excluded.orders, sent_at = now();
end;
$$;

-- Il GM vede gli ordini in attesa del turno corrente
create or replace function public.gm_orders() returns table (matricola text, turn integer, orders jsonb)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select u.matricola, o.turn, o.orders
        from public.game_orders o
        join public.game_units u on u.id = o.unit_id
        where o.applied_at is null and u.status = 'active'
        and o.turn = (select t.turn from public.game_turn t)
        order by u.matricola;
end;
$$;

-- Azioni immediate: non passano dalla catena degli ordini
create function public.board_vehicle(p_matricola text) returns void
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
        transported = true, carried_by = v_vehicle.id
        where id = v_me.id;
    perform public.notify(v_vehicle.owner_id, 'ordini', 'Unità a bordo', v_me.matricola || ' è salita sul tuo mezzo.');
end;
$$;

create function public.leave_vehicle(p_x integer, p_y integer) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_me public.game_units;
begin
    select * into v_me from public.game_units where owner_id = auth.uid() and status = 'active' for update;
    if v_me.id is null then raise exception 'Unita non attiva'; end if;
    if not v_me.transported then raise exception 'Non a bordo'; end if;
    if abs(p_x - v_me.pos_x) + abs(p_y - v_me.pos_y) <> 1 then raise exception 'Casella non adiacente'; end if;
    if not public.cell_free(p_x, p_y, v_me.id) then raise exception 'Casella occupata'; end if;

    update public.game_units set pos_x = p_x, pos_y = p_y, transported = false, carried_by = null
        where id = v_me.id;
end;
$$;

create function public.give_supplies(p_matricola text, p_quantity integer) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_me public.game_units;
    v_to public.game_units;
    v_reach integer;
    v_max integer;
begin
    if p_quantity is null or p_quantity < 1 then raise exception 'Quantita non valida'; end if;

    select * into v_me from public.game_units where owner_id = auth.uid() and status = 'active' for update;
    if v_me.id is null then raise exception 'Unita non attiva'; end if;

    select * into v_to from public.game_units where matricola = p_matricola and status = 'active' for update;
    if v_to.id is null or v_to.id = v_me.id then raise exception 'Destinatario non valido'; end if;

    v_reach := case when 'Scarico Avanzato' = any(v_me.components) then 2 else 1 end;
    if abs(v_to.pos_x - v_me.pos_x) + abs(v_to.pos_y - v_me.pos_y) > v_reach then
        raise exception 'Destinatario lontano';
    end if;
    if v_me.supplies < p_quantity then raise exception 'Rifornimenti insufficienti'; end if;

    select s.supplies into v_max from public.unit_stats(v_to.type, v_to.components) as s;
    if v_to.supplies + p_quantity > v_max then raise exception 'Destinatario pieno'; end if;

    update public.game_units set supplies = supplies - p_quantity where id = v_me.id;
    update public.game_units set supplies = supplies + p_quantity where id = v_to.id;
    perform public.notify(
        v_to.owner_id, 'ordini', 'Rifornimenti ricevuti',
        v_me.matricola || ' ti ha ceduto ' || p_quantity || ' rifornimenti.'
    );
end;
$$;

-- Il mezzo si muove come un'unità singola, carico compreso
create function public.follow_vehicle() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_units set pos_x = new.pos_x, pos_y = new.pos_y where carried_by = new.id;
    return null;
end;
$$;

create trigger on_vehicle_moved after update of pos_x, pos_y on public.game_units
    for each row when (old.pos_x is distinct from new.pos_x or old.pos_y is distinct from new.pos_y)
    execute function public.follow_vehicle();

-- Un mezzo perso lascia a terra chi trasporta, un'unità persa libera il suo posto
create function public.free_passengers() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    update public.game_units set transported = false, carried_by = null where carried_by = new.id or id = new.id;
    return null;
end;
$$;

create trigger on_unit_left_game after update of status on public.game_units
    for each row when (old.status in ('pending', 'active') and new.status in ('lost', 'rejected'))
    execute function public.free_passengers();

-- Il GM che sposta un'unità a bordo la fa scendere dal mezzo
create or replace function public.gm_move_unit(p_matricola text, p_x integer, p_y integer) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    if exists (
        select 1 from public.game_units u
        where u.pos_x = p_x and u.pos_y = p_y and u.status = 'active' and u.matricola <> p_matricola
    ) or exists (
        select 1 from public.game_enemies e where e.pos_x = p_x and e.pos_y = p_y
    ) then
        raise exception 'Casella occupata';
    end if;
    update public.game_units set pos_x = p_x, pos_y = p_y, transported = false, carried_by = null
        where matricola = p_matricola and status in ('pending', 'active');
end;
$$;

drop function public.get_team();

create function public.get_team() returns table (
    matricola text, rank text, pu integer, type text, components text[], lore text,
    hp integer, supplies integer, uses jsonb, pos_x integer, pos_y integer, transported boolean, status text,
    group_leader text, group_name text, squadron_leader text, squadron_name text, carried_by text
)
language sql stable security definer set search_path = '' as $$
    select u.matricola, p.rank, p.pu, u.type, u.components, u.lore,
           u.hp, u.supplies, u.uses, u.pos_x, u.pos_y, u.transported, u.status,
           case p.rank when 'Caporale' then u.matricola when 'Soldato' then l.matricola end,
           case p.rank when 'Caporale' then u.squad_name when 'Soldato' then l.squad_name end,
           case p.rank when 'Capitano' then u.matricola when 'Caporale' then l.matricola when 'Soldato' then c.matricola end,
           case p.rank when 'Capitano' then u.squad_name when 'Caporale' then l.squad_name when 'Soldato' then c.squad_name end,
           m.matricola
    from public.game_units u
    join public.players p on p.id = u.owner_id
    left join public.rank_salaries r on r.rank = p.rank
    left join public.game_units l on l.id = u.squad_of
    left join public.game_units c on c.id = l.squad_of
    left join public.game_units m on m.id = u.carried_by
    where u.status in ('pending', 'active')
    order by r.position desc nulls last, u.matricola;
$$;

revoke all on function public.unit_stats(text, text[]) from public, anon, authenticated;
revoke all on function public.free_seats(uuid, text) from public, anon, authenticated;
revoke all on function public.cell_free(integer, integer, uuid) from public, anon, authenticated;
revoke all on function public.path_length(integer, integer, integer, integer, integer, uuid) from public, anon, authenticated;
revoke all on function public.line_blocked(integer, integer, integer, integer) from public, anon, authenticated;
revoke all on function public.fire(public.game_components, integer, integer) from public, anon, authenticated;
revoke all on function public.execute_orders(uuid, integer, jsonb) from public, anon, authenticated;
revoke all on function public.execute_turn_orders() from public, anon, authenticated;
revoke all on function public.follow_vehicle() from public, anon, authenticated;
revoke all on function public.free_passengers() from public, anon, authenticated;
revoke all on function public.board_vehicle(text) from public, anon;
revoke all on function public.leave_vehicle(integer, integer) from public, anon;
revoke all on function public.give_supplies(text, integer) from public, anon;
revoke all on function public.get_team() from public;

grant execute on function public.board_vehicle(text) to authenticated;
grant execute on function public.leave_vehicle(integer, integer) to authenticated;
grant execute on function public.give_supplies(text, integer) to authenticated;
grant execute on function public.get_team() to anon, authenticated;

commit;
