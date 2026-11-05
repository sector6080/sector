-- I rifornimenti si cedono anche in diagonale: conta la distanza contando le diagonali come un passo.
-- Da eseguire dopo squadra-senza-unita.sql. Cambia solo il corpo della funzione, non i permessi.

begin;

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
    if greatest(abs(v_to.pos_x - v_me.pos_x), abs(v_to.pos_y - v_me.pos_y)) > v_reach then
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

commit;
