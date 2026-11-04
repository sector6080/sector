# Multiplayer

Piano per passare dal prototipo offline al gioco condiviso. Oggi ogni telefono ha la sua partita.

## Perché è più semplice di quanto sembra
Sector 6080 è **PvE, a turni lunghi e con [[Game Master]] umani**: i player non si affrontano e un turno dura giorni. Quindi non serve un server in tempo reale: basta un **database condiviso con permessi** e una notifica quando cambia il turno.

## Scelta: Supabase
Il sito usava già Supabase e il collegamento dell'app è stato avviato il 04/11/2026. Struttura reale in [[Database Supabase]].

Copre queste esigenze e c'è già un progetto attivo:
- **Auth**: account del player, senza username e password. Consigliato l'accesso con Google, vedi [[Account e identità]]. Un account = un player = massimo una [[Unità]].
- **Database Postgres con Row Level Security**: dati condivisi e permessi lato server.
- **Realtime**: chat aggiornata in tempo reale.
- **Firebase Cloud Messaging**: servirà comunque per le notifiche push su Android, anche con Supabase.

### Piano gratuito di Supabase (controllato il 03/11/2026)
| Voce | Limite gratuito |
|---|---|
| Prezzo | 0 $ al mese |
| Progetti attivi | 2 |
| Database | 500 MB |
| Utenti attivi al mese | 50.000 |
| File | 1 GB |
| Traffico in uscita | 5 GB |
| Connessioni Realtime | 200 contemporanee |
| Messaggi Realtime | 2 milioni al mese |
| Backup automatici | **non inclusi** |

- **Pausa:** dopo 1 settimana con poche richieste al database il progetto va in pausa. Supabase avvisa via email. Bastano in genere poche richieste al giorno per evitarlo.
- **Ripristino:** dalla dashboard, fino a **1 anno** dopo la pausa.
- **Piano Pro:** da 25 $ al mese, senza pause.
- Per i limiti superati e l'uso commerciale nel piano gratuito non ho trovato una risposta chiara nella documentazione.

> [!warning] Progetto del sito
> Era fermo e il suo indirizzo non esisteva più. È stato riaperto il 03/11/2026 e il servizio di autenticazione risponde. Senza richieste regolari al database tornerà in pausa dopo circa una settimana.

Attenzioni:
- Una partita attiva evita la pausa, una partita ferma no.
- Senza backup automatici, i dati vanno salvati da noi.
- L'app usa il client Kotlin della community `supabase-kt` 3.9.0, che è quello indicato dalla documentazione di Supabase.
- La chat si aggiorna ogni 10 secondi. Realtime resta un miglioramento possibile.
- La chiave del sito è pubblica, quindi la sicurezza dipende tutta dalle regole RLS: risultati in [[Database Supabase]].

Nell'app basta aggiungere una nuova implementazione di `GameRepository`: la UI resta com'è.

## Struttura dei dati prevista
Quadro completo, compreso ciò che manca. Le tabelle già definite e i loro nomi reali sono in [[Database Supabase]].

| Tabella | Contenuto | Chi scrive | Chi legge |
|---|---|---|---|
| `players/{uid}` | nome, grado, PU, ruolo | GM (il player solo il nome) | il player stesso, GM |
| `game/current` | turno, scadenza | GM | tutti |
| `units/{id}` | proprietario, stato (in attesa, attiva, persa, rifiutata), statistiche, posizione | GM (il player solo la creazione) | proprietario, GM |
| `orders/{turno}_{unit}` | catena di ordini | il proprietario, fino alla scadenza | proprietario, GM |
| `enemies/{id}` | nemici sulla mappa | GM | **solo GM** |
| `vision/{turno}` | ciò che vede la squadra, visione unica per tutti | GM | tutti i player |
| `chat/{id}` | messaggi firmati dall'unità | chi ha un'unità attiva o in attesa | tutti |

## Le due regole chiave
1. **I permessi stanno sul server.** Le regole del database garantiscono che solo i GM approvino e modifichino le unità. Nascondere i pulsanti nell'app non basta.
2. **La nebbia di guerra non si fa sul telefono.** Se i nemici arrivassero al client, basterebbe ispezionare l'app per vederli. La visione è **condivisa da tutta la squadra**: la sezione GM unisce ciò che vedono tutte le unità e scrive solo quello in `vision`.

## Un turno
1. Il GM apre il turno (`game/current`).
2. I player scelgono gli ordini dalla mappa e li inviano.
3. Allo scadere gli ordini si bloccano.
4. Il GM li rivede: la sezione GM riusa `OrderRules`, le stesse regole Kotlin del player, quindi un ordine truccato viene scoperto.
5. Il GM applica i risultati, muove i nemici e pubblica la visione comune.
6. Parte il turno dopo, con una notifica.

## Ordine di lavoro consigliato
1. ✅ Backend scelto, accesso anonimo, unità online. Provato dal vivo.
2. ✅ Sezione GM, turni automatici e stipendi: script eseguito il 04/11/2026, prova dal vivo in corso.
3. ✅ Invio ordini e turno condiviso, stesso stato del punto 1.
4. ✅ Chat reale, stesso stato del punto 1.
5. ✅ Mappa su server, alleati visibili con scheda e PU, visione comune: provato dal vivo. Restano i nemici e la loro nebbia di guerra lato GM.
6. Accesso Google reale e bonus PU dal server.
7. Notifiche push.

## Obblighi per il Play Store
Vedi [[Questioni legali]]. I costi del backend vanno ricontrollati prima della pubblicazione.

Vedi: [[Game Master]] · [[App Android]] · [[Account e identità]] · [[Problemi noti e sicurezza]]
