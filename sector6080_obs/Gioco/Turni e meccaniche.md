# Turni e meccaniche

## Ciclo dei turni
- Un turno dura **24 ore** (decisione del 04/11/2026: il Google Doc dice ancora 2 giorni).
- Il cambio turno è **automatico**: il server controlla ogni minuto, aumenta il turno e paga gli stipendi, anche con tutti i telefoni spenti.
- Gli stipendi vanno ai player con un'unità attiva, in base al grado (vedi [[Gerarchia e Costituzione]]).
- In un turno si possono dare questi ordini **quante volte si vuole e in qualsiasi ordine**, con i limiti indicati:
  - Attivare componenti: **max 1 volta per componente**
  - Salire / scendere da un mezzo
  - Ricaricare componenti
  - Cedere rifornimenti
  - Muoversi: **max 1 volta per turno**
  - Curarsi
- Dettagli e formato in [[Ordini]].

## Rifornimenti
- Un'unità può dare rifornimenti **istantaneamente** a unità o edifici vicini.
- Un'unità adiacente a un **deposito** ripristina di continuo scorte e abilità finché resta vicina, anche se le usa nello stesso turno.
- Non c'è limite ai rifornimenti spendibili in un turno.
- Ogni rifornimento speso permette di scegliere **una** cosa:
  1. ripristinare **tutti gli HP**, oppure
  2. ricaricare **tutti gli utilizzi** di un componente.
- Alcuni componenti pesanti richiedono 2 o 3 rifornimenti per ricaricarsi (es. Cannone 125mm, Testate Nucleari).

## Trasporto
- Salire e scendere da un mezzo è istantaneo.
- Si scende solo su **caselle adiacenti libere**.
- Le unità trasportate **non possono muoversi né attaccare**, ma **possono usare i rifornimenti**.
- Le unità trasportate subiscono gli stessi attacchi del mezzo che le trasporta.
- Il mezzo si muove come un'unità singola, carico compreso.
- Capacità di trasporto: Rampa da Carico (1), Gru Mobile (2), Gru Pesante (4) per la [[Motorizzata]]; Piattaforma Mobile (2 fanti) per la [[Corazzata]].

## Combattimento
- **Perforazione vs armatura**: se l'armatura del bersaglio è **maggiore** della perforazione dell'attacco, l'attacco fallisce.
- **Range**: distanza massima del componente, **senza diagonali** (distanza di Manhattan). Si può sparare anche sopra e dentro l'acqua, ma non attraverso un'altra unità.
- **Utilizzi** `(x/y)`: usi rimasti/massimi; quando finiscono serve un rifornimento per ricaricare.
- Danni `AxB` (es. `3x2`): A attacchi separati da B danni ciascuno.
- Area `NxM`: colpisce il bersaglio e le caselle intorno (es. una granata 3x3 colpisce il bersaglio + 8 caselle).
- A **0 HP** l'unità viene distrutta e tolta dalla mappa.

## Movimento e visione
- Movimento solo orizzontale/verticale; non si esce dalla mappa e non ci si ferma su altre unità né le si attraversa.
- Non si entra in mare o nei fiumi: si attraversano solo i ponti (vedi [[Mappa]]).
- **Nebbia di guerra**: Visione N = vedi N caselle in tutte le direzioni, senza diagonali. La visione è **condivisa**: tutti i player vedono ciò che vede almeno un'unità della squadra.

## Effetti speciali citati
Marcatura delle caselle (Faro Laser, per 2 turni), Bruciatura (1 danno/turno), EMP/disabilitazione, jammer radar, trappole, bersagli volanti, "non può muoversi" (armi pesanti), strutture ed edifici.
