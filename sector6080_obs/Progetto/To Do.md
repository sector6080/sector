# To Do

## App (prossimi passi)
- [x] Aggiungere nell'app il link del server Discord (costante `DISCORD_URL` in `App.kt`)
- [ ] Aggiornare il Google Doc: budget di creazione 5 PU invece di 10, "Player" al posto del nick Discord, Discord facoltativo
- [ ] Definire le coordinate dei tre porti e dei depositi
- [ ] Rendere il regolamento leggibile da tutti ("Chiunque abbia il link")
- [ ] Decidere la durata del turno: 2 giorni o 24 ore
- [ ] Iniziare il [[Multiplayer]]: login Firebase e sezione [[Game Master]] minima

## Vecchia lista del sito
Copiata dal `README.md` del repo (aggiornata dopo la riunione del comando supremo del 13/12/2025). Le voci già completate sono barrate.

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
