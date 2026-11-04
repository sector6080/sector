# App Android (obiettivo)

Trasformare Sector6080 **da sito web/discord ad app Android pubblicata sul Google Play Store**.

**non prendere tutto alla lettera a parte il regolamento sector visto che molte cose sono state fatte per il discord/sito web o besta testing**
## Stato
- **Scelte fatte (03/11/2026):** Kotlin + Jetpack Compose, offline-first (salvataggio locale dietro un'interfaccia `GameRepository`, pronta per Firebase), sviluppo con Android Studio.
- **Prima versione scritta** (progetto Gradle nella root del repo, modulo `app/`, package `com.sector6080.app`):
  - catalogo, mappa, stipendi e impostazioni stanno su Supabase e si modificano senza aggiornare l'app. Le note [[Fanteria]], [[Motorizzata]], [[Corazzata]] e [[Artiglieria]] sono ora un riferimento: **modificarle non cambia più il gioco**, e servono solo ai test;
  - scheda Unità: accesso senza account (Google in arrivo), conferma di aver letto il regolamento, creazione sul server con i PU del player, negozio di componenti, avvisi che si chiudono con la X. Chiuso l'avviso sull'account, resta solo un link grigio "Collega Google" in fondo alla pagina;
  - scheda Ordini: nessun testo libero, opzioni fattibili a sinistra e le altre visibili con il motivo, caselle scelte dalla mappa, invio al [[Game Master]] (si può ritirare);
  - scheda GM: visibile solo ai [[Game Master]], con approvazioni, ordini, unità, gradi e nemici;
  - scheda Squadra: tutte le unità divise per grado, dal più alto, con la scheda di ognuna. Da fuori mostra solo matricola, tipo e nome dello squadrone. Qui i Capitani propongono e votano i gradi;
  - scheda Mappa: turno e conto alla rovescia, mappa dal server 80×60 con texture, edifici con descrizione, rimpicciolita all'avvio, zoom, visione comune della squadra, alleati con scheda pubblica, selezione delle caselle valide, acqua e ponti;
  - scheda Chat: quattro canali sul server, aggiornati ogni 10 secondi. Eventi lo scrive il Comando Supremo, Comune chi ha un'unità, Squadrone chi è in uno squadrone, Comando Capitani, Caporali e GM. Squadrone e Comando compaiono nella barra solo a chi ne fa parte. Dalla chat Squadrone il Caporale sceglie nome e Soldati. Il server firma i messaggi come `grado(matricola)-tipo`, per esempio `Soldato(ASD-543)-Fanteria`;
  - test JVM delle regole in `app/src/test`.
- **Collegamento a Supabase (04/11/2026):** l'app legge e scrive sul server con `RemoteGame.kt`, e tiene una copia sul telefono. Provato dal vivo con due player: vedi [[Database Supabase]].
- La scheda Link è stata tolta. Il regolamento si apre dalla domanda iniziale, e il link Discord non è più nell'app.
- **Ultimo prototipo condivisibile:** v0.6.0 in `dist/` (cartella non inclusa in git), con scheda GM, canali della chat, squadroni, votazioni e nemici. Le versioni precedenti non funzionano più con il database attuale. È una build di debug, da installare a mano.
- **Build verificata (03/11/2026):** AGP 9.5.0, Gradle 9.6.0, Kotlin 2.4.21, compileSdk/targetSdk 37. Test, lint e APK di debug passano, e l'app gira sull'emulatore. Da riga di comando: `gradlew.bat :app:assembleDebug` con `JAVA_HOME` sul JBR di Android Studio.
- Il piano originale del README ("tradurre tutto in Angular") è superato. Il piano per il gioco condiviso è in [[Multiplayer]].

## Funzionalità da portare dal sito
- Creazione delle [[Unità]] con budget in PU, acquisto dei componenti dal catalogo del tipo, calcolo automatico delle statistiche, lore, approvazione da parte dei [[Game Master]]
- Lista delle unità (approvate / in attesa)
- Composizione e invio della catena di [[Ordini]]
- Visualizzazione della [[Mappa]] (zoom e pan, unità sulla mappa)
- Timer del turno
- Link a Discord (facoltativo) e regolamento (Google Doc)

## Funzionalità nuove possibili (dal regolamento)
- Nebbia di guerra per giocatore in base alla visione; mappa master separata con i nemici
- Validazione degli ordini secondo le regole (1 movimento per turno, range e diagonali, adiacenza, trasporto)
- Gradi, stipendio in PU (oggi gestito dal bot Discord con `/collect-income`), raccolta degli ordini da parte dei caporali
- Notifica del nuovo turno (durata: 2 giorni o 24 ore, da decidere, vedi [[Turni e meccaniche]])

## Decisioni aperte
- **Backend e login**: il sito usa già Supabase, quindi il consiglio è restarci. Vedi [[Multiplayer]].
- **Sezione GM**: nella stessa app o separata, vedi [[Game Master]].
- **Notifiche push**: nuovo turno, unità approvata (FCM).
- **Lingua**: italiano / inglese selezionabile.
- **Mappa**: confermare significato dei colori, blocco dell'acqua e riga mancante (59 invece di 60). Vedi [[Mappa]].

## Requisiti Play Store
- Account sviluppatore Google Play (25 $ una tantum).
- Per gli account personali nuovi: closed testing con almeno 12 tester per 14 giorni prima della pubblicazione.
- Privacy policy pubblica (servirà, visto che si raccolgono nick e login).
- Chat e account comportano altri obblighi: vedi [[Questioni legali]].
- Icona, screenshot, scheda dello store, classificazione dei contenuti (tema militare).
- Target API level recente, build AAB firmato.

Vedi: [[Problemi noti e sicurezza]] · [[To Do]]
