# Game Master

Sector 6080 è **PvE**: i player collaborano e i nemici sono gestiti dai Game Master (GM).

## Cosa fanno solo i GM
- Approvare le [[Unità]] create dai player.
- Modificare le unità: HP, rifornimenti, posizione, componenti, distruzione.
- Rivedere e applicare gli [[Ordini]].
- Gestire i nemici e far avanzare il turno.
- Moderare la chat (da definire).

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

Nemici e moderazione della chat non sono ancora nella scheda.

Vedi [[Multiplayer]].
