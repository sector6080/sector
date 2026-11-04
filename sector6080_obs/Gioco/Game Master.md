# Game Master

Sector 6080 è **PvE**: i player collaborano e i nemici sono gestiti dai Game Master (GM).

## Cosa fanno solo i GM
- Approvare le [[Unità]] create dai player.
- Modificare le unità: HP, rifornimenti, posizione, componenti, distruzione.
- Rivedere e applicare gli [[Ordini]].
- Gestire i nemici e far avanzare il turno.
- Moderare la chat: cancellare i messaggi e silenziare i player.

I player possono solo creare la propria unità, comprare componenti, inviare ordini e usare la chat. I GM coincidono con il Comando Supremo.

## Sezione GM
- Nell'app c'è una scheda **GM**, visibile solo a chi ha il ruolo.
- **Si diventa GM solo dalla console di Supabase**, impostando `players.rank` a `Comando Supremo`. Nessuna funzione dell'app può farlo.
- **Il Comando Supremo non può avere unità.** Il server rifiuta la creazione, e chi riceve il grado perde l'unità che ha.
- In chat i GM scrivono firmando `Comando Supremo`.
- Ogni azione viene ricontrollata dal server, quindi nascondere la scheda non è l'unica protezione.

Dalla scheda GM si può:
- **Approvare** un'unità: entra in gioco e la recluta diventa soldato.
- **Rifiutare** un'unità: tornano tutti i PU spesi, componenti compresi.
- **Applicare gli ordini** inviati, con le stesse regole dei player.
- **Cambiare HP e rifornimenti** di un'unità.
- **Segnare un'unità come persa**: il player tiene i PU, perde componenti e grado e torna Recluta.
- **Passare subito al turno successivo**, oltre al cambio automatico.
- **Spostare un'unità**, scrivendo le coordinate lette dalla mappa.
- **Aggiungere e togliere componenti** senza spendere PU: utilizzi e statistiche si aggiornano.
- **Cambiare il grado**, come ordine diretto del Comando Supremo (vedi [[Gerarchia e Costituzione]]).
- **Silenziare un player** in chat. Il silenzio resta anche se crea un'altra unità.
- **Creare, modificare, spostare ed eliminare i nemici**. A 0 HP il nemico viene tolto dalla mappa.

Dalla scheda Chat il GM cancella i messaggi e legge tutti i canali, squadroni compresi, dove però non scrive.

## Nemici
- Hanno nome, HP, armatura, descrizione e posizione.
- Il server li manda ai telefoni solo se sono dentro la **visione comune**, quindi non si vedono nemmeno modificando l'app. Il GM li vede tutti.
- Quelli visibili bloccano movimento e tiro, ma si possono scegliere come bersaglio.
- **I danni non sono automatici:** il GM legge l'ordine di attacco, applica perforazione e armatura e toglie gli HP a mano. Fa lo stesso per gli attacchi dei nemici alle unità.

Vedi [[Multiplayer]].
