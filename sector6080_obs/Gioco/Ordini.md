# Ordini

Il player agisce con la sua [[Unità]] in due modi, ma solo quando è approvata: prima la scheda Ordini lo dice e basta. Limiti e regole sono in [[Turni e meccaniche]].

- **Azioni immediate:** salire su un mezzo, scendere e cedere rifornimenti. Partono subito, in qualsiasi momento, anche dopo aver inviato la catena.
- **Catena di ordini:** muoversi, attivare componenti, ricaricare e curarsi. Il server la esegue allo scadere del turno.

## Azioni immediate
Compaiono in cima alla scheda Ordini solo quando si possono fare, una riga per azione:
- **Scendere da mezzo:** se si è a bordo e c'è una casella libera. Si tocca la casella sulla [[Mappa]].
- **Salire su mezzo:** una riga per ogni mezzo adiacente con posti liberi.
- **Cedere rifornimenti:** una riga per ogni alleato a portata che può riceverne. Accanto si legge quanti ne ha il destinatario, per esempio 2/4. Il pulsante +1 ne cede uno alla volta e il destinatario riceve subito una notifica.

## Catena di ordini
**Niente testo libero**, così il player non può sbagliare a scrivere le coordinate:
1. Sceglie il tipo di ordine. Quelli fattibili stanno a sinistra; gli altri restano visibili, più chiari, e spiegano perché non si possono dare.
2. Se serve, sceglie il componente. Anche quelli non usabili restano visibili, per esempio "Pistola (0/2) · scarico".
3. Se serve una casella, si apre la **[[Mappa]]**: si avvicina all'unità ed evidenzia solo le caselle valide.
4. Tocca la casella e conferma. L'ordine entra nella catena.
5. Con "Invia ordini" la catena passa al server. Si può ritirare e modificare fino allo scadere del turno.

Le caselle valide vengono calcolate provando ogni casella con le stesse regole della catena, tenendo conto degli ordini già dati: per esempio, dopo un movimento il range parte dalla nuova posizione.

## Esecuzione a fine turno
- Il server esegue gli ordini allo scadere del turno, anche con tutti i telefoni spenti.
- Ricontrolla ogni ordine con le stesse regole dell'app. Un ordine non più valido viene saltato e gli altri proseguono.
- Chi ha inviato prima agisce prima.
- Vengono eseguiti solo gli ordini inviati: una catena non inviata si perde.
- Nessuno può applicarli prima. L'unica eccezione è "Passa subito al turno successivo" del [[Game Master]], perché è un vero cambio turno.
- Il player riceve una notifica con l'esito di ogni ordine.

## Attacchi
I danni ai nemici sono automatici:
- L'attacco fallisce se l'armatura del nemico è maggiore della perforazione.
- Un'arma `AxB` toglie A volte B danni.
- Un'area 3x3 è centrata sul bersaglio. Una 2x2 parte dal bersaglio verso destra e verso il basso, una 2x1 verso destra.
- A 0 HP il nemico viene tolto dalla mappa.
- Un colpo su una casella vuota consuma comunque l'utilizzo.

> [!warning] Scelta dell'app da confermare
> Le aree colpiscono solo i nemici, non le unità alleate.

## Effetti dei componenti
**Automatici:**
- i bonus fissi a HP, movimento, visione, armatura e massimo di rifornimenti;
- perforazione, danni, numero di colpi, range e area delle armi;
- i mezzi: Rampa da Carico, Gru Mobile, Gru Pesante, Piattaforma Mobile e Scarico Avanzato;
- i bonus alle armi, compreso il range mostrato sulla mappa;
- le cure a bordo: a ogni cambio turno Cucina da Campo dà 1 HP, Ospedale da Campo 2 HP, Kit da Supporto 1 HP alla sola Fanteria, senza superare il massimo;
- Postazione Comando Mobile: +1 PU ogni 2 unità diverse rifornite nello stesso turno;
- Faro Laser, Supporto Aereo, Fuoco di Correzione, Sistema Ponte Mobile, Bunker Antiproiettile e Sistema ECM, spiegati qui sotto.

### Bonus alle armi
| Componente | Vale per | Effetto |
|---|---|---|
| Proiettili Perforanti | armi da fuoco | +1 Perf |
| Proiettili Esplosivi | armi da fuoco | +1 danno per colpo |
| Mirino Laser | armi da fuoco | +2 Perf se il bersaglio è entro 3 caselle |
| Mirino ACOG | armi da fuoco | +2 Perf se il bersaglio è a 4 caselle o più |
| Mirino Digitale | armi da fuoco | +1 Range; +1 Perf se il bersaglio è a 4 caselle o più |
| Ottica a Doppio Zoom | armi da fuoco | +2 Range, -1 Perf |
| Sistema di Mira Laser | armi da fuoco | +1 Perf, +1 Range |
| Sistema di Guida | tutte le armi della Corazzata | +1 Perf |
| Piattaforma Stabilizzata | tutte le armi dell'Artiglieria | +1 Perf, +1 danno |
| Sistema di Puntamento Laser | tutte le armi dell'Artiglieria | +1 Perf, +1 Range |
| Fuoco di Correzione | tutte le armi dell'Artiglieria | +7 danni per colpo sulle caselle marcate |

I bonus si sommano tra loro. **Armi da fuoco:** Pistola, Pistola Silenziata, SMG, Mitra Avanzato, Mitragliatrice Leggera, Fucile d'Assalto, Fucile a Pompa, Fucile da Tiro, Fucile di Precisione, Fucile Anticarro, Fucile ad Antimateria, Fucile a Reticolo, Kit da Cecchino, Kit da Cecchino Elite. Granate, esplosivi, lanciafiamme, mortai e missili non lo sono. L'elenco è una scelta dell'app: si cambia dal Table Editor, colonna `firearm` di `game_components`.

