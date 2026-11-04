-- Firma dei messaggi in chat: grado(matricola)-tipo, per esempio Soldato(ASD-543)-Fanteria.
-- Da eseguire una volta sola e per intero, dopo chat-gerarchia-gm.sql.

begin;

create or replace function public.send_chat(p_channel text, p_body text) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_rank text;
    v_muted boolean;
    v_unit uuid;
    v_author text;
    v_squad uuid;
begin
    select rank, muted into v_rank, v_muted from public.players where id = auth.uid();
    if v_muted then raise exception 'Sei stato silenziato'; end if;

    select u.id, v_rank || '(' || u.matricola || ')-' || u.type into v_unit, v_author
        from public.game_units u
        where u.owner_id = auth.uid() and u.status in ('pending', 'active');
    if v_gm then v_author := 'Comando Supremo'; end if;
    if v_author is null then raise exception 'Serve una unita per scrivere'; end if;

    if p_channel = 'eventi' and not v_gm then raise exception 'Canale non disponibile'; end if;
    if p_channel = 'comando' and not v_gm and v_rank not in ('Capitano', 'Caporale') then
        raise exception 'Canale non disponibile';
    end if;
    if p_channel = 'squadrone' then
        v_squad := public.my_squad();
        if v_squad is null then raise exception 'Canale non disponibile'; end if;
    end if;

    if exists (
        select 1 from public.chat_messages
        where author = v_author and sent_at > now() - interval '2 seconds'
    ) then
        raise exception 'Messaggi troppo ravvicinati';
    end if;

    insert into public.chat_messages (unit_id, channel, squad_id, author, body)
        values (v_unit, p_channel, v_squad, v_author, trim(p_body));
end;
$$;

-- Messaggi già inviati: cambia solo il formato, il grado resta quello dell'invio
update public.chat_messages
    set author = regexp_replace(author, '^(.+) - ([A-Z]{3}-[0-9]{3}) - (.+)$', '\1(\2)-\3')
    where author ~ '^.+ - [A-Z]{3}-[0-9]{3} - .+$';

commit;
