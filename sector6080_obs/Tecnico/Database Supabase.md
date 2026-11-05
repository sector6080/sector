# Database Supabase

Controllo di sicurezza del 03/11/2026 sul progetto "Sector" (piano Free). Fatto in sola lettura: regole dalla dashboard, codice di `index.html` e conteggi con la chiave pubblica. Nessun dato o password è stato letto e nulla è stato modificato.

## Tabelle dell'app
Definite in `supabase/schema-app.sql`. Le vecchie tabelle del sito non vengono toccate.

| Tabella | Contenuto | Lettura | Scrittura del player |
|---|---|---|---|
| `players` | PU e grado, creato in automatico con l'account. Il grado torna `Recluta` quando l'unità viene persa | solo il proprio | nessuna |
| `game_units` | unità, proprietario, stato | solo la propria | con `create_unit` |
| `game_turn` | turno corrente, inizio e durata | tutti | nessuna |
| `game_orders` | ordini per unità e turno | solo i propri | con `send_orders` e `withdraw_orders` |
| `chat_messages` | messaggi per canale, firmati dal server: `grado(matricola)-tipo` | con `get_chat`, secondo canale e grado | con `send_chat` |
| `rank_proposals`, `rank_votes` | cambi di grado in votazione | con `get_proposals`, Capitani, GM e chi ha creato la votazione | con `propose_rank`, `vote_rank` e `close_proposal` |
| `game_enemies` | nemici | con `get_enemies`, solo dentro la visione comune | solo GM |
| `enemy_templates` | modelli dei nemici | con `gm_enemy_templates`, solo GM | dal Table Editor |
| `notifications` | notifiche di ogni player, cancellate dopo 30 giorni | con `get_notifications`, solo le proprie | le crea il server |
| `squad_requests` | richieste per entrare in un gruppo o in uno squadrone | con `get_squad_requests`, chi le ha fatte o ricevute | con `request_squad` e `answer_squad_request` |
| `game_map` | una riga per casella: terreno e struttura | tutti | nessuna |
| `game_sites` | descrizione delle strutture | tutti | nessuna |
| `game_components` | i 138 componenti, con costi e statistiche | tutti | nessuna |
| `unit_types` | tipi di unità e statistiche base | tutti | nessuna |
| `rank_salaries` | stipendio per grado | tutti | nessuna |
| `game_settings` | PU iniziali, bonus Google e posti negli squadroni | tutti | nessuna |

Le ultime cinque tabelle si modificano dal Table Editor e valgono subito, senza aggiornare l'app. Sono in `supabase/gm-turni-catalogo.sql` (eseguito il 04/11/2026: tabelle, 138 componenti, 4800 caselle e turno di 24 ore verificati), insieme a:
- ruolo GM dato dal grado `Comando Supremo` in `players.rank`, modificabile solo dalla console. Il grado toglie l'unità e impedisce di crearne altre;
- funzioni `gm_*`, che controllano il ruolo sul server;
- `create_unit` e `buy_component`, che calcolano statistiche e costi sul server;
- cambio turno con `pg_cron`, ogni minuto.

La mappa e la squadra si aggiungono con `supabase/mappa-e-squadra.sql` (eseguito: 4720 caselle e 9 strutture). L'app legge tutto con due funzioni, disponibili anche senza account:
- `get_map`: mappa compatta e strutture.
- `get_team`: unità approvate con grado, PU, statistiche, componenti, lore e posizione. Il proprietario resta privato. I PU pubblici sono stati aggiunti con `supabase/pu-pubblici.sql` (eseguito). Provato dal vivo con 4 unità: mappa dal server, visione comune, schede e PU.

`supabase/matricola-e-gradi.sql` (eseguito il 04/11/2026, firma e squadra verificate): elimina la colonna `player_name`, aggiunge il trigger che toglie il grado a chi perde l'unità, cambia la firma della chat e fa restituire a `get_team` anche le unità in attesa, ordinate per grado. L'app mostra sulla mappa solo quelle approvate. Dopo lo script le vecchie versioni dell'app non funzionano più.

`supabase/chat-gerarchia-gm.sql` (eseguito il 04/11/2026): aggiunge canali della chat, moderazione, squadroni (`game_units.squad_of`), votazioni, nemici e le funzioni GM per grado, posizione e componenti. La chat non si legge più dalla tabella, ma solo con `get_chat`, così i canali privati restano privati.

