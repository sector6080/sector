# Link del sito:
https://sector6080.github.io/sector/test.html

# Link del API:
* https://script.googleusercontent.com/macros/echo?user_content_key=AehSKLiX9gHrkm_x4DbM4qZDo3JMU8shkKkNOqEeZsfKGxbx_FZJdTp3U_c-itHCRX8TrWwp-HGoDBMmuiCTKXEEBlYF1mXywbwoWAzERUMYqXg8J1Ko0SSnbj6HuBOc3qO87QH2HiJA8nVZII26e4gjw_BmzqllqJzOEqVuQp2ZCxyd5U3wf2jEaSlSYZXf93bQ_B335v1rxuLZHll1wyXIdLllOhDZj5G07yGcYg5u3wwtBxg9XcU7yyOdC56uILaPuEsVMxlIcw62FPzylHMvlOJ-oYL1qQ&lib=MLV1ks7wQQ37SnILDHqD1wVTcg6hxLEu9
* API foglio mappa (beta): AIzaSyAjSe4WyZIJyGk5HLMMytZcm_s4xQJKNU4

# To Do List

## ESSENZIALE:
* cambiare lettere in numeri per le coordinate
* fare il server su firebase
* creare un mail ufficiale del progetto (con cui fare github e server)
* implementare nelle unità un booleano per il cambio turno

## ALTA PRIORITA:
* fare la mappa (mappa centrale+tendina a sinistra con le unita)
* trasportare/rifare/migliorare il backend (agg il cambio turno)
* fare cleaning di tutti i codici (input.html/map.html/appsscript) molte funzioni sono rindondanti o non servono
* staccare i codici .ts dal .html
* ~~ce un bug dove al posto di mandare lunita giusta manda quella precedente (html)~~
* ~~mancano dei field di dati da html->tabella~~
* ~~html non legge piu le unita nella riga 2~~
* ~~risposte del modulo 1 non si organizzano bene in foglio 1 (e bisogna automatizzarlo)~~
* ~~aggiungere trigger nel server quando viene modificato il primo foglio, deve cancellare le righe vecchie nel secondo foglio e chiamar ela funzione giusta~~
* ~~fixxare la cosaa delle virgola~~

## MEDIA PRIORITA:
* mettere nella mappa e main un tempo in modo da non confondere con i fusi orari (es. turno:3 10h 23 min al nuovo turno)
* implementare i link a: discord,doc google(regole),html(mappa),html(main),(youtube?)
* interfacccia utente migliore
* fare sicurezza(anti-spam,capire piu o meno chi/come si accede all api,le password sono sicure?,) (XSS,CSRF,Bruteforce,Session hijacking,Data injection)
* rifare invia ordini (poco sicuro e brutto)
* fixxare CheckOrderValidity
* ~~spostare CheckOrderValidity() nel test.html non nell AS~~

## BASSA PRIORITA:
* Quando un campo è compilato correttamente appare un check ✓; errore → highlight rosso.
* organizzare sistema di notifiche dentro lhtml (da verificare e setuppare)
* login/unita salvato nel locale
* sostituire in ordini salire/scendere da mezzo al posto di posizione->unita

## Next
* dark mode
* autosave della pagina (se la pagina viene chiusa)
* colori palette

## ???:
* implementare l'opzione di cambiare la propria password?
* tradurre TUTTO in inglese(fare che sia switchabile???)
* implementare sistema di notifiche sul browser fuori html (es. ricevi notifica quando nuovo turno/unita approvata??)
* negli html barra di ricerca per le unita sulla mappa/nel main??? 
* skeleton UI durante il caricamento
* AS che implementa automaticamente gli ordini
