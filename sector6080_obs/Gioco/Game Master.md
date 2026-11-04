# Game Master

Sector 6080 è **PvE**: i player collaborano e i nemici sono gestiti dai Game Master (GM).

## Cosa fanno solo i GM
- Approvare le [[Unità]] create dai player.
- Modificare le unità: HP, rifornimenti, posizione, componenti, distruzione.
- Rivedere e applicare gli [[Ordini]].
- Gestire i nemici e far avanzare il turno.
- Moderare la chat (da definire).

I player possono solo creare la propria unità, inviare ordini e usare la chat.
 GM coincidono con il comando supremo
## Sezione GM
- I GM avranno una **sezione a parte**.
- **Per ora si sviluppa solo l'interfaccia del player**: la sezione GM non esiste ancora.
- Nel frattempo i GM lavorano dalla **dashboard di Supabase**, nel Table Editor:
  - **Approvare un'unità:** in `game_units`, controllare statistiche e costo, poi impostare `status` su `active`.
  - **Rifiutarla:** impostare `status` su `rejected`. Oggi i PU non vengono restituiti da soli.
  - **Unità persa:** impostare `status` su `lost`.
  - **Modificare un'unità:** cambiare direttamente `hp`, `supplies`, `pos_x`, `pos_y`.
  - **Nuovo turno:** in `game_turn`, aumentare `turn` e impostare `started_at`.

## Da decidere
- Sezione dentro la stessa app (visibile solo con ruolo GM) o pannello separato.

Vedi [[Multiplayer]].