Controllo del 04/11/2026 con la chiave pubblica, senza account: `get_chat`, `get_team` e `get_enemies` rispondono. Tutte le funzioni di voto, squadrone e GM e `send_chat` sono rifiutate, come la lettura diretta di `chat_messages`, `rank_proposals`, `rank_votes` e `game_enemies`. Non è stato provato con account normali: chi ha un account passa dai controlli interni a ogni funzione.

`supabase/nome-squadrone.sql` (eseguito il 04/11/2026): aggiunge `game_units.squad_name` e la funzione `set_squad_name`. Il nome sta sull'unità del Caporale e il database accetta solo testo senza numeri, di massimo 8 caratteri.

Il 04/11/2026 è stato collegato il connettore Supabase: il cambio turno `sector-cambio-turno` risulta attivo e riuscito ogni minuto, e tutte le tabelle hanno RLS attivo.

`supabase/gerarchia-squadroni.sql` (eseguito il 04/11/2026 e provato con account simulati in una transazione annullata: permessi, canali, richieste e limiti rispondono come previsto): cambia la gerarchia. `game_units.squad_of` punta a chi sta sopra (Soldato, Caporale, Capitano), la chat ha il nuovo canale `gruppo` e `get_team` restituisce gruppo e squadrone di ogni unità.

`supabase/richieste-squadrone.sql` (eseguito il 04/11/2026): aggiunge le richieste per entrare in uno squadrone.

`supabase/gruppi-capitani.sql` (eseguito il 04/11/2026): permette ai Capitani di guidare uno squadrone e mette i limiti di 6 e 40 Soldati in `game_settings`, alle voci `squad_limit_corporal` e `squad_limit_captain`. Il limite di 40 ora conta Caporali e Soldati dello squadrone.

`supabase/voti-e-chiusura.sql` (eseguito il 05/11/2026 e provato con account simulati, senza salvare nulla): `resolve_proposal` decide l'esito contando solo i Capitani attuali, `get_proposals` mostra lo stesso conteggio e dice chi può chiudere, `close_proposal` chiude una votazione senza esito, il trigger `on_captains_changed` rifà i conti quando cambia il numero dei Capitani.

`supabase/notifiche.sql` (eseguito il 05/11/2026 e provato con account simulati, senza salvare nulla): aggiunge le notifiche e le menzioni in chat. Cambia anche la creazione: `create_unit` non accetta più componenti e controlla solo i PU, `gm_review_unit` fa pagare il tipo all'approvazione, `buy_component` funziona solo con l'unità attiva. La colonna `game_units.cost` è stata eliminata, perché un'unità rifiutata non ha speso nulla.

`supabase/template-nemici.sql` (eseguito il 04/11/2026): aggiunge i template dei nemici.

`supabase/fix-cambio-turno.sql` (applicato il 04/11/2026): Supabase rifiuta gli `UPDATE` e i `DELETE` senza `where` quando arrivano dall'app, ma non dal cambio automatico. Ogni funzione chiamata dall'app deve quindi avere il `where`, anche sulle tabelle con una riga sola.

**Regola di fondo:** il player non scrive mai direttamente nelle tabelle. Ogni funzione ricava il player dall'account collegato, quindi nessuno può agire per conto di un altro, cambiare i propri PU o firmare messaggi con un'altra matricola.

Stati dell'unità: `pending` (in attesa), `active` (approvata), `lost` (persa), `rejected` (rifiutata). Il database impedisce più di una unità `pending` o `active` per player.

**Limite risolto dallo script `gm-turni-catalogo.sql`:** costo e statistiche iniziali non arrivano più dall'app.

### Prova reale del 04/11/2026
Eseguita con due account anonimi dall'app sull'emulatore. Funzionano: account e profilo automatico, unità sul server con PU scalati, chat, unità recuperata dopo un riavvio, approvazione, invio e ritiro degli ordini.

Tentativi fatti direttamente sul server con l'account del secondo player, tutti senza effetto:

| Tentativo | Risultato |
|---|---|
| Leggere unità e profili altrui | vede solo i propri |
| Spostare l'unità di un altro o la propria | 0 righe modificate |
| Aumentare i propri PU | 0 righe modificate |
| Approvare la propria unità | 0 righe modificate |
| Inserire ordini direttamente | rifiutato |
| Scrivere in chat firmando con un'altra matricola | rifiutato |
| Creare una seconda unità | rifiutato |
| Inviare ordini da un'unità in attesa | rifiutato |
| Usare un costo negativo | rifiutato |
| Riusare una matricola | rifiutato, PU non scalati |