### Caselle marcate
- Il **Faro Laser** ha range 4 e si usa come ogni altro ordine: la casella viene marcata allo scadere del turno.
- Sulla [[Mappa]] compare un **flare** che tutti vedono, anche nella nebbia. Toccando la casella si legge fino a quale turno è marcata.
- La marcatura fatta alla fine del turno N vale per i turni N+1 e N+2, compresi gli ordini eseguiti alla fine del turno N+2.
- **Supporto Aereo** spara solo su caselle marcate.
- **Fuoco di Correzione** aggiunge 7 danni per colpo sulle caselle marcate.
- Nella stessa catena il Faro Laser vale già per gli ordini che lo seguono.

### Ponti
- **Sistema Ponte Mobile** ha range 1 e 1 utilizzo. Si ricarica come gli altri componenti, quindi ogni ricarica permette un altro ponte.
- Si costruisce solo sul fiume, non sul mare.
- Il ponte **resta per sempre**: la mappa cambia per tutti e i telefoni la riscaricano da soli.

### Armatura delle unità
- **Bunker Antiproiettile:** +4 armor se l'unità non si è mossa nel turno corrente. Un movimento eseguito allo scadere del turno conta per il turno che inizia. Contano anche salire, scendere ed essere trasportati da un mezzo in movimento.
- **Sistema ECM:** +1 armor agli alleati entro 3 caselle, ma non a chi lo monta. Più ECM si sommano.
- Il valore compare nella scheda dell'unità e nella scheda GM. Serve al [[Game Master]] quando risolve gli attacchi dei nemici.

### Effetti attivi a mano
Quando si attiva uno di questi componenti il server consuma l'utilizzo e avvisa il Comando Supremo, che applica l'effetto. La notifica dice da quale casella è stato usato.

| Gruppo | Componenti |
|---|---|
| Cure e rifornimenti | Kit Medico, Cassetta Medica, Kit Riparazione, Kit Riparazione Rapida, Rifornimento Condiviso |
| Bonus temporanei | Siringa Stim, Kit da Rally, Sistema di Fuga Rapida, Scudo Energizzato, Drone Spotter |
| Bonus alle armi | Munizioni Incendiarie, Proiettili AP, Testate Nucleari |
| Controllo | Proiettili EMP, Scaricatore EMP, Jammer Portatile |
| Strutture e bersagli speciali | Kit da Demolitore, Proiettili HE, Missile MANPADS, Trappola Esplosiva |
| Casi particolari | Cannone Fisso 185mm (oggi colpisce solo il bersaglio, non la linea), Fucile a Reticolo (oggi viene bloccato come le altre armi) |

### Senza effetto per ora
Restano nel catalogo e si possono comprare, ma oggi non fanno nulla.

- **Stivali Antimine:** non esistono trappole nemiche. Serve solo se il [[Game Master]] ne aggiunge.
- **Munizioni Esplosive:** gli edifici non hanno HP e non si attaccano. Serve solo se diventeranno bersagli.

## Tipi di ordine
| Ordine | Quando parte | Si sceglie | Regole controllate |
|---|---|---|---|
| **Salire su mezzo** | subito | mezzo adiacente | non già a bordo, nessuna unità a bordo della propria, posto libero per il proprio tipo |
| **Scendere da mezzo** | subito | casella adiacente | solo se a bordo, casella libera e non in acqua |
| **Cedere rifornimenti** | subito | alleato, +1 alla volta | entro 1 casella, diagonali comprese (2 con Scarico Avanzato), non più di quelli posseduti, non oltre il massimo del destinatario |
| **Muoversi** | fine turno | casella sulla mappa | percorso entro il movimento, senza diagonali e senza acqua, max 1 per turno, non da trasportata |
| **Attivare componente** | fine turno | componente, poi casella se ha un range | utilizzi rimasti, max 1 per componente, range, non attacca da trasportata, armi pesanti non dopo un movimento |
| **Ricaricare componente** | fine turno | componente | costa 1 rifornimento (2–3 per alcuni), non se già carico |
| **Curarsi** | fine turno | — | costa 1 rifornimento, riporta gli HP al massimo |

## Mezzi
- Rampa da Carico dà 1 posto, Gru Mobile 1 in più, Gru Pesante 2 in più. Piattaforma Mobile dà 2 posti, solo per la Fanteria.
- Chi è a bordo segue il mezzo e sulla mappa non si vede: compare il mezzo.
- Se il mezzo viene perso, chi era a bordo resta sulla sua casella.

## Limiti del prototipo
- Salire su un mezzo richiede una casella adiacente: il regolamento non lo dice esplicitamente, è una scelta dell'app da confermare.
- I rifornimenti si cedono solo alle unità, non agli edifici.
- Se rimuovi un ordine, quelli successivi possono diventare non validi: vengono segnati in rosso e bloccano l'invio.
- Una catena già inviata può diventare non valida dopo un'azione immediata, per esempio salendo su un mezzo: a fine turno quegli ordini vengono saltati.

## Template del regolamento
Era il formato usato su Discord, ora non serve più all'app:
```
Turno: <turno corrente>
Matricola: <matricola unità>
Coordinate: X,Y
ORDINI:
  movimento da X,Y a X,Y
  sale su X,Y
  scende a X,Y
  usa componente su X,Y   (una riga per ogni componente usato)
```

I **Caporali** nel regolamento raccolgono gli ordini prima di mandarli al comando (vedi [[Gerarchia e Costituzione]]): da decidere se ha ancora senso con l'invio dall'app.
