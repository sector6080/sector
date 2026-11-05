-- Componenti passivi: bonus alle armi, cure a bordo dei mezzi e Postazione Comando Mobile.
-- I valori stanno in game_components, quindi si cambiano dal Table Editor senza aggiornare l'app.
-- Da eseguire dopo ordini-automatici.sql.

begin;

-- firearm: l'arma è un'arma da fuoco; for_firearms: il bonus vale solo per quelle
-- perf_from e perf_to: distanza del bersaglio per cui vale il bonus alla perforazione
-- regen: HP che il mezzo ridà a ogni turno a chi è a bordo, carries limita il tipo di unità
alter table public.game_components
    add column firearm boolean not null default false,
    add column for_firearms boolean not null default false,
    add column bonus_perf integer not null default 0,
    add column bonus_damage integer not null default 0,
    add column bonus_range integer not null default 0,
    add column perf_from integer,
    add column perf_to integer,
    add column regen integer not null default 0;

update public.game_components set firearm = true
    where type = 'Fanteria' and name in (
        'Pistola', 'Pistola Silenziata', 'SMG', 'Mitra Avanzato', 'Mitragliatrice Leggera',
        'Fucile d''Assalto', 'Fucile a Pompa', 'Fucile da Tiro', 'Fucile di Precisione',
        'Fucile Anticarro', 'Fucile ad Antimateria', 'Fucile a Reticolo',
        'Kit da Cecchino', 'Kit da Cecchino Elite'
    );

update public.game_components c set
    for_firearms = v.firearms, bonus_perf = v.perf, bonus_damage = v.damage, bonus_range = v.reach,
    perf_from = v.near, perf_to = v.far
from (values
    ('Fanteria', 'Proiettili Perforanti', true, 1, 0, 0, null::integer, null::integer),
    ('Fanteria', 'Proiettili Esplosivi', true, 0, 1, 0, null, null),
    ('Fanteria', 'Mirino Laser', true, 2, 0, 0, null, 3),
    ('Fanteria', 'Mirino ACOG', true, 2, 0, 0, 4, null),
    ('Fanteria', 'Mirino Digitale', true, 1, 0, 1, 4, null),
    ('Fanteria', 'Ottica a Doppio Zoom', true, -1, 0, 2, null, null),
    ('Fanteria', 'Sistema di Mira Laser', true, 1, 0, 1, null, null),
    ('Corazzata', 'Sistema di Guida', false, 1, 0, 0, null, null),
    ('Artiglieria', 'Piattaforma Stabilizzata', false, 1, 1, 0, null, null),
    ('Artiglieria', 'Sistema di Puntamento Laser', false, 1, 0, 1, null, null)
) as v(type, name, firearms, perf, damage, reach, near, far)
where c.type = v.type and c.name = v.name;

update public.game_components set regen = 1 where name = 'Cucina da Campo';
update public.game_components set regen = 2 where name = 'Ospedale da Campo';
update public.game_components set regen = 1, carries = 'Fanteria' where name = 'Kit da Supporto';

-- Unità diverse rifornite in un turno, per la Postazione Comando Mobile
create table public.supply_log (
    turn integer not null,
    giver uuid not null references public.game_units (id) on delete cascade,
    receiver uuid not null references public.game_units (id) on delete cascade,
    primary key (turn, giver, receiver)
);
alter table public.supply_log enable row level security;
revoke all on public.supply_log from anon, authenticated;

create function public.weapon_bonus(p_unit public.game_units, p_weapon public.game_components, p_distance integer)
returns table (perf integer, damage integer, reach integer)
language sql stable security definer set search_path = '' as $$
    select coalesce(sum(b.bonus_perf) filter (
               where p_distance >= coalesce(b.perf_from, p_distance)
               and p_distance <= coalesce(b.perf_to, p_distance)
           ), 0)::integer,
           coalesce(sum(b.bonus_damage), 0)::integer,
           coalesce(sum(b.bonus_range), 0)::integer
    from public.game_components b
    where b.type = p_unit.type and b.name = any(p_unit.components)
    and (not b.for_firearms or p_weapon.firearm);
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
            v_distance := abs(v_x - v_unit.pos_x) + abs(v_y - v_unit.pos_y);
            select * into v_bonus from public.weapon_bonus(v_unit, v_item, coalesce(v_distance, 0));
            v_reach := v_item.range + case when v_armed then v_bonus.reach else 0 end;
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
            elsif v_armed and v_x is not null
                and public.line_blocked(v_unit.pos_x, v_unit.pos_y, v_x, v_y) then
                v_why := 'un''altra unità blocca il tiro';
            else
                v_used := array_append(v_used, v_name);
                v_still := v_still or v_item.stationary;
                v_unit.uses := v_unit.uses || jsonb_build_object(v_name, v_uses - 1);

                if v_armed and not v_item.manual and v_x is not null then
                    v_item.perforation := greatest(v_item.perforation + v_bonus.perf, 0);
                    v_item.damage := v_item.damage + v_bonus.damage;
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

-- Cucina da Campo, Ospedale da Campo e Kit da Supporto, a ogni cambio turno
create function public.heal_passengers() returns void
language sql security definer set search_path = '' as $$
    update public.game_units u
    set hp = least(u.hp + h.amount, s.hp)
    from (
        select p.id, sum(c.regen)::integer as amount
        from public.game_units p
        join public.game_units v on v.id = p.carried_by
        join public.game_components c on c.type = v.type and c.name = any(v.components)
        where p.status = 'active' and v.status = 'active'
        and c.regen > 0 and (c.carries is null or c.carries = p.type)
        group by p.id
    ) as h
    cross join lateral public.unit_stats(
        (select b.type from public.game_units b where b.id = h.id),
        (select b.components from public.game_units b where b.id = h.id)
    ) as s
    where u.id = h.id and u.hp > 0 and u.hp < s.hp;
$$;

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

    delete from public.supply_log where turn < v_turn;
    delete from public.notifications where created_at < now() - interval '30 days';
end;
$$;

-- Postazione Comando Mobile: +1 PU ogni 2 unità diverse rifornite nello stesso turno
create or replace function public.give_supplies(p_matricola text, p_quantity integer) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_me public.game_units;
    v_to public.game_units;
    v_reach integer;
    v_max integer;
    v_turn integer;
    v_served integer;
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

    if 'Postazione Comando Mobile' = any(v_me.components) then
        select turn into v_turn from public.game_turn;
        insert into public.supply_log (turn, giver, receiver) values (v_turn, v_me.id, v_to.id)
            on conflict do nothing;
        if found then
            select count(*) into v_served from public.supply_log where turn = v_turn and giver = v_me.id;
            if v_served % 2 = 0 then
                update public.players set pu = pu + 1 where id = v_me.owner_id;
                perform public.notify(
                    v_me.owner_id, 'ordini', 'Postazione Comando Mobile',
                    'Hai rifornito ' || v_served || ' unità in questo turno: +1 PU.'
                );
            end if;
        end if;
    end if;
end;
$$;

revoke all on function public.weapon_bonus(public.game_units, public.game_components, integer) from public, anon, authenticated;
revoke all on function public.heal_passengers() from public, anon, authenticated;

commit;
