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
| `supabase/pulizia-sito.sql` | Ha chiuso il database del sito (già eseguito), vedi [[Database Supabase]] |
| `supabase/schema-app.sql` | Tabelle e regole dell'app, vedi [[Database Supabase]] |
| `supabase/mappa-e-squadra.sql` | Tabella della mappa e funzioni per mappa e squadra |
| `supabase/pu-pubblici.sql` | Aggiunge i PU alla squadra visibile a tutti |
| `supabase/rimuovi-tabelle-sito.sql` | Elimina le vecchie tabelle del sito, vedi [[Database Supabase]] |

## Sito web (abbandonato, GitHub Pages spento)
| File | Cosa fa |
|---|---|
| `index.html` | Ultimo sito con Supabase |
| `map.html` | Mappa del sito |
| `style.css` | Stile del sito |
| `api_stuff/` | Dipendenza `@supabase/supabase-js` (con `node_modules` incluso nel repo) |
| `trasferire_in_backend.txt` | Codice di validazione ordini da spostare nel backend |

`test.html`, `debugger.html` e gli script Apps Script sono stati rimossi dal sito: restano nella storia di git.

## Altro
| File | Cosa fa |
|---|---|
| `foto/…png` | Logo CGD – Consiglio Globale di Difesa |
| `README.md` | Link al sito e al backend + To Do del sito |
| `sector6080_obs/` | Questo vault Obsidian |

Vedi [[Architettura sito web]].
