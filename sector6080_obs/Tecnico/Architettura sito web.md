# Architettura del sito web (versione attuale)

Sviluppata tra novembre e dicembre 2025. Repo: `github.com/the-silver33/sector`, pubblicato su GitHub Pages.

```
[test.html su GitHub Pages]
   │  GET (fetch, CORS) ─────────────► [Apps Script Web App]  ──► legge Foglio1 → JSON unità
   │
   │  POST form nascosto + iframe ──► [Google Form] ──► foglio "Risposte del modulo 1"
   │                                                     │ trigger onFormSubmit
   │                                                     ▼
   │                                    [Apps Script form_response] ──► scrive su Foglio1
   │
[map.html] ── GET Sheets API v4 (API key) ──► [Google Sheet "mappa"]
```

## Componenti
- **Frontend**: HTML/CSS/JS senza framework (`test.html`, `map.html`, `style.css`, icone Font Awesome 6.4).
- **Lettura dati**: Apps Script pubblicato come Web App (`API_URL` in `test.html`). Il `doGet` che restituisce `{success, data:[unità]}` **non è nel repo**: probabilmente vive solo nell'editor Apps Script (il file `order_manager(AS)` è vuoto).
- **Scrittura dati**: per aggirare i problemi CORS con Apps Script, il sito invia un **Google Form** nascosto con un unico campo (`entry.1167099953`). Il campo contiene un testo tipo `REGISTRAZIONE - Matricola: X, Password: Y, ...` oppure `ORDINE - Matricola: X, Tipo: ...`.
- **Elaborazione**: `form_response(AS)` parsa il testo dividendolo sulle virgole (`extractValue`). Il trigger `onFormSubmit` scrive le unità e gli ordini su `Foglio1` e marca la riga come `PROCESSATO` nella colonna T. Esiste anche un `doPost` JSON (`action: register | submitOrder`), che però non viene usato dal sito.
- **Mappa**: Google Sheet separato letto direttamente con la Sheets API e una API key.

## Limiti
- Le virgole nei campi di testo rompono il parsing.
- Il client non sa se la scrittura è andata a buon fine: risolve dopo il caricamento dell'iframe o dopo un timeout di 5 secondi.
- Latenza di Apps Script, quote, nessuna autenticazione reale.
- Vedi [[Problemi noti e sicurezza]].

Per l'app il piano in [[To Do]] è: "tradurre tutto in Angular" e "fare il server su Firebase". Vedi [[App Android]].
