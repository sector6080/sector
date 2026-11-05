-- Votazioni: contano solo i Capitani di oggi, anche nel totale mostrato, e il conteggio si rifà
-- quando cambia il numero dei Capitani. Chi ha creato la votazione o il GM possono chiuderla.
-- Da eseguire dopo notifiche.sql.

begin;

create function public.resolve_proposal(p_id bigint) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_proposal public.rank_proposals;
    v_owner uuid;
    v_voters integer;
    v_quorum integer;
    v_yes integer;
    v_no integer;
begin
    select * into v_proposal from public.rank_proposals where id = p_id and status = 'open' for update;
    if v_proposal.id is null then return; end if;

    select owner_id into v_owner from public.game_units where id = v_proposal.unit_id;
    select count(*) into v_voters from public.players where rank = 'Capitano' and id <> v_owner;
    if v_voters = 0 then return; end if;
    v_quorum := v_voters / 2 + 1;

    select count(*) filter (where v.approve), count(*) filter (where not v.approve)
        into v_yes, v_no
        from public.rank_votes v
        join public.players p on p.id = v.voter_id
        where v.proposal_id = p_id and p.rank = 'Capitano' and p.id <> v_owner;

    if v_yes >= v_quorum then
        update public.rank_proposals set status = 'approved', closed_at = now() where id = p_id;
        update public.players set rank = v_proposal.new_rank where id = v_owner;
    elsif v_no > v_voters - v_quorum then
        update public.rank_proposals set status = 'rejected', closed_at = now() where id = p_id;
    end if;
end;
$$;

create or replace function public.vote_rank(p_id bigint, p_approve boolean) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_proposal public.rank_proposals;
begin
    if public.my_rank() is distinct from 'Capitano' then raise exception 'Solo i Capitani'; end if;

    select * into v_proposal from public.rank_proposals where id = p_id and status = 'open' for update;
    if v_proposal.id is null then raise exception 'Proposta chiusa'; end if;

    if (select owner_id from public.game_units where id = v_proposal.unit_id) = auth.uid() then
        raise exception 'Non puoi votare su te stesso';
    end if;

    insert into public.rank_votes (proposal_id, voter_id, approve) values (p_id, auth.uid(), p_approve)
        on conflict (proposal_id, voter_id) do update set approve = excluded.approve;

    perform public.resolve_proposal(p_id);
end;
$$;

-- Chi ha creato una votazione la vede anche se non è più Capitano, altrimenti non potrebbe chiuderla
drop function public.get_proposals();

create function public.get_proposals() returns table (
    id bigint, matricola text, from_rank text, new_rank text, proposed_by text,
    votes_yes integer, votes_no integer, my_vote boolean, can_close boolean
)
language plpgsql stable security definer set search_path = '' as $$
declare
    v_gm boolean := public.is_gm();
    v_all boolean := v_gm or public.my_rank() = 'Capitano';
begin
    return query
        select r.id, u.matricola, r.from_rank, r.new_rank, b.matricola,
               (select count(*)::integer from public.rank_votes v
                    join public.players p on p.id = v.voter_id
                    where v.proposal_id = r.id and v.approve and p.rank = 'Capitano' and p.id <> u.owner_id),
               (select count(*)::integer from public.rank_votes v
                    join public.players p on p.id = v.voter_id
                    where v.proposal_id = r.id and not v.approve and p.rank = 'Capitano' and p.id <> u.owner_id),
               (select v.approve from public.rank_votes v where v.proposal_id = r.id and v.voter_id = auth.uid()),
               v_gm or b.owner_id is not distinct from auth.uid()
        from public.rank_proposals r
        join public.game_units u on u.id = r.unit_id
        left join public.game_units b on b.id = r.proposed_by
        where r.status = 'open' and (v_all or b.owner_id = auth.uid())
        order by r.id;
end;
$$;

-- Se un Capitano arriva o se ne va, il numero di voti necessari cambia
create function public.recount_proposals() returns trigger
language plpgsql security definer set search_path = '' as $$
declare
    v_id bigint;
begin
    for v_id in select id from public.rank_proposals where status = 'open' order by id loop
        perform public.resolve_proposal(v_id);
    end loop;
    return null;
end;
$$;

create trigger on_captains_changed after update of rank on public.players
    for each row when ((old.rank = 'Capitano') is distinct from (new.rank = 'Capitano'))
    execute function public.recount_proposals();

create function public.close_proposal(p_id bigint) returns void
language plpgsql security definer set search_path = '' as $$
declare
    v_proposal public.rank_proposals;
begin
    select * into v_proposal from public.rank_proposals where id = p_id and status = 'open' for update;
    if v_proposal.id is null then raise exception 'Proposta chiusa'; end if;

    if not public.is_gm()
        and auth.uid() is distinct from (select owner_id from public.game_units where id = v_proposal.proposed_by) then
        raise exception 'Solo chi ha creato la votazione';
    end if;

    update public.rank_proposals set status = 'closed', closed_at = now() where id = p_id;
end;
$$;

-- Le votazioni già aperte seguono subito le regole nuove
select public.resolve_proposal(id) from public.rank_proposals where status = 'open';

revoke all on function public.resolve_proposal(bigint) from public, anon, authenticated;
revoke all on function public.recount_proposals() from public, anon, authenticated;
revoke all on function public.get_proposals() from public, anon;
revoke all on function public.close_proposal(bigint) from public, anon;

grant execute on function public.get_proposals() to authenticated;
grant execute on function public.close_proposal(bigint) to authenticated;

commit;
