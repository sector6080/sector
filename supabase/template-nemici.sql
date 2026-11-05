-- Template dei nemici: il GM li sceglie quando fa comparire un nemico sulla mappa.
-- Si aggiungono e si modificano dal Table Editor, senza aggiornare l'app.
-- I valori iniziali sono una proposta, da bilanciare.
-- Da eseguire una volta sola e per intero, dopo fix-cambio-turno.sql.

begin;

create table public.enemy_templates (
    name text primary key check (char_length(name) between 1 and 40),
    hp integer not null check (hp > 0),
    armor integer not null default 0 check (armor >= 0),
    description text not null default '' check (char_length(description) <= 500),
    sort_order integer not null default 0
);

-- I player non devono conoscere i nemici prima di incontrarli
alter table public.enemy_templates enable row level security;
revoke all on public.enemy_templates from anon, authenticated;

insert into public.enemy_templates (name, hp, armor, description, sort_order) values
    ('Soldato mutato', 6, 0, 'Ex soldato trasformato dall''Entità. Debole da solo, pericoloso in gruppo.', 1),
    ('Bruto mutato', 12, 1, 'Mutazione avanzata: lento, ma regge molti colpi.', 2),
    ('Creatura biomeccanica', 9, 1, 'Carne e metallo fusi insieme. Veloce e imprevedibile.', 3),
    ('Predatore biomeccanico', 14, 2, 'Caccia le unità isolate. Le armi leggere non lo fermano.', 4),
    ('Drone senziente', 5, 1, 'Macchina da ricognizione: osserva e segnala la Task Force.', 5),
    ('Macchina da guerra', 20, 4, 'Macchina senziente pesante che si autoripara. Serve artiglieria.', 6);

create function public.gm_enemy_templates() returns table (
    name text, hp integer, armor integer, description text
)
language plpgsql stable security definer set search_path = '' as $$
begin
    if not public.is_gm() then raise exception 'Solo Game Master'; end if;
    return query
        select t.name, t.hp, t.armor, t.description
        from public.enemy_templates t
        order by t.sort_order, t.name;
end;
$$;

revoke all on function public.gm_enemy_templates() from public, anon;
grant execute on function public.gm_enemy_templates() to authenticated;

commit;
