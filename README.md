# Sector 6080

Wargame PvE cooperativo a turni per Android. Regole, decisioni e documentazione stanno nelle note Obsidian in `sector6080_obs/`, a partire da `00 Home`.

## Cartelle
- `app/`: app Android in Kotlin e Jetpack Compose.
- `supabase/`: script del database.
- `sector6080_obs/`: note del progetto.

## Compilare
Serve il JDK di Android Studio e `local.properties` con il percorso dell'SDK.

```bash
./gradlew assembleDebug testDebugUnitTest
```

## Database da zero
Eseguire gli script nel SQL Editor di Supabase, in quest'ordine:
1. `schema-app.sql`
2. `mappa-e-squadra.sql`
3. `pu-pubblici.sql`
4. `gm-turni-catalogo.sql`
5. `matricola-e-gradi.sql`
6. `chat-gerarchia-gm.sql`
7. `firma-chat.sql`
8. `nome-squadrone.sql`
