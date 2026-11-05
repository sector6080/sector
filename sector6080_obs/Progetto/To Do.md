# To Do

## App (prossimi passi)
- [x] Aggiungere nell'app il link del server Discord (costante `DISCORD_URL` in `App.kt`)
- [x] Riaprire il progetto Supabase del sito (03/11/2026)
- [x] Salvare una copia del database (il piano gratuito non ha backup automatici)
- [ ] Aggiornare il Google Doc: elenco in [[modifiche doc google da fare]]
- [ ] Completare la lista in [[Questioni legali]]
- [x] Definire le coordinate dei tre porti e dei depositi (dalla [[Mappa]])
- [ ] Confermare i dubbi sulla [[Mappa]]: colori, effetto di magazzini e raffinerie
- [x] Rendere il regolamento leggibile da tutti ("Chiunque abbia il link")
- [x] Decidere la durata del turno: 24 ore
- [x] Decidere il backend: Supabase
- [x] Eseguire `supabase/schema-app.sql`
- [x] Eliminare le vecchie tabelle del sito
- [x] Attivare gli accessi anonimi nella dashboard
- [x] Provare il collegamento dell'app: unità, ordini, chat, approvazione, secondo player
- [ ] Togliere dal database i dati di prova prima di aprire ad altri
- [ ] Attivare un CAPTCHA o un altro limite per gli accessi anonimi
- [x] Unità rifiutate senza perdite: il tipo si paga all'approvazione, quindi il rimborso non serve più
- [x] Controllare le regole di sicurezza RLS di Supabase (vedi [[Database Supabase]])
- [x] Decidere il futuro del sito: abbandonato per l'app
- [x] Esportare i dati ed eseguire `supabase/pulizia-sito.sql` (vedi [[Database Supabase]])
- [x] Cancellare la copia CSV (già cancellata, confermato il 04/11/2026)
- [x] Spegnere GitHub Pages (04/11/2026)
- [x] Eseguire `supabase/mappa-e-squadra.sql`
- [x] Eseguire `supabase/pu-pubblici.sql` e provare mappa, alleati, PU pubblici e visione comune
- [x] Eseguire `supabase/gm-turni-catalogo.sql` e darsi il ruolo GM
- [x] Eseguire `supabase/matricola-e-gradi.sql`
- [x] Eseguire `supabase/chat-gerarchia-gm.sql`
- [x] Eseguire `supabase/firma-chat.sql`
- [x] Eseguire `supabase/nome-squadrone.sql`
- [x] Eseguire `supabase/gruppi-capitani.sql`
- [x] Eseguire `supabase/richieste-squadrone.sql`
- [x] Eseguire `supabase/gerarchia-squadroni.sql`
- [x] Eseguire `supabase/template-nemici.sql`
- [x] Eseguire `supabase/notifiche.sql`
- [x] Eseguire `supabase/voti-e-chiusura.sql`
- [ ] Provare nell'app la chiusura delle votazioni
- [ ] Provare nell'app notifiche, menzioni, creazione senza componenti e negozio dopo l'approvazione
- [ ] Decidere se aggiungere Firebase per le notifiche immediate ad app chiusa
- [ ] Bilanciare i valori dei template dei nemici
- [x] Correggere il grado scritto con un a capo finale
- [ ] Provare canali della chat, squadroni, votazioni, moderazione e nemici
- [ ] Rendere i danni degli attacchi automatici
- [x] Guardare il nuovo stile su ingresso, creazione unità, scheda GM e nemici
- [ ] Decidere se aggiungere un carattere dedicato, per esempio Chakra Petch
- [x] Provare GM, cambio turno automatico, stipendi e blocchi di movimento e tiro (negozio, timer, mappa 60×80 e descrizioni edifici provati il 04/11/2026)
- [x] Impedire di fermarsi sulla casella di un alleato e di sparare attraverso le unità
- [ ] Controllare la presenza del mezzo o del destinatario per salire e cedere rifornimenti
- [x] Decidere il metodo di accesso: Google + anonimo
- [ ] Sciogliere i dubbi rimasti in [[Account e identità]]
- [x] Prototipo: chat solo in lettura senza unità, messaggi firmati dall'unità
- [x] Spostare PU e grado dall'unità al player
- [ ] Accesso Google reale e bonus PU dal server
- [x] Rendere comune la visione di tutta la squadra
- [ ] Iniziare il [[Multiplayer]]: login e sezione [[Game Master]] minima
- [ ] Aggiornare `origin` di git al nuovo indirizzo `github.com/sector6080/sector`
- [ ] Fare le cosi dette "azioni istantanee" che non hanno bis
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
- [x] Togliere il codice del vecchio sito dal repository
- [x] Mancavano dei campi html → tabella
- [x] html non leggeva più le unità nella riga 2
- [x] Risposte del modulo 1 non organizzate in Foglio1
- [x] Trigger sul server alla modifica del primo foglio
- [x] Bug delle virgole

## Media priorità
- [ ] Timer del turno nella mappa e nel main (fusi orari)
- [ ] Link a Discord, Google Doc (regole), mappa, main, YouTube
- [x] Interfaccia utente migliore
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
- [x] Dark mode: l'app è sempre scura
- [ ] Autosave della pagina
- [ ] Account GitHub
- [x] Palette colori
- [ ] Nuova mail per Sector (Discord / TikTok / Instagram / YouTube)

## ???
- [ ] Cambio password
- [ ] Tutto in inglese (switchabile?)
- [ ] Notifiche fuori dall'app (nuovo turno / unità approvata)
- [ ] Barra di ricerca delle unità
- [ ] Skeleton UI durante il caricamento
- [ ] Apps Script che applica automaticamente gli ordini
