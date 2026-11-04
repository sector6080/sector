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

## Vecchio sito
Tolto dal repository il 04/11/2026 perché l'app non lo usa: `index.html`, `map.html`, `style.css`, `api_stuff/`, `trasferire_in_backend.txt`, il logo in `foto/`, `supabase/pulizia-sito.sql` e `supabase/rimuovi-tabelle-sito.sql`. Restano nella storia di git, fino al commit `a322174`, insieme a `test.html`, `debugger.html` e agli script Apps Script.

## Altro
| File | Cosa fa |
|---|---|
| `README.md` | Cartelle del progetto, compilazione e ordine degli script del database |
| `sector6080_obs/` | Questo vault Obsidian |

Vedi [[Architettura sito web]].
