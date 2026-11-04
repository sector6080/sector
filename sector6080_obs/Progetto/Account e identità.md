# Account e identità

Come si lega un player alla sua [[Unità]]. Obiettivo: il minimo di passaggi per il player, senza che nessuno possa muovere unità altrui.

## Regole decise
- Un player ha **al massimo una unità** attiva o in attesa.
- Il player **scrive in chat solo tramite la sua unità**, anche se è una recluta.
- Senza unità attiva o in attesa, il player **può solo leggere** la chat.
- **PU e grado appartengono al player**: restano se l'unità viene persa.
- **I componenti appartengono all'unità**: se viene persa, i PU investiti in armamenti sono persi.

## Stati del player
| Stato | Accesso | Chat | Ordini | Note |
|---|---|---|---|---|
| Visitatore | nessuno | legge | no | può leggere il regolamento |
| Unità in attesa | sì | scrive | da definire | aspetta un [[Game Master]] |
| Unità attiva | sì | scrive | sì | |
| Unità persa | sì | legge | no | tiene PU e grado, può crearne subito un'altra |

## Opzioni di accesso
| Opzione | Per il player | Sicurezza | Problema |
|---|---|---|---|
| **Accesso con Google** | un tocco, nessuna password | alta | serve un account Google (quasi sempre presente su Android) |
| Account anonimo automatico | nessun passaggio | media | disinstallando l'app si perde l'accesso e si può ricominciare da capo, aggirando il limite di una unità |
| Link o codice via email | nessuna password, ma deve aprire la mail | alta | più lento, serve configurare l'invio delle email |
| Accesso con Discord | un tocco per chi lo usa | alta | Discord è facoltativo, quindi non può essere l'unico metodo |
| Username e password | da inventare e ricordare | dipende dalle password | serve comunque l'email per il recupero |
| Password per unità (vecchio sito) | da ricordare | bassa | da non riproporre |

## Decisione: Google + anonimo
Deciso il 03/11/2026. L'accesso si chiede quando il player crea l'unità, non all'apertura dell'app. Il nome mostrato in gioco resta il campo "Player": email e nome Google non vengono mostrati.

- **PU iniziali: 4 per tutti.**
- **Con l'accesso Google: +1 PU**, quindi 5. Vale una volta sola, anche se il collegamento avviene dopo la creazione dell'unità.
- L'app avvisa che **senza Google, disinstallando l'app o cambiando telefono, l'unità è persa**.
- L'app spiega che Google si può collegare **in qualsiasi momento**.
- Il bonus reale dovrà essere assegnato dal server, non dall'app.

**Stato:** oggi funziona solo l'accesso anonimo, con account reale sul server. Il pulsante Google è visibile ma disattivato: serve configurare Google e assegnare il bonus dal server. Il vecchio accesso simulato è stato tolto. Con 4 PU l'Artiglieria (5 PU) non è disponibile, con la Corazzata (4 PU) non restano PU per i componenti.

## Passaggio futuro a solo Google
Fattibile con Supabase (documentazione controllata il 03/11/2026).

**Se il gioco cresce:**
1. L'app invita gli anonimi a collegare Google, con un avviso chiaro e una scadenza.
2. Una regola RLS impedisce agli anonimi di creare nuove unità (controllo `is_anonymous`).
3. Si disattivano i nuovi accessi anonimi.
4. Si eliminano a mano gli anonimi rimasti: Supabase non lo fa da solo.

**Cosa conferma la documentazione:**
- Un utente anonimo può collegare Google con `linkIdentity`. Il client Kotlin ha `linkIdentityWithIdToken`.
- Va attivato "Manual Linking", che è ancora in beta.
- Il collegamento fallisce se quell'account Google è già legato a un altro utente.
- Chi esce, cancella i dati o cambia telefono prima di collegare Google perde l'accesso, senza recupero.
- Per gli accessi anonimi è raccomandato un CAPTCHA. Il limite predefinito è di 30 richieste all'ora per IP.

**Da provare prima di contarci:**
- Che l'id dell'utente resti uguale dopo il collegamento: la documentazione lo lascia intendere ma non lo dice. Le unità dipendono da questo.
- Cosa succede alle sessioni anonime già aperte quando si disattivano i nuovi accessi.
- Come usare un CAPTCHA dentro un'app Android.

**Rischio della fase iniziale:** il limite di una unità è debole, perché basta reinstallare per avere un nuovo account. Si può anche creare un'unità in attesa per scrivere in chat, quindi i GM devono poter bloccare un player.

## Cosa protegge davvero l'unità
La sicurezza non dipende dalla password ma dal legame sul server:
- Ogni unità ha un proprietario (`owner_id`), impostato dal server con l'id dell'account.
- Il database accetta ordini solo dal proprietario dell'unità, con le regole RLS di Supabase.
- Conoscere la matricola o il nome di un'unità non dà alcun potere.
- Il limite di una unità è un vincolo del database, non un controllo dell'app.
- La chat accetta messaggi solo da chi ha un'unità attiva o in attesa.

## Limiti
- Il massimo di una unità vale **per account**, non per persona: chi ha due account Google può creare due player. L'approvazione dei GM è il secondo filtro.
- Il Play Store richiede comunque di poter eliminare l'account dall'app.

## Da decidere
- **PU alla nuova unità**: i 4 PU (5 con Google) valgono solo la prima volta, poi si usano i PU del player?
- **Artiglieria**: va bene che con 4 PU non sia disponibile?
- **Unità in attesa**: oggi può preparare gli ordini ma non inviarli, finché un GM non la approva. Da confermare.
- **Unità rifiutata da un GM**: i PU vengono restituiti?
