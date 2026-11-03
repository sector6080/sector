# App Android (obiettivo)

Trasformare Sector6080 **da sito web/discord ad app Android pubblicata sul Google Play Store**.

**non prendere tutto alla lettera a parte il regolamento sector visto che molte cose sono state fatte per il discord/sito web o besta testing**
## Stato
- **Scelte fatte (03/11/2026):** Kotlin + Jetpack Compose, offline-first (salvataggio locale dietro un'interfaccia `GameRepository`, pronta per Firebase), sviluppo con Android Studio.
- **Prima versione scritta** (progetto Gradle nella root del repo, modulo `app/`, package `com.sector6080.app`):
  - il catalogo dei componenti è letto **direttamente dalle note** [[Fanteria]], [[Motorizzata]], [[Corazzata]], [[Artiglieria]]: se modifichi le note, l'app si aggiorna al build successivo. Va mantenuto il formato `- Nome – Perf. X | Range Y | Z danni | (n/n)`;
  - scheda Unità: conferma di aver letto il regolamento, creazione con 5 PU, statistiche calcolate, prerequisiti ("richiede ..."), scheda in sola lettura;
  - scheda Ordini: nessun testo libero, opzioni impossibili disattivate, caselle scelte dalla mappa, invio al [[Game Master]] (si può ritirare), timer del turno;
  - scheda Mappa: griglia 80×60 rimpicciolita all'avvio, zoom, nebbia di guerra, selezione delle caselle valide;
  - scheda Chat: solo interfaccia, i messaggi restano sul telefono finché non c'è il server;
  - scheda Link: regolamento e server Discord facoltativo;
  - test JVM delle regole in `app/src/test`.
- **Solo interfaccia player:** approvazioni e modifiche alle unità spettano ai [[Game Master]], che avranno una sezione a parte non ancora sviluppata. Nel prototipo le unità restano in attesa di approvazione, il turno resta 0 e non si può ricreare l'unità senza cancellare i dati dell'app.
- **Ultimo prototipo condivisibile:** v0.2.0 in `dist/` (cartella non inclusa in git). È una build di debug, da installare a mano.
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
- **Backend e login**: consigliato Firebase, vedi [[Multiplayer]].
- **Sezione GM**: nella stessa app o separata, vedi [[Game Master]].
- **Notifiche push**: nuovo turno, unità approvata (FCM).
- **Lingua**: italiano / inglese selezionabile.
- **Coordinate di porti e depositi**: oggi le unità partono dalla casella provvisoria 40,30.

## Requisiti Play Store
- Account sviluppatore Google Play (25 $ una tantum).
- Per gli account personali nuovi: closed testing con almeno 12 tester per 14 giorni prima della pubblicazione.
- Privacy policy pubblica (servirà, visto che si raccolgono nick e login).
- Chat e account comportano altri obblighi: segnalazione e blocco degli utenti, eliminazione dell'account (vedi [[Multiplayer]]).
- Icona, screenshot, scheda dello store, classificazione dei contenuti (tema militare).
- Target API level recente, build AAB firmato.

Vedi: [[Problemi noti e sicurezza]] · [[To Do]]
