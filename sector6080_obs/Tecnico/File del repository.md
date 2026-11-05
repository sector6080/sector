# File del repository

## App Android
| File | Cosa fa |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `gradlew*` | Progetto Gradle |
| `app/` | Modulo Android, vedi [[App Android]] |
| `app/.../model/GameMap.kt` | Copia della [[Mappa]] e regole di percorso |
| `app/.../data/RemoteGame.kt` | Collegamento a Supabase |
| `app/.../rules/` | Regole di [[Unità]] e [[Ordini]], visione condivisa |
| `app/.../ui/MapRenderer.kt` | Texture della mappa generate dal codice |
| `app/.../ui/ComponentArt.kt` | Texturine del negozio, generate dal codice: un disegno per famiglia di componenti |
| `app/.../ui/screens/` | Schermate: Unità, Ordini, Mappa, Chat, Link |
| `app/src/test/` | Test delle regole |
| `dist/` | APK da condividere (non incluso in git) |

## Database
| File | Cosa fa |
|---|---|
| `supabase/schema-app.sql` | Tabelle e regole dell'app, vedi [[Database Supabase]] |
| `supabase/mappa-e-squadra.sql` | Tabella della mappa e funzioni per mappa e squadra |
| `supabase/pu-pubblici.sql` | Aggiunge i PU alla squadra visibile a tutti |
| `supabase/gm-turni-catalogo.sql` | Sezione GM, turni automatici, stipendi, catalogo e mappa 60×80 |
| `supabase/matricola-e-gradi.sql` | Toglie il nome del player, grado perso con l'unità, firma della chat, squadra per grado |
| `supabase/chat-gerarchia-gm.sql` | Canali della chat, moderazione, squadroni, votazioni, nemici e strumenti GM |
| `supabase/firma-chat.sql` | Firma dei messaggi: `grado(matricola)-tipo` |
| `supabase/nome-squadrone.sql` | Nome dello squadrone scelto dal Caporale |
| `supabase/gruppi-capitani.sql` | Squadroni dei Capitani e limiti in `game_settings` |
| `supabase/richieste-squadrone.sql` | Richieste per entrare in uno squadrone |
| `supabase/gerarchia-squadroni.sql` | Gruppi dei Caporali dentro gli squadroni dei Capitani, canale Gruppo |
| `supabase/fix-cambio-turno.sql` | Corregge il cambio turno manuale del GM |
| `supabase/template-nemici.sql` | Template tra cui il GM sceglie i nemici |
| `supabase/notifiche.sql` | Notifiche di gioco e menzioni in chat |
| `supabase/voti-e-chiusura.sql` | Conteggio dei voti sui Capitani attuali e chiusura delle votazioni |
| `supabase/ordini-automatici.sql` | Ordini eseguiti dal server, danni automatici, mezzi e rifornimenti immediati |
| `supabase/componenti-passivi.sql` | Bonus alle armi, cure a bordo e Postazione Comando Mobile |
| `supabase/marcature-e-ponti.sql` | Caselle marcate, ponti permanenti, armatura di Bunker ed ECM, descrizioni uniformi |
| `supabase/squadra-senza-unita.sql` | Comando Supremo e Reclute senza unità nella scheda Squadra |
| `supabase/rifornimenti-in-diagonale.sql` | I rifornimenti si cedono anche in diagonale |
| `supabase/gm-player.sql` | Sezione Player della scheda GM: elenco, PU, silenzio e cancellazione |

## Vecchio sito
Tolto dal repository il 04/11/2026 perché l'app non lo usa: `index.html`, `map.html`, `style.css`, `api_stuff/`, `trasferire_in_backend.txt`, il logo in `foto/`, `supabase/pulizia-sito.sql` e `supabase/rimuovi-tabelle-sito.sql`. Restano nella storia di git, fino al commit `a322174`, insieme a `test.html`, `debugger.html` e agli script Apps Script.

## Altro
| File | Cosa fa |
|---|---|
| `README.md` | Cartelle del progetto, compilazione e ordine degli script del database |
| `sector6080_obs/` | Questo vault Obsidian |

Vedi [[Architettura sito web]].
