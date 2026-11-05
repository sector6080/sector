# Problemi noti e sicurezza

Problemi trovati leggendo il codice del sito, ora tolto dal repository. Valgono come lezioni per l'[[App Android]].

## Sicurezza (critici)
- **Password in chiaro**: salvate così nel foglio (colonna C) e **inviate a tutti i client** con la GET delle unità. La verifica avviene nel browser (`verifyPassword`), quindi chiunque apra la console del browser può vedere tutte le password.
- In caso di password errata, il messaggio d'errore **stampa la password corretta** (`'❌ Password errata! ' + unit.password + password`).
- **API key Google** scritta in vecchie versioni di `map.html` e del `README.md`, su un repo pubblico. I file attuali non la contengono, ma resta nella storia di git: va limitata (referrer/API) o rigenerata.
- **XSS**: nick, componenti ecc. vengono inseriti con `innerHTML` senza escape.
- Nessun anti-spam o rate limit: chiunque può inviare il Google Form direttamente.
- Lista nella [[To Do]]: XSS, CSRF, brute force, session hijacking, data injection.

## Bug funzionali
- I dettagli degli ordini (destinazione, componente, quantità) non vengono letti dal form, vedi [[Ordini]].
- Le coordinate a lettere non coprono 80 colonne, vedi [[Mappa]].
- Il parsing basato sulle virgole si rompe se un testo contiene una virgola.
- Possibile conflitto in colonna R, vedi [[Schema Google Sheet]].
- Gestori delle tab registrati due volte in `test.html`.
- `sendData` riceve un 4° parametro che la funzione non usa.

## Lezioni per l'app
- Un messaggio "connessione non riuscita" può nascondere un errore del server: i log di Supabase dicono quale.
- Autenticazione vera (Supabase Auth o simile), password mai inviate al client.
- Validazione degli ordini **lato server**.
- Database strutturato (es. Firestore) al posto del parsing di testo da Google Form.
