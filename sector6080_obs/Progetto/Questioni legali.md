# Questioni legali

Tutto ciò che riguarda leggi, regole del Play Store, dati personali e diritti sull'app.

> [!warning] Non è consulenza legale
> Questa nota serve a non dimenticare nulla. Prima della pubblicazione conviene far controllare privacy policy e termini a un professionista.

**Legenda:** ✅ controllato sulle pagine ufficiali di Google il 03/11/2026 · ❓ da confermare.

## Prima della pubblicazione
- [ ] Decidere chi è il responsabile legale dell'app (titolare del trattamento)
- [ ] Privacy policy pubblicata su un indirizzo web
- [ ] Termini d'uso, da accettare prima di scrivere in chat
- [ ] Eliminazione dell'account dall'app e da una pagina web
- [ ] Segnalazione e blocco degli utenti in chat
- [ ] Moderazione attiva e sanzioni
- [ ] Modulo Data safety su Play Console
- [ ] Questionario sulla classificazione dei contenuti
- [ ] Età minima decisa
- [ ] Accordo scritto tra le persone del team
- [ ] Controllo di nomi, logo e licenze
- [ ] Controllo dei dati dei vecchi giocatori

## Play Store
### Privacy e Data safety ✅
- La **privacy policy è obbligatoria** per compilare il modulo Data safety.
- Il modulo serve per ogni app pubblicata, compresi i test chiusi e aperti. Sono esentate le app attive solo nel test interno.
- Vanno dichiarati i dati raccolti o condivisi e il loro scopo, compresi quelli raccolti da librerie di terzi.
- Per noi: identificativo dell'account, email (con Google), nome del player, messaggi della chat, unità e ordini.

### Eliminazione dell'account ✅
- Chi può creare un account nell'app deve poter chiedere di eliminarlo. Vale anche se parte dell'app funziona senza account.
- Serve un percorso nell'app, ben visibile, oppure un link interno alla pagina web di eliminazione.
- Serve anche una pagina web, da usare senza dover reinstallare l'app.
- I dati vanno cancellati anche dai fornitori. Ciò che si conserva (es. per sicurezza) va spiegato nella privacy policy.
- ❓ Gli account anonimi sono sul server, quindi probabilmente la regola vale anche per loro.
- ❓ Cosa fare con i messaggi di un account eliminato: cancellarli o renderli anonimi.

### Chat: contenuti degli utenti ✅
- Prima di scrivere, il player deve **accettare i termini d'uso**, che devono definire e vietare i contenuti e i comportamenti non ammessi.
- Serve una segnalazione nell'app per contenuti e utenti.
- Con la chat serve anche il **blocco degli utenti**.
- La moderazione deve essere efficace e continua, con azioni concrete su contenuti e utenti.
- Gli utenti, soprattutto i minori, vanno protetti da minacce e bullismo.
- La classificazione dei contenuti deve tenere conto di ciò che scrivono gli utenti.

I [[Game Master]] sono i moderatori naturali: servono strumenti per bloccare un player, anche anonimo.

**Dal 04/11/2026 la chat è collegata al server:** prima di aprire l'app a sconosciuti servono termini, segnalazione, blocco e moderazione, oggi assenti.

### Pubblicazione ✅
- Account sviluppatore **personale** creato dopo il 13/11/2023: serve un test chiuso con **almeno 12 tester per 14 giorni consecutivi**. Chi esce e rientra riparte da zero.
- ❓ Quali dati personali Google mostra sulla scheda dello sviluppatore: da controllare prima di aprire l'account.
- ❓ Classificazione: guerra, armi, testate nucleari e chat tra utenti incidono sul risultato.

## Privacy e GDPR ❓
- L'app è aperta al pubblico, quindi il GDPR si applica anche a un progetto senza guadagni.
- **Titolare del trattamento:** va indicato chi risponde dei dati, con un contatto. Oggi è una decisione aperta.
- **Diritti dei player:** accesso, correzione e cancellazione.
- **Fornitori:** Supabase e Google trattano i dati per nostro conto. Vanno controllati i loro accordi sul trattamento dei dati.
- **Dove stanno i dati:** preferire un server in Unione Europea, altrimenti valutare il trasferimento.
- **Conservazione:** decidere quanto tenere chat e unità perse, compresa un'eventuale "lista morti".
- **Account anonimi:** identificativi e messaggi possono comunque essere dati personali.
- **Schede pubbliche:** nome del player, grado, PU, statistiche, lore e posizione delle unità approvate sono visibili a tutti, anche senza account. Va detto nella privacy policy, e il campo Player non dovrebbe contenere il nome reale.
- **Bonus Google:** +1 PU premia il collegamento dell'account. La privacy policy deve spiegare chiaramente cosa cambia, e va verificato che il bonus non crei problemi.

### Minori
- In Italia il consenso digitale autonomo parte da 14 anni. Va deciso se fissare un'età minima, tenendo conto di chat e tema militare.

## Termini d'uso
Devono coprire almeno:
- Comportamenti vietati in chat e sanzioni, compresa la perdita dell'accesso.
- Autorità dei Game Master e del Comando Supremo.
- Valore dei **PU**: moneta di gioco senza valore reale, non rimborsabile.
- Perdita dell'unità in gioco e perdita per disinstallazione senza Google.
- Possibilità di modificare o chiudere il servizio.

### Pagamenti
Oggi l'app è gratuita e senza acquisti. Se in futuro si vendessero PU o altri contenuti, servirebbero una nuova verifica delle regole di Google, dei diritti dei consumatori e degli aspetti fiscali.

## Proprietà intellettuale ❓
- **Team:** al gioco hanno contribuito più persone (vedi [[Community e staff]]) e al codice almeno `l-godeassi`. Serve un accordo scritto su chi possiede il gioco e chi può pubblicarlo.
- **Licenza del codice:** nel repository non c'è un file di licenza. Se è pubblico, è visibile ma non significa che altri possano riusarlo.
- **Logo CGD:** chiarire chi l'ha creato e con quale strumento. L'icona attuale dell'app è provvisoria.
- **Nome "Sector 6080":** cercare eventuali marchi simili prima della pubblicazione.
- **Nomi di prodotti reali:** nel catalogo c'è "Mirino ACOG", che è un nome commerciale. Meglio un nome generico. Da ricontrollare anche gli altri componenti.
- **Pulsante Google:** va rispettata la grafica prevista da Google per l'accesso.
- **Librerie open source:** controllare le licenze e aggiungere una schermata con gli avvisi richiesti.

## Dati dei vecchi giocatori ❓
- Il vecchio sito salvava le **password in chiaro** e le inviava a ogni visitatore (vedi [[Problemi noti e sicurezza]]).
- Va controllato dove sono finiti quei dati: Google Sheet, Supabase, storia di git.
- Su Supabase le colonne password sono state eliminate il 03/11/2026 (vedi [[Database Supabase]]). La copia CSV può ancora contenerle, e restano da controllare il vecchio Google Sheet e la storia di git.
- Chi ha riusato la stessa password altrove potrebbe essere a rischio. Conviene cancellare i dati non più necessari e chiedere a un professionista se serve avvisare le persone coinvolte.
- Non migrare mai quelle password nella nuova app.

## Fonti Google (03/11/2026)
- [Contenuti generati dagli utenti](https://support.google.com/googleplay/android-developer/answer/9876937)
- [Eliminazione dell'account](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Test per i nuovi account personali](https://support.google.com/googleplay/android-developer/answer/14151465)

Vedi: [[App Android]] · [[Account e identità]] · [[Multiplayer]]
