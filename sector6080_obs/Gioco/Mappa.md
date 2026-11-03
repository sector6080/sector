# Mappa

- Griglia **60 × 80**, da cui il nome "Sector 6080". Nel codice: `MAP_WIDTH = 80` colonne, `MAP_HEIGHT = 60` righe.
- Ambientazione: la **Gran Bretagna** invasa, da riconquistare settore per settore (vedi [[Lore]]).
- Elementi citati nelle regole: **tre porti** (punti di schieramento delle nuove unità), **depositi** (ricarica continua dei rifornimenti), edifici/strutture, ponti, coperture.
- **Nebbia di guerra** in base alla visione delle unità.
- Movimento e range senza diagonali.

## Coordinate
- Il regolamento e l'app usano `X,Y` numerici. Il player non le scrive mai: le sceglie toccando la mappa.
- Il vecchio sito usava lettera + numero (`B2`), che con 80 colonne non funzionava.

## Nell'app
- All'apertura la mappa è **rimpicciolita**, con tutta la griglia in vista.
- Zoom con il pizzico, spostamento con il trascinamento.
- Nebbia di guerra in base alla visione dell'unità.
- Quando un ordine richiede una casella, la mappa si avvicina all'unità, evidenzia le caselle valide e consente di confermare solo quelle (vedi [[Ordini]]).
- Nessuno schieramento manuale: le posizioni le decidono i [[Game Master]].

## Vecchio sito
- I dati stanno nel foglio `mappa` di un Google Sheet separato, letto con la Sheets API v4 (vedi [[Architettura sito web]]).
- `map.html` genera la griglia (celle da 20px) e scrive in ogni cella il valore del foglio.
- Controlli: zoom con Ctrl + rotella (1×–3×), spostamento con WASD o frecce.
- Nelle [[DEV note]]: una "**mappa master con nemici**", copia del foglio riservata ai master.

## Da fare
- Coordinate dei tre porti e dei depositi, oggi non definite.
- Alleati e nemici sulla mappa, vedi [[Multiplayer]] (i nemici non devono mai arrivare al telefono dei player).
- Ricerca delle unità.
- "Terreni" e "Costruire" sono archiviati, vedi [[DEV note]].
