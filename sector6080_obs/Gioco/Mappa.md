# Mappa

La versione ufficiale è la tabella `game_map` su Supabase, con una riga per casella (vedi [[Database Supabase]]). Un GM può cambiare terreno o struttura dal Table Editor: l'app legge la mappa all'avvio.

Origine: foglio Google "Sector 6080 (master)", scheda `mappa` (vedi [[Fonti e link]]). L'app tiene una copia in `GameMap.kt`, usata finché il server non risponde.

## Dimensioni
- **80 colonne × 60 righe**, come da regolamento. Il foglio ne aveva 59: l'ultima riga, la 60, è stata aggiunta come prato.
- Ambientazione: la **Gran Bretagna** invasa, da riconquistare settore per settore (vedi [[Lore]]).

## Terreni
| Colore nel foglio | Terreno | Caselle | Si attraversa |
|---|---|---|---|
| Verde | Prato | 3254 | sì |
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
| Raffineria 1 | 37,10 | 2×2 |
| Raffineria 2 | 78,11 | 2×2 |
| Raffineria 3 | 3,14 | 2×2 |
| Magazzino 1 | 61,15 | 1×1 |
| Magazzino 2 | 25,16 | 1×1 |
| Magazzino 3 | 13,17 | 1×1 |

- Le **nuove unità** partono da un porto, sempre lo stesso per la stessa matricola.
- I **magazzini** sono probabilmente i "depositi" del regolamento, da confermare.
- Le **raffinerie** non hanno ancora regole.

## Regole della mappa nell'app
- Non si entra in acqua. Il percorso si calcola casella per casella senza diagonali, quindi un fiume si attraversa solo su un ponte.
- Il Sistema Ponte Mobile trasforma per sempre una casella di fiume in ponte, per tutti (vedi [[Ordini]]).
- Il pannello in basso compare solo toccando una casella, sopra la mappa: dice casella, terreno e distanza e apre la scheda di ciò che c'è sopra. Si chiude con Chiudi o toccando di nuovo la casella. Quando si sceglie la casella di un ordine resta sempre aperto.
- Vale anche per salire e scendere dai mezzi.
- **Le armi sparano anche sull'acqua**: il range ignora mare e fiumi, e il bersaglio può trovarsi in acqua.
- Il blocco del movimento in acqua è una scelta confermata, anche se non è nel regolamento. Vedi anche [[Turni e meccaniche]].
- Gli edifici non bloccano il passaggio. Toccandoli si apre una breve descrizione, modificabile nella tabella `game_sites`.
- **Le unità bloccano:** non ci si ferma su un'altra unità e non la si attraversa.
- **Non si spara attraverso un'altra unità:** il controllo segue la linea retta tra chi spara e il bersaglio. L'unità stessa si può colpire.

## Grafica
- Le texture sono **generate dal codice** (`MapRenderer.kt`), senza file di immagine: terreni con rumore casuale ma sempre uguale, coste irregolari con schiuma, onde, ponti in legno, porti con gru, raffinerie con serbatoi e fiamma, magazzini.
- In alto ci sono turno e conto alla rovescia: "prossimo turno in h m s".
- All'apertura la mappa è **rimpicciolita**, con tutta la griglia in vista. La griglia compare quando ci si avvicina.
- Zoom con il pizzico, spostamento con il trascinamento.
- **Nebbia di guerra condivisa**: tutti i player vedono la stessa cosa, cioè l'unione delle visioni di tutte le unità (senza diagonali). Non esiste una visione per singola unità. Fuori dalla visione il terreno resta visibile ma più scuro. I nemici, quando ci saranno, non dovranno mai arrivare al telefono dei player se non sono visibili alla squadra (vedi [[Multiplayer]]).
- Quando un ordine richiede una casella, la mappa si avvicina all'unità ed evidenzia in verde le caselle valide (vedi [[Ordini]]).
- Toccando una casella si leggono coordinate, terreno e struttura.
- **Simboli:** una targa con la sagoma del tipo: elmetto per la Fanteria, camion per la Motorizzata, carro armato per la Corazzata, cannone per l'Artiglieria. Restano leggibili anche con la mappa rimpicciolita. La propria unità ha la stessa misura delle altre e si distingue per il colore ambra.
- **Flare:** le caselle marcate hanno un flare arancione che pulsa, visibile a tutti (vedi [[Ordini]]).
- **Nemici:** rombi rossi con un segnale pulsante, solo dentro la visione comune. Toccandone uno si legge la sua scheda.
- **Alleati:** le unità approvate compaiono in blu, la propria in ambra con un segnale pulsante. Il percorso degli ordini è una linea tratteggiata. Toccandone una, o aprendo l'elenco "Squadra", si legge la sua scheda: grado e PU del player, statistiche, componenti, posizione e lore.

## Da fare
- Nemici sulla mappa.
- Ricerca delle unità.
- "Terreni" e "Costruire" sono archiviati, vedi [[DEV note]].
