# Ordini

Ogni turno il player invia gli ordini della sua [[Unità]]. Limiti e regole sono in [[Turni e meccaniche]].

## Come funziona nell'app
**Niente testo libero**, così il player non può sbagliare a scrivere le coordinate:
1. Sceglie il tipo di ordine. Gli ordini impossibili in quel momento sono disattivati.
2. Se serve, sceglie il componente o la quantità di rifornimenti (pulsanti − e +).
3. Se serve una casella, si apre la **[[Mappa]]**: si avvicina all'unità ed evidenzia solo le caselle valide.
4. Tocca la casella e conferma. L'ordine entra nella catena.
5. Con "Invia ordini" la catena passa al [[Game Master]]. Si può ritirare e modificare.

Le caselle valide vengono calcolate provando ogni casella con le stesse regole della catena, tenendo conto degli ordini già dati: per esempio, dopo un movimento il range parte dalla nuova posizione.

## Tipi di ordine
| Ordine | Si sceglie | Regole controllate |
|---|---|---|
| **Muoversi** | casella sulla mappa | entro il movimento, senza diagonali, max 1 per turno, non da trasportata |
| **Salire su mezzo** | casella adiacente | non già a bordo |
| **Scendere da mezzo** | casella adiacente | solo se a bordo |
| **Attivare componente** | componente, poi casella se ha un range | utilizzi rimasti, max 1 per componente, range, non attacca da trasportata, armi pesanti non dopo un movimento |
| **Ricaricare componente** | componente | costa 1 rifornimento (2–3 per alcuni), non se già carico |
| **Curarsi** | — | costa 1 rifornimento, riporta gli HP al massimo |
| **Cedere rifornimenti** | quantità, poi casella | entro 1 casella (2 con Scarico Avanzato), non più di quelli posseduti |

## Limiti del prototipo
- Senza server gli ordini inviati restano sul telefono.
- Mancano le altre unità: non vengono controllate caselle occupate, presenza di un mezzo su cui salire o di un destinatario per i rifornimenti.
- Salire su un mezzo richiede una casella adiacente: il regolamento non lo dice esplicitamente, è una scelta dell'app da confermare.
- Se rimuovi un ordine, quelli successivi possono diventare non validi: vengono segnati in rosso e bloccano l'invio.

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
