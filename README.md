# TURING - distributed collaborative editing
Turing è una applicazione JAVA che permette a più utenti di modificare simultanamente documenti via rete. Progetto realizzato per il corso di Reti.

## Compilazione ed esecuzione
L’unica dipendenza esterna è, per il server, la libreria Apache Commons CLI per la gestione dei parametri da linea di comando.

Si può compilare il progetto con maven, che si occuperà anche di reperire le dipendenze. Il risultato della compilazione si potrà trovare nella cartella target.
```
cd TuringServer && mvn install
cd TuringClient && mvn install
```
Per eseguire i programmi:
```
cd target
java -jar TuringServer.jar
```
Ai programmi sono passati tramite parametro le opzioni necessarie al loro funzionamento, come la porta d’ascolto per il server, o l’indirizzo a cui connettersi per il client. Alcune opzioni possono non essere specificate, perché ci sono valori di default o perché possono non essere necessarie per una certa esecuzione del programma (es. un client che non intende effettuare la registrazione al servizio può non specificare la porta del Remote Object Registry)

Legenda:
```
TuringClient <hostname> <port> [optional <rmi-
registry-port>]

TuringServer -l <port> [OPTIONS]
-h,--help
 show help
-l,--listen
 server listen port
-m,--rmi
 RMI port (default 1099)
-r,--registry
 registry port (default 7432)
```
Esempio di esecuzione del programma:
```
java -jar TuringServer-1.0.jar -l 9999 -r
7432 -m 3737
java -jar TuringClient-1.0.jar localhost 9999 7432
[...]
Turing: $ register utente password
Registrazione avvenuta con successo
Turing: $ register utente2 password2
Registrazione avvenuta con successo
Turing: $ login utente password
login avvenuto con successo
Turing: @utente $ create documento 10
inserimento eseguito con successo
Turing: @utente $ edit documento 0
sezione scaricata con successo in
/tmp/turing11432698582708773061/documento.0
È possibile modificare il file.
Turing: @utente $ end-edit documento 0
documento inviato con successo
Turing: @utente $ share documento utente2
documento condiviso con successo
Turing: @utente $ logout
logout eseguito con successo
Turing: $ login utente2 password2
login avvenuto con successo
Turing: @utente2 $
sei stato invitato a collaborare a questi
documenti: documento
premi invio per continuare
```

## Descrizione generale
TuringServer è un server multithreaded che accetta connessioni in ingresso sulla porta specificata come parametro al suo avvio e le affida immediatamente ad un thread pool. Un thread si prenderà cura di gestire la comunicazione con il client, verificarne le credenziali ed effettuare le operazioni richieste.

TuringClient presenta all’utente, dopo un messaggio di aiuto relativo alle funzionalità, un prompt simile ad un interprete di comandi. Il thread principale rimane in attesa di input dall’utente e invia le corrispondenti richieste al server, mostrando poi il riscontro – positivo o negativo – del server. Per permettere la ricezione immediata delle notifiche di invito, un thread dispatcher riceve i dati dal server e sulla base di questi informa l’utente oppure consegna il messaggio al thread principale.

Ogni documento che ha modifiche in corso ha associato un indirizzo multicast dedicato alla chat. Quando tutte le modifiche sul documento sono terminate l'indirizzo è rimesso nella coda degli indirizzi disponibili.

Per evitare di sovraccaricare il server, la chat è implementata con un paradigma peer-to-peer: il server ha il solo compito di coordinare i client informandoli dell'IP dedicato alla chat, e questi provvedono a mettersi in ascolto di messaggi provenienti dagli altri client.
La lettura dei documenti può avvenire in maniera concorrente da parte di più lettori anche dopo una richiesta di edit. Dopo che è stata ricevuta una richiesta di edit per una certa sezione, successive richieste di edit per la stessa sezione sono interdette. Invece per garantire la consistenza dei dati offerti ai lettori, ai file sul server sono associate Read-Write Lock. Le lock sono acquisite solo contestualmente alle effettive operazioni di lettura/scrittura su disco.

La verifica della correttezza dei dati inviati avviene sia da parte del client che del server. Questo permette al server di continuare ad operare anche in caso di client che non rispettano il protocollo o cercano di compiere operazioni illegali (ad esempio modificare più di un documento oppure compiere altre operazioni prima di rilasciare la lock sul documento con end-edit).

La lock associata ad una sezione di un documento è rilasciata in caso di terminazione improvvisa della connessione e, per garantire la consistenza, il file sul server viene effettivamente scritto solo dopo che il client ha terminato di inviare i dati (in questo modo i client lettori potranno leggere il documento o nello stato precedente alla modifica o in quello successivo, ma mai una scrittura parziale, anche se il client che invia una modifica subisce una terminazione anomala). Il client e il server registrano uno shutdown hook per eliminare i file temporanei, alla chiusura dell’applicazione.

La registrazione al servizio avviene tramite RMI. Il client e il server si scambiano tramite serializzazione un oggetto che contiene il tipo di operazione richiesta e le informazioni necessarie al suo compimento, oppure il risultato di una certa operazione. Tali informazioni possono essere reperite e impostate tramite i relativi metodi getter e setter. Di seguito i tipi di operazione e i valori di ritorno.
```
public enum OP {
      LOGIN,
      LOGOUT,
      CREA,
      INVITA,
      ELENCA,
      EDIT,
      ENDEDIT,
      SHOW,
      SHOWALL,
      NOTIFICA
    }
    public enum STATUS {
      SUCCESS,
      FAILURE
    }
```

## Thread e strutture dati
Il client attiva al login un thread dispatcher dedicato a ricevere i dati provenienti dal server e lo termina dopo l’operazione di logout. Similmente un thread dedicato a rimanere in ascolto dei messaggi di chat è creato dopo l’operazione di edit e terminato dopo quella di end-edit. Due LinkedBlockingQueue sono utilizzate per trasmettere i dati di interesse dai thread secondari al main thread.

Il server utilizza un CachedThreadPool per la gestione delle operazioni del client. La gestione di utenti e documenti è mediata dalle classi users e documentEl, che nascondono la rappresentazione interna e garantiscono la thread-safety. In tal modo il codice del worker può rimanere incentrato sulla comunicazione e restare invariato in caso di modifiche che riguardino lo storage di dati o metadati.

Per permettere accesso concorrente e operazioni atomiche sono utilizzate Concurrent Collections: ConcurrentHashMap per la  lista di documenti e utenti, ConcurrentLinkedQueue per la lista di notifiche pendenti, e ancora ConcurrentHashMap per la lista dei documenti cui un utente è autorizzato ad accedere.
Ove ciò non sia possibile a causa di operazioni complesse (es. verificare che un utente esista e sia online, assegnare un nuovo indirizzo multicast solo al primo che fa richiesta di edit) sono utilizzati blocchi e metodi synchronized.
