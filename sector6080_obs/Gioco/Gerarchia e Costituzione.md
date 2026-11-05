# Gerarchia e Costituzione

Tutto il personale deve rispettare la **Costituzione Marziale**. Lo stipendio è automatico a ogni cambio turno, per i player con un'unità attiva. Gli importi stanno nella tabella `rank_salaries` su Supabase.

All'approvazione della prima unità la recluta diventa soldato. I GM coincidono con il Comando Supremo, che non può avere unità.

## Gradi
| Grado | Ruolo | Stipendio |
|---|---|---|
| **Comando Supremo** | Definisce obiettivi, operazioni e promozioni dei capitani | — |
| **Capitani** | Ruoli organizzativi e tattici: aiutano i giocatori, facilitano le decisioni, coordinano lo schieramento | 3 PU/turno |
| **Caporali** | Gestiscono piccoli sottogruppi nella branca di un Capitano, **raccolgono gli ordini** prima di mandarli al comando | 2 PU/turno |
| **Soldati** | Eseguono le missioni sul campo | 1 PU/turno |
| **Reclute** | Nuove leve, unità in attesa di entrare in partita | — |

Quando l'unità muore il giocatore tiene i PU, ma perde il grado e torna Recluta (vedi [[Unità]]). Deciso il 04/11/2026: il regolamento dice ancora che il grado resta. Il Comando Supremo non ha unità e non è toccato da questa regola.

Nell'app la scheda **Squadra** mostra tutte le unità divise per grado, comprese quelle in attesa. Da fuori si leggono solo matricola, tipo e nome dello squadrone. Toccandone una si apre la sua scheda, con PU, statistiche e lore. Il Comando Supremo e chi non ha un'unità non compaiono, perché non hanno una matricola.

## Mostrine
| Grado | Frecce |
|---|---|
| Recluta | una |
| Soldato | due |
| Caporale | tre |
| Capitano | tre, di cui una invertita |
| Comando Supremo | tre, di cui due invertite |

## Gruppi e squadroni
La gerarchia ha due livelli:
- Il **gruppo** è formato da un Caporale e fino a 6 Soldati.
- Lo **squadrone** è guidato da un Capitano, che sceglie quali gruppi far entrare. Ogni gruppo entra intero, Caporale compreso, e tutti i suoi membri fanno parte dello squadrone.
- Lo squadrone ha fino a 30 unità, contando sia i Caporali sia i Soldati.
- I limiti stanno nella tabella `game_settings` e si cambiano dal Table Editor.

Regole:
- Ognuno ha il suo canale: **Gruppo** per il Caporale e i suoi Soldati, **Squadrone** per il Capitano e tutti i gruppi scelti. Chi non ne fa parte non li vede.
- Chi guida sceglie nome e membri dal pulsante in alto nel canale. Gli altri membri vedono l'elenco.
- Il nome è un testo **senza numeri, di massimo 8 caratteri**.
- I Soldati liberi possono **chiedere di entrare** in un gruppo, i Caporali in uno squadrone, dalla scheda dell'unità nella pagina Squadra. Chi guida accetta o rifiuta dal canale.
- Si può avere una sola richiesta alla volta: una nuova sostituisce la precedente, e si può ritirare.
- Ogni cambio di grado fa uscire dal proprio gruppo o squadrone e libera chi stava sotto. Se un Caporale perde l'unità, il suo gruppo si scioglie ed esce dallo squadrone.

## Costituzione Marziale
### Art. 1 – Promozioni e declassamenti
- **Soldato → Caporale**: un Capitano lo propone come candidato e lo vota il 50%+1 dei Capitani.
- **Caporale → Capitano**: almeno **6 giorni reali** nel ruolo, e poi
  - proposta dei Capitani votata dal 50%+1 dei Capitani, **oppure**
  - ordine diretto del Comando Supremo.
- **Declassamento** di Capitani e Caporali per violazioni delle regole o della Costituzione: con voto dei Capitani oppure con ordine diretto del Comando Supremo.

### Art. 2 – Modifiche alla Costituzione
- Qualsiasi unità può proporre una modifica, con il sostegno di almeno **5 unità** oppure di **un Capitano**.
- La proposta viene scartata se è anticostituzionale.
- I Capitani coinvolti la presentano al Comando Supremo, che ha **2 giorni reali** per confermarla o rigettarla.
- Se viene rigettata, si hanno 48 ore per modificarla e ripresentarla. Dopo **3 rifiuti** proposte simili non vengono più prese in considerazione.

*Data di approvazione: 29/06/2087 (in-game) · Firma: Comando Supremo*

### Come funziona nell'app
- Il **Capitano** apre la scheda di un'unità e propone il grado: il suo voto conta subito a favore.
- Le votazioni stanno in cima alla scheda Squadra. Votano i Capitani, tranne chi è oggetto della proposta. Serve il 50%+1.
- Contano solo i voti di chi è Capitano in quel momento. Se il numero dei Capitani cambia, il conteggio si rifà da solo.
- Chi ha creato la votazione può chiuderla senza esito, anche se nel frattempo ha cambiato grado. Può farlo anche il Comando Supremo.
- Il **Comando Supremo** cambia il grado dalla scheda GM, come ordine diretto.
- I **6 giorni da Caporale** valgono per entrambe le strade e li controlla il server.
- Si cambia un grado alla volta.

> [!warning] Differenza con la Costituzione
> Il Comando Supremo può rendere Caporale un Soldato anche senza voto. La Costituzione non lo prevede, ma senza Capitani iniziali nessuno potrebbe mai diventarlo.

L'art. 2, sulle modifiche alla Costituzione, non è nell'app.

## Comunicazione
- La collaborazione è fondamentale: piani complessi, difese congiunte, assalti sincronizzati, esche.
- Aggiornare sempre il proprio superiore e coordinarsi con i giocatori vicini.
- Non agire da soli: un'unità isolata è un facile bersaglio.
- I canali principali sono nella chat dell'app. Il server Discord è facoltativo (vedi [[Community e staff]]).

Idee nelle [[DEV note]]: branche suddivise per capitano, codice penale, onorificenze, "Regole d'oro".

> [!tip] Per l'app
> Stipendi, votazioni e squadroni sono nell'app. La raccolta degli ordini da parte dei caporali no: gli ordini vanno direttamente ai GM. Vedi [[App Android]].