Dati di prova rimasti: unità ASD-543 (approvata, con un ordine) e XYZ-100, due messaggi in chat e due account anonimi.

Altre regole: chat con massimo 500 caratteri e un messaggio ogni 2 secondi per unità. La visione comune si calcola nell'app sommando le unità di `get_team`: quando ci saranno i nemici dovrà calcolarla il server, per non inviarli ai telefoni.

## Vecchie tabelle del sito
`units`, `new_units`, `player`, `map` e `general` sono state eliminate il 04/11/2026 con `supabase/rimuovi-tabelle-sito.sql`, ora tolto dal repository insieme a `pulizia-sito.sql`. Verificato: nel progetto restano solo le 5 tabelle dell'app, con il numero di colonne previsto e RLS attivo. `game_turn` contiene la sua riga iniziale.

> [!success] Pulizia eseguita il 03/11/2026
> Copia salvata ed eseguito `supabase/pulizia-sito.sql`. Verificato dopo: 5 tabelle con RLS attivo e nessuna regola, quindi nessun accesso con la chiave pubblica. Colonne password eliminate, `verify_unit` eliminata. Le sezioni qui sotto descrivono il controllo fatto **prima** della pulizia.

## Tabelle e regole prima della pulizia
RLS è **attivo su tutte le tabelle**.

| Tabella | Colonne | Chi può leggere | Chi può scrivere |
|---|---|---|---|
| `general` | 4 | nessuno | nessuno |
| `map` | 3 | tutti | nessuno |
| `new_units` | 14 | nessuno | **tutti** (solo inserimento) |
| `player` | 3 | nessuno | nessuno |
| `units` | 15 | tutti (3 righe) | nessuno |
| `verify_unit` | 2 | nessuno | **tutti** (solo inserimento) |

"Nessuno" significa nessuno dall'esterno. Chi ha accesso alla dashboard vede e modifica tutto.

## Cosa va bene
- Con la chiave pubblica **le password non si leggono**: `player` e `new_units` hanno la colonna password ma sono chiuse in lettura.
- `units` è pubblica ma il filtro sulla colonna `password` viene rifiutato, quindi quella colonna non risulta esserci.
- Nessuno dall'esterno può modificare o cancellare unità.
- Security Advisor: **0 errori**.

## Problemi
1. **Inserimenti aperti:** chiunque abbia la chiave pubblica, cioè chiunque apra il sito, può aggiungere righe senza limiti a `new_units` e `verify_unit`. Sono i 2 avvisi "RLS Policy Always True".
2. **Password in chiaro:** il sito invia la password a `new_units` senza cifrarla, e nel suo codice non c'è alcun hash. Il confronto avviene nel browser. La pagina dei trigger non mostrava nulla, ma non è una conferma certa. Chi ha accesso alla dashboard potrebbe quindi vederle.
3. **Il sito attuale risulta rotto** (deduzione da codice e regole, non provato aprendo il sito):
   - La verifica password legge `player`, che è chiusa, e `units`, che non ha la colonna: fallisce sempre.
   - Gli ordini vanno nella tabella `orders`, che non esiste.
4. **Nessun proprietario:** non c'è un legame tra account e unità, che serve per l'app (vedi [[Account e identità]]).

Gli altri 12 avvisi riguardano la visibilità delle tabelle in GraphQL: non espongono dati, ma le tabelle inutilizzate andrebbero nascoste. I 2 suggerimenti non sono stati letti.

## Non controllato
- Il contenuto reale di `player` e `new_units`: le righe sono nascoste e le stime della dashboard non sono affidabili (per `units` indicano 0 righe, ma sono 3).
- Quali colonne sono esposte in `units`.
- Se le password salvate sono in chiaro: va guardato dalla dashboard da chi ne è responsabile.

## Decisione: sito abbandonato
Deciso il 03/11/2026: il sito viene abbandonato per l'app, quindi il database si può chiudere.

**Pulizia, script `supabase/pulizia-sito.sql` (eseguito):** ha chiuso letture e inserimenti pubblici, ed eliminato le colonne password e la tabella `verify_unit`.

La copia CSV è stata cancellata: i vecchi dati del sito non esistono più.

**Dopo, per l'app:** nuove tabelle con il proprietario delle unità, limite di una unità per player, ordini e chat. Vedi [[Multiplayer]], [[Account e identità]] e [[Questioni legali]].

Senza traffico il progetto tornerà in pausa dopo circa una settimana. Non è un problema finché l'app non lo usa, ma va ripristinato entro 1 anno.
