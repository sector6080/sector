# Schema Google Sheet

## Spreadsheet principale (unità + ordini)
- Foglio `Foglio1`: i dati iniziano dalla riga 3 (`PRIMA_RIGA = 3`).
- Foglio `Risposte del modulo 1`: risposte grezze del Google Form.

### Foglio1 – Unità (colonne A–L)
| Col | Campo | Valore iniziale |
|---|---|---|
| A | Approvata | `NO` (lo staff la mette a `SI`) |
| B | Matricola | |
| C | Password | ⚠️ in chiaro |
| D | Nick | |
| E | Tipo | |
| F | Posizione | `N/A` |
| G | Movimento | |
| H | Visione | |
| I | Armatura | |
| J | Rifornimenti | `n/n` |
| K | Trasportata | `N/A` |
| L | Componenti | |

### Foglio1 – Ordini (colonne O–T)
| Col | Campo |
|---|---|
| O | Approvato (`NO`) |
| P | Matricola |
| Q | Movimento |
| R | Salire/Scendere |
| S | Componenti |
| T | Rifornimenti |

- `saveRawFormData` scrive i testi non riconosciuti in colonna **R**, la stessa colonna di Salire/Scendere: possibile conflitto.

### Risposte del modulo 1
- Colonna B: testo inviato dal sito.
- Colonna T: stato di elaborazione (`PROCESSATO` / `ERRORE: ...`).

## Spreadsheet mappa
- Foglio `mappa`: una cella per ogni casella della [[Mappa]].

Vedi: [[Unità]] · [[Ordini]] · [[Architettura sito web]]
