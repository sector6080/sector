# Mappa

Fonte: foglio Google "Sector 6080 (master)", scheda `mappa` (vedi [[Fonti e link]]). L'app ne contiene una copia in `GameMap.kt`: se il foglio cambia, la copia va aggiornata.

## Dimensioni
- **80 colonne × 59 righe** dentro il bordo nero del foglio.
- ⚠️ Il regolamento parla di griglia 60×80: nel foglio manca una riga. Va deciso se aggiungerla.
- Ambientazione: la **Gran Bretagna** invasa, da riconquistare settore per settore (vedi [[Lore]]).

## Terreni
| Colore nel foglio | Terreno | Caselle | Si attraversa |
|---|---|---|---|
| Verde | Prato | 3174 | sì |
| Giallo | Sabbia | 785 | sì |
| Blu | Mare | 582 | no |
| Azzurro | Fiume | 138 | no |
| Arancione scuro | Ponte | 8 | sì |
| Arancione chiaro | Porto | 18 | sì |
| Bianco | Edificio | 15 | sì |

I significati di giallo, azzurro e arancione scuro sono dedotti dalla mappa: vanno confermati.

## Strutture (coordinate X,Y dell'angolo in alto a sinistra)
| Struttura | Posizione | Dimensione |
|---|---|---|
| Porto 1 | 18,8 | 2×3 |
| Porto 2 | 50,4 | 2×3 |
| Porto 3 | 70,7 | 2×3 |
| Raffineria carburante | 37,10 | 2×2 |
| Raffineria carburante | 78,11 | 2×2 |
| Raffineria carburante | 3,14 | 2×2 |
| Magazzino | 61,15 | 1×1 |
| Magazzino | 25,16 | 1×1 |
| Magazzino | 13,17 | 1×1 |

- Le **nuove unità** partono da un porto, sempre lo stesso per la stessa matricola.
- I **magazzini** sono probabilmente i "depositi" del regolamento, da confermare.
- Le **raffinerie** non hanno ancora regole.

## Regole della mappa nell'app
- Non si entra in acqua. Il percorso si calcola casella per casella senza diagonali, quindi un fiume si attraversa solo su un ponte.
- Vale anche per salire e scendere dai mezzi. I range delle armi invece ignorano l'acqua.
- Il regolamento non parla dell'acqua: è una scelta dell'app, da confermare. Vedi anche [[Turni e meccaniche]].
- Sugli edifici si può camminare, in attesa di una regola.

## Grafica
- Le texture sono **generate dal codice** (`MapRenderer.kt`), senza file di immagine: terreni con rumore casuale ma sempre uguale, coste irregolari con schiuma, onde, ponti in legno, porti con gru, raffinerie con serbatoi e fiamma, magazzini.
- All'apertura la mappa è **rimpicciolita**, con tutta la griglia in vista. La griglia compare quando ci si avvicina.
- Zoom con il pizzico, spostamento con il trascinamento.
- **Nebbia di guerra**: il terreno resta visibile ma più scuro, oltre la visione dell'unità (senza diagonali). I nemici, quando ci saranno, non dovranno mai arrivare al telefono dei player (vedi [[Multiplayer]]).
- Quando un ordine richiede una casella, la mappa si avvicina all'unità ed evidenzia in verde le caselle valide (vedi [[Ordini]]).
- Toccando una casella si leggono coordinate, terreno e struttura.

## Da fare
- Alleati e nemici sulla mappa.
- Ricerca delle unità.
- "Terreni" e "Costruire" sono archiviati, vedi [[DEV note]].
