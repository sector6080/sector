# To Do

## App (prossimi passi)
- [x] Aggiungere nell'app il link del server Discord (costante `DISCORD_URL` in `App.kt`)
- [x] Riaprire il progetto Supabase del sito (03/11/2026)
- [x] Salvare una copia del database (il piano gratuito non ha backup automatici)
- [ ] Aggiornare il Google Doc: budget di creazione 4 PU (+1 con Google) invece di 10, "Player" al posto del nick Discord, Discord facoltativo
- [ ] Completare la lista in [[Questioni legali]]
- [x] Definire le coordinate dei tre porti e dei depositi (dalla [[Mappa]])
- [ ] Confermare i dubbi sulla [[Mappa]]: colori, riga mancante, magazzini ed edifici
- [ ] Rendere il regolamento leggibile da tutti ("Chiunque abbia il link")
- [ ] Decidere la durata del turno: 2 giorni o 24 ore
- [x] Decidere il backend: Supabase
- [x] Eseguire `supabase/schema-app.sql`
- [x] Eliminare le vecchie tabelle del sito
- [x] Attivare gli accessi anonimi nella dashboard
- [x] Provare il collegamento dell'app: unità, ordini, chat, approvazione, secondo player
- [ ] Togliere dal database i dati di prova prima di aprire ad altri
- [ ] Attivare un CAPTCHA o un altro limite per gli accessi anonimi
- [ ] Restituzione dei PU per le unità rifiutate
- [x] Controllare le regole di sicurezza RLS di Supabase (vedi [[Database Supabase]])
- [x] Decidere il futuro del sito: abbandonato per l'app
- [x] Esportare i dati ed eseguire `supabase/pulizia-sito.sql` (vedi [[Database Supabase]])
- [ ] Cancellare la copia CSV quando non serve più: può contenere password
- [ ] Spegnere GitHub Pages, così nessuno usa più il sito rotto
- [x] Decidere il metodo di accesso: Google + anonimo
- [ ] Sciogliere i dubbi rimasti in [[Account e identità]]
- [x] Prototipo: chat solo in lettura senza unità, messaggi firmati dall'unità
- [x] Spostare PU e grado dall'unità al player
- [ ] Accesso Google reale e bonus PU dal server
- [ ] Portare la visione condivisa sul server
- [ ] Iniziare il [[Multiplayer]]: login e sezione [[Game Master]] minima
- [ ] Aggiornare `origin` di git al nuovo indirizzo `github.com/sector6080/sector`

## Vecchia lista del sito
Copiata dal `README.md` di dicembre 2025 (aggiornata dopo la riunione del comando supremo del 13/12/2025). Il README attuale ne ha una versione più recente. Le voci già completate sono barrate.

## Essenziale
- [ ] Tradurre tutto in Angular → da rivalutare per l'[[App Android]]
- [ ] Cambiare lettere in numeri per le coordinate ([[Mappa]])
- [ ] Fare il server su Firebase
- [ ] Aggiungere un boolean nelle unità (es. *trasportata*)

## Alta priorità
- [ ] Fare la mappa (mappa centrale + tendina a sinistra con le unità)
- [x] Bug: veniva inviata l'unità precedente invece di quella giusta
- [ ] Cleaning del codice (input.html / map.html / Apps Script), molte funzioni ridondanti
- [ ] Staccare i codici .gs dal .html
- [x] Mancavano dei campi html → tabella
- [x] html non leggeva più le unità nella riga 2
- [x] Risposte del modulo 1 non organizzate in Foglio1
- [x] Trigger sul server alla modifica del primo foglio
- [x] Bug delle virgole

## Media priorità
- [ ] Timer del turno nella mappa e nel main (fusi orari)
- [ ] Link a Discord, Google Doc (regole), mappa, main, YouTube
- [ ] Interfaccia utente migliore
- [ ] Sicurezza: anti-spam, accesso all'API, password, XSS, CSRF, brute force, session hijacking, data injection → vedi [[Problemi noti e sicurezza]]
- [ ] Rifare invia ordini (poco sicuro e brutto)
- [x] Spostare `CheckOrderValidity()` in test.html
- [ ] Fixxare `CheckOrderValidity`

## Bassa priorità
- [ ] Check ✓ verde / evidenziazione rossa sui campi del form
- [ ] Sistema di notifiche nell'html
- [ ] Login/unità salvati in locale
- [ ] Ordini salire/scendere: indicare l'unità invece della posizione

## Next
- [ ] Dark mode
- [ ] Autosave della pagina
- [ ] Account GitHub
- [ ] Palette colori
- [ ] Nuova mail per Sector (Discord / TikTok / Instagram / YouTube)

## ???
- [ ] Cambio password
- [ ] Tutto in inglese (switchabile?)
- [ ] Notifiche fuori dall'app (nuovo turno / unità approvata)
- [ ] Barra di ricerca delle unità
- [ ] Skeleton UI durante il caricamento
- [ ] Apps Script che applica automaticamente gli ordini
