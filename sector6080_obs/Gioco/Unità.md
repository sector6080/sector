# Unità

## Caratteristiche (regolamento)
| Campo        | Descrizione                                                   |
| ------------ | ------------------------------------------------------------- |
| Matricola    | Codice identificativo, 3 lettere – 3 numeri (es. `XYZ-789`)   |
| Player       | Nome del giocatore (non più il nick Discord)                  |
| Tipo         | [[Fanteria]], [[Motorizzata]], [[Corazzata]], [[Artiglieria]] |
| HP           | Punti vita (`attuali/max`). A 0 l'unità è eliminata           |
| Movimento    | Caselle per turno, solo orizzontale/verticale                 |
| Visione      | Caselle visibili intorno a sé (nebbia di guerra)              |
| Armatura     | Blocca gli attacchi con perforazione inferiore                |
| Rifornimenti | `attuali/max`                                                 |
| Componenti   | Presi dalla lista del proprio tipo, con utilizzi `(x/y)`      |
| Lore         | Origine e motivazione dell'unità                              |
| PU rimasti   | PU avanzati dopo la creazione                                 |

> [!note] Differenze con il vecchio sito
> Il sito non salvava **HP**, **Lore** e **PU rimasti**, e salvava i componenti come testo libero. L'app segue il regolamento.

## Statistiche base per tipo
| Tipo | Costo | HP | Mov | Vis | Armor | Rifornimenti |
|---|---|---|---|---|---|---|
| [[Fanteria]] | 0 PU | 9 | 2 | 3 | 1 | 4 |
| [[Motorizzata]] | 1 PU | 12 | 3 | 3 | 2 | 8 |
| [[Corazzata]] | 4 PU | 14 | 3 | 2 | 3 | 4 |
| [[Artiglieria]] | 5 PU | 8 | 1 | 5 | 1 | 4 |

## Creazione
L'app chiede prima di confermare di aver letto il regolamento. Poi:
1. Scegliere il tipo (il costo si scala dai PU del player: **4 all'inizio, 5 con l'accesso Google**).
2. Creare una matricola non ancora usata.
3. Scrivere una breve lore. Il server calcola le statistiche.
4. Aspettare l'approvazione di un [[Game Master]]: solo allora si paga il tipo.
5. Comprare i componenti del proprio tipo con i PU rimasti: **si può solo dopo l'approvazione**.

In gioco tutti si chiamano con la **matricola**: non esiste un nome del player.

> [!warning] Differenza con il regolamento
> Il Google Doc dice 10 PU, ma la decisione attuale è **4 PU, più 1 con Google**: il doc va aggiornato. Vedi [[Account e identità]].

## Chi modifica l'unità
Costo, HP, rifornimenti e porto di partenza li calcola il server. Approvazioni e modifiche spettano solo ai [[Game Master]].

## Nuovi componenti
Il player può comprare altri componenti del proprio tipo con i PU rimasti, dalla scheda Unità. Si applicano subito e si perdono insieme all'unità.

## Unità rifiutata
Se un GM la rifiuta, tornano tutti i PU spesi per l'unità e i suoi componenti.

## Dispiegamento e morte
- **Un'unità per player**, attiva o in attesa (vedi [[Account e identità]]).
- Quando l'unità arriva a 0 HP viene distrutta e tolta dalla mappa. Il player **tiene i PU, perde il grado e torna Recluta**, e può crearne subito un'altra.
- **Gli armamenti sono persi**: i PU spesi in componenti non vengono restituiti.
- Senza unità attiva o in attesa, il player può solo leggere la chat.
- La nuova unità viene schierata in **uno dei tre porti** il turno dopo la morte.
- I tre porti sono sulla [[Mappa]]: il server assegna a caso una casella libera di un porto.
- Idea nelle [[DEV note]]: una "lista morti".

## Esempio
```
Matricola: XYZ-789     Giocatore: @User546     Tipo: Fanteria
HP 9/9 · Movimento 2 · Visione 3 · Armor 1 · Rifornimenti 3/3
Componenti:
  Fucile d'Assalto – Perf. 1 | Range 3 | 2 danni | (3/3)
  Granata – Perf. 1 | Range 2 | 1 danno (area 3x3) | (1/1)
PU rimasti: 6
```
(Nota: nell'esempio i rifornimenti sono 3/3, ma la base della fanteria è 4/4.)

## Campi del sito
- **Approvata** (`SI`/`NO`), **Posizione**, **Trasportata**, vedi [[Schema Google Sheet]].
