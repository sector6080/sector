-- Tabelle dell'app Sector 6080, parte player.
-- Non tocca le vecchie tabelle del sito (units, new_units, player, map, general).
--
-- Regola di fondo: i player non scrivono mai direttamente nelle tabelle.
-- Ogni scrittura passa da una funzione che ricava il player dall'account,
-- quindi nessuno può agire per conto di un altro o cambiare le proprie statistiche.
-- I Game Master per ora lavorano dalla dashboard, che ignora RLS.

begin;

create table public.players (
    id uuid primary key references auth.users (id) on delete cascade,
    pu integer not null default 4 check (pu >= 0),
    rank text not null default 'Recluta',
    created_at timestamptz not null default now()
);

create table public.game_units (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references public.players (id) on delete cascade,
    matricola text not null unique check (matricola ~ '^[A-Z]{3}-[0-9]{3}$'),
    player_name text not null check (char_length(player_name) between 1 and 40),
    type text not null check (type in ('Fanteria', 'Motorizzata', 'Corazzata', 'Artiglieria')),
    components text[] not null default '{}',
    lore text not null check (char_length(lore) between 1 and 2000),
    hp integer not null check (hp >= 0),
    supplies integer not null check (supplies >= 0),
    uses jsonb not null default '{}',
    pos_x integer not null check (pos_x between 1 and 80),
    pos_y integer not null check (pos_y between 1 and 59),
    transported boolean not null default false,
    status text not null default 'pending' check (status in ('pending', 'active', 'lost', 'rejected')),
    created_at timestamptz not null default now()
);

-- Massimo una unità attiva o in attesa per player
create unique index one_live_unit_per_player on public.game_units (owner_id)
    where status in ('pending', 'active');

-- Una sola riga con il turno corrente
create table public.game_turn (
    id boolean primary key default true check (id),
    turn integer not null default 0,
    started_at timestamptz,
    hours integer not null default 48
);
insert into public.game_turn default values;

create table public.game_orders (
    unit_id uuid not null references public.game_units (id) on delete cascade,
    turn integer not null,
    orders jsonb not null,
    sent_at timestamptz not null default now(),
    primary key (unit_id, turn)
);

create table public.chat_messages (
    id bigint generated always as identity primary key,
    unit_id uuid references public.game_units (id) on delete set null,
    author text not null,
    body text not null check (char_length(body) between 1 and 500),
    sent_at timestamptz not null default now()
);

alter table public.players enable row level security;
alter table public.game_units enable row level security;
alter table public.game_turn enable row level security;
alter table public.game_orders enable row level security;
alter table public.chat_messages enable row level security;

create policy "Il player vede il proprio profilo" on public.players
    for select to authenticated using (id = auth.uid());

create policy "Il player vede la propria unita" on public.game_units
    for select to authenticated using (owner_id = auth.uid());

create policy "Tutti vedono il turno" on public.game_turn
    for select to anon, authenticated using (true);

create policy "Il player vede i propri ordini" on public.game_orders
    for select to authenticated using (
        exists (select 1 from public.game_units u where u.id = unit_id and u.owner_id = auth.uid())
    );

create policy "Tutti leggono la chat" on public.chat_messages
    for select to anon, authenticated using (true);

grant select on public.players, public.game_units, public.game_orders to authenticated;
grant select on public.game_turn, public.chat_messages to anon, authenticated;

-- Ogni nuovo account riceve il suo profilo con i PU iniziali
create function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    insert into public.players (id) values (new.id);
    return new;
end;
$$;

create trigger on_auth_user_created after insert on auth.users
    for each row execute function public.handle_new_user();

-- Crea l'unità in attesa e scala i PU in un'unica operazione.
-- Le statistiche inviate dall'app vengono controllate dai GM prima dell'approvazione.
create function public.create_unit(
    p_matricola text, p_player_name text, p_type text, p_components text[], p_lore text,
    p_cost integer, p_hp integer, p_supplies integer, p_uses jsonb, p_x integer, p_y integer
) returns void
language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null then raise exception 'Accesso richiesto'; end if;
    if p_cost < 0 then raise exception 'Costo non valido'; end if;

    update public.players set pu = pu - p_cost where id = auth.uid() and pu >= p_cost;
    if not found then raise exception 'PU insufficienti'; end if;

    insert into public.game_units (
        owner_id, matricola, player_name, type, components, lore, hp, supplies, uses, pos_x, pos_y
    ) values (
        auth.uid(), p_matricola, p_player_name, p_type, p_components, p_lore,
        p_hp, p_supplies, p_uses, p_x, p_y
    );
end;
$$;

-- Solo un'unità approvata può inviare ordini, e solo per il turno corrente
create function public.send_orders(p_orders jsonb) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
    v_turn integer;
begin
    select id into v_unit from public.game_units
        where owner_id = auth.uid() and status = 'active';
    if v_unit is null then raise exception 'Unita non attiva'; end if;

    select turn into v_turn from public.game_turn;
    insert into public.game_orders (unit_id, turn, orders) values (v_unit, v_turn, p_orders)
        on conflict (unit_id, turn) do update set orders = excluded.orders, sent_at = now();
end;
$$;

create function public.withdraw_orders() returns void
language plpgsql security definer set search_path = '' as $$
begin
    delete from public.game_orders o
        using public.game_units u, public.game_turn t
        where o.unit_id = u.id and o.turn = t.turn and u.owner_id = auth.uid();
end;
$$;

-- Il messaggio viene firmato dal server con la matricola dell'unità attiva o in attesa
create function public.send_chat(p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_unit uuid;
    v_matricola text;
begin
    select id, matricola into v_unit, v_matricola from public.game_units
        where owner_id = auth.uid() and status in ('pending', 'active');
    if v_unit is null then raise exception 'Serve una unita per scrivere'; end if;

    if exists (
        select 1 from public.chat_messages
        where unit_id = v_unit and sent_at > now() - interval '2 seconds'
    ) then
        raise exception 'Messaggi troppo ravvicinati';
    end if;

    insert into public.chat_messages (unit_id, author, body) values (v_unit, v_matricola, trim(p_body));
end;
$$;

revoke all on function public.handle_new_user() from public, anon, authenticated;
revoke all on function public.create_unit(text, text, text, text[], text, integer, integer, integer, jsonb, integer, integer) from public, anon;
revoke all on function public.send_orders(jsonb) from public, anon;
revoke all on function public.withdraw_orders() from public, anon;
revoke all on function public.send_chat(text) from public, anon;

grant execute on function public.create_unit(text, text, text, text[], text, integer, integer, integer, jsonb, integer, integer) to authenticated;
grant execute on function public.send_orders(jsonb) to authenticated;
grant execute on function public.withdraw_orders() to authenticated;
grant execute on function public.send_chat(text) to authenticated;

commit;
