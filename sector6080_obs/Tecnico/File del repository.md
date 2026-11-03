# File del repository

## App Android
| File | Cosa fa |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle/`, `gradlew*` | Progetto Gradle |
| `app/` | Modulo Android, vedi [[App Android]] |
| `app/.../model/GameMap.kt` | Copia della [[Mappa]] e regole di percorso |
| `app/.../rules/` | Regole di [[Unità]] e [[Ordini]] |
| `app/.../ui/MapRenderer.kt` | Texture della mappa generate dal codice |
| `app/.../ui/screens/` | Schermate: Unità, Ordini, Mappa, Chat, Link |
| `app/src/test/` | Test delle regole |
| `dist/` | APK da condividere (non incluso in git) |

## Sito web
| File | Cosa fa |
|---|---|
| `index.html` | Sito attuale con Supabase |
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
