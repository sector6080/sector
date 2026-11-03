# Multiplayer

Piano per passare dal prototipo offline al gioco condiviso. Oggi ogni telefono ha la sua partita.

## Perché è più semplice di quanto sembra
Sector 6080 è **PvE, a turni lunghi e con [[Game Master]] umani**: i player non si affrontano e un turno dura giorni. Quindi non serve un server in tempo reale: basta un **database condiviso con permessi** e una notifica quando cambia il turno.

## Scelta consigliata: Firebase
È già l'idea nella [[To Do]] e copre tutto ciò che serve:
- **Firebase Auth**: account del player (email o Google). Un account = un player = una [[Unità]].
- **Firestore**: database condiviso con regole di sicurezza. Può aggiornare la chat in tempo reale.
- **Cloud Messaging (FCM)**: notifiche di nuovo turno, unità approvata, messaggi.
- **Cloud Functions**: solo quando serviranno automatismi (es. invio notifiche). Richiedono il piano a consumo Blaze, quindi vanno rimandate il più possibile.

Nell'app basta aggiungere una nuova implementazione di `GameRepository`: la UI resta com'è.

## Struttura dei dati
| Collezione | Contenuto | Chi scrive | Chi legge |
|---|---|---|---|
| `players/{uid}` | nome, grado, PU, ruolo | GM (il player solo il nome) | il player stesso, GM |
| `game/current` | turno, scadenza | GM | tutti |
| `units/{id}` | statistiche, posizione, approvazione | GM (il player solo la creazione) | proprietario, GM |
| `orders/{turno}_{unit}` | catena di ordini | il proprietario, fino alla scadenza | proprietario, GM |
| `enemies/{id}` | nemici sulla mappa | GM | **solo GM** |
| `vision/{unit}/turns/{n}` | ciò che l'unità vede | GM | proprietario |
| `chat/{id}` | messaggi | tutti gli autenticati | tutti gli autenticati |

## Le due regole chiave
1. **I permessi stanno sul server.** Le regole di sicurezza di Firestore garantiscono che solo i GM approvino e modifichino le unità. Nascondere i pulsanti nell'app non basta.
2. **La nebbia di guerra non si fa sul telefono.** Se i nemici arrivassero al client, basterebbe ispezionare l'app per vederli. La sezione GM calcola cosa vede ogni unità e scrive solo quello in `vision`.

## Un turno
1. Il GM apre il turno (`game/current`).
2. I player scelgono gli ordini dalla mappa e li inviano.
3. Allo scadere gli ordini si bloccano.
4. Il GM li rivede: la sezione GM riusa `OrderRules`, le stesse regole Kotlin del player, quindi un ordine truccato viene scoperto.
5. Il GM applica i risultati, muove i nemici e pubblica la visione di ogni unità.
6. Parte il turno dopo, con una notifica.

## Ordine di lavoro consigliato
1. Progetto Firebase, login, salvataggio dell'unità online.
2. Approvazione: **sezione GM minima** (senza, nessuna unità può entrare in gioco).
3. Invio ordini reale e turno condiviso.
4. Chat reale.
5. Mappa condivisa: alleati visibili, nemici, nebbia di guerra lato GM.
6. Notifiche push.

## Obblighi per il Play Store
- **Chat**: contenuti generati dagli utenti, quindi servono segnalazione, blocco e moderazione (i GM sono i candidati naturali).
- **Account**: serve una funzione per eliminare l'account dall'app.
- **Privacy policy**: obbligatoria, perché si raccolgono account e messaggi.

Da ricontrollare al momento della pubblicazione, insieme ai costi di Firebase: sono regole e listini che cambiano.

Vedi: [[Game Master]] · [[App Android]] · [[Problemi noti e sicurezza]]
