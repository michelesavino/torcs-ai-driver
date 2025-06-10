package scr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;
import scr.Controller.Stage;

/**
 * @author Daniele Loiacono (modificato per supportare HumanDriver e altri driver)
 *
 */
public class Client {

    private static final int UDP_TIMEOUT = 10000;
    private static int port;
    private static String host;
    private static String clientId;
    private static boolean verbose;
    private static int maxEpisodes;
    private static int maxSteps;
    private static Stage stage;
    private static String trackName;

    // Flag per indicare se il driver è in modalità di guida autonoma o raccolta dati
    private static boolean guidaAutonoma; 

    /**
     * @param args viene utilizzato per definire tutte le opzioni del client.
     *             - port:N viene utilizzato per specificare la porta per la connessione (il valore predefinito è 3001).
     *             - host:INDIRIZZO viene utilizzato per specificare l'indirizzo dell'host dove il server è in esecuzione (il valore predefinito è localhost).
     *             - id:ClientID viene utilizzato per specificare l'ID del client inviato al server (il valore predefinito è SCR).
     *             - verbose:on viene utilizzato per attivare la modalità verbose (il valore predefinito è spento).
     *             - maxEpisodes:N viene utilizzato per impostare il numero di episodi (il valore predefinito è 1).
     *             - maxSteps:N viene utilizzato per impostare il numero massimo di passaggi per ogni episodio (il valore predefinito è 0, che significa numero illimitato di passaggi).
     *             - stage:N viene utilizzato per impostare lo stadio corrente: 0 è WARMUP, 1 è QUALIFYING, 2 è RACE, altri valori significano UNKNOWN (il valore predefinito è UNKNOWN).
     *             - trackName:nome viene utilizzato per impostare il nome della pista attuale.
     *             - guidaAutonoma:true/false (NUOVO) viene utilizzato per indicare se il driver è in modalità AI (true) o manuale/raccolta dati (false).
     */
    public static void main(String[] args) {
        // Parsing dei parametri dalla riga di comando
        parseParameters(args);
        
        // Dichiarazione della variabile per i messaggi in ingresso
        String inMsg; 

        try {
            // Inizializzazione del gestore della socket per comunicare con TORCS
            SocketHandler mySocket = new SocketHandler(host, port, verbose);
            
            // Caricamento dinamico del driver specificato come primo argomento (es. scr.HumanDriver)
            Controller driver = load(args[0]); 
            
            // Verifica se il driver è stato caricato correttamente
            if (driver == null) { 
                System.err.println("Errore: Impossibile caricare il driver. Uscita.");
                System.exit(1);
            }

            // Imposta lo stadio e il nome della pista per il driver
            driver.setStage(stage);
            driver.setTrackName(trackName);

            /* Costruzione della stringa di inizializzazione per TORCS */
            // Gli angoli iniziali dei sensori del driver
            float[] angles = driver.initAngles(); 
            String initStr = clientId + "(init";
            for (int i = 0; i < angles.length; i++) {
                initStr = initStr + " " + angles[i];
            }
            initStr = initStr + ")";

            long curEpisode = 0;
            boolean shutdownOccurred = false;
            
            // Loop principale per gli episodi di guida
            do {
                /*
                 * Fase di identificazione del client con il server TORCS
                 */
                do {
                    mySocket.send(initStr); // Invia la stringa di inizializzazione
                    inMsg = mySocket.receive(UDP_TIMEOUT); // Attende la risposta con timeout
                } while (inMsg == null || inMsg.indexOf("***identified***") < 0); // Ripete finché non è identificato

                /*
                 * Inizia la guida
                 */
                long currStep = 0;
                while (true) { // Loop per i passi di simulazione all'interno di un episodio
                    /*
                     * Riceve dal server TORCS lo stato del gioco (sensori)
                     */
                    inMsg = mySocket.receive(UDP_TIMEOUT); // Riceve i dati dei sensori con timeout

                    if (inMsg != null) {
                        /*
                         * Controlla se la gara è terminata (shutdown)
                         */
                        if (inMsg.indexOf("***shutdown***") >= 0) {
                            shutdownOccurred = true;
                            System.out.println("Server shutdown!");
                            break; // Esce dal loop interno
                        }

                        /*
                         * Controlla se la gara è stata riavviata
                         */
                        if (inMsg.indexOf("***restart***") >= 0) {
                            driver.reset(); // Resetta lo stato del driver
                            if (verbose) {
                                System.out.println("Server restarting!");
                            }
                            break; // Esce dal loop interno per ricominciare l'identificazione
                        }

                        // Il driver calcola l'azione da intraprendere basandosi sui dati dei sensori
                        Action action = new Action();
                        if (currStep < maxSteps || maxSteps == 0) {
                            action = driver.control(new MessageBasedSensorModel(inMsg));
                        } else {
                            action.restartRace = true; // Se si supera maxSteps, richiede il riavvio
                        }

                        currStep++;
                        mySocket.send(action.toString()); // Invia l'azione calcolata al server
                    } else {
                        System.out.println("Il server non ha risposto entro il timeout.");
                        break; // Esce dal loop interno in caso di timeout prolungato
                    }
                }

            } while (++curEpisode < maxEpisodes && !shutdownOccurred); // Continua per il numero di episodi o finché non c'è shutdown

            /*
             * Shutdown del controller e chiusura della socket
             */
            driver.shutdown();
            mySocket.close(); 
            System.out.println("Client spento.");
            System.out.println("Arrivederci!");
            
        } catch (Exception e) { 
            // Cattura qualsiasi altra eccezione non gestita specificamente
            System.err.println("Errore inatteso del Client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parsa i parametri dalla riga di comando e imposta le variabili statiche del client.
     * @param args Array di stringhe con i parametri.
     */
    private static void parseParameters(String[] args) {
        // Imposta i valori predefiniti per le opzioni
        port = 3001;
        host = "localhost";
        clientId = "SCR";
        verbose = false;
        maxEpisodes = 1;
        maxSteps = 0;
        stage = Stage.UNKNOWN;
        trackName = "unknown";
        guidaAutonoma = false; // Default: modalità manuale/raccolta dati per HumanDriver

        for (int i = 1; i < args.length; i++) {
            StringTokenizer st = new StringTokenizer(args[i], ":");
            String entity = st.nextToken();
            String value = st.hasMoreTokens() ? st.nextToken() : null; // Assicura che ci sia un valore

            if (entity.equals("port")) {
                port = Integer.parseInt(value);
            } else if (entity.equals("host")) {
                host = value;
            } else if (entity.equals("id")) {
                clientId = value;
            } else if (entity.equals("verbose")) {
                if (value != null && value.equals("on")) {
                    verbose = true;
                } else if (value != null && value.equals("off")) {
                    verbose = false;
                } else {
                    System.out.println(entity + ":" + value + " non è un'opzione valida.");
                    System.exit(0);
                }
            } else if (entity.equals("stage")) {
                stage = Stage.fromInt(Integer.parseInt(value));
            } else if (entity.equals("trackName")) {
                trackName = value;
            } else if (entity.equals("maxEpisodes")) {
                maxEpisodes = Integer.parseInt(value);
                if (maxEpisodes <= 0) {
                    System.out.println(entity + ":" + value + " non è un'opzione valida.");
                    System.exit(0);
                }
            } else if (entity.equals("maxSteps")) {
                maxSteps = Integer.parseInt(value);
                if (maxSteps < 0) {
                    System.out.println(entity + ":" + value + " non è un'opzione valida.");
                    System.exit(0);
                }
            } else if (entity.equals("guidaAutonoma")) { 
                // Parsa il valore booleano per il flag guidaAutonoma
                guidaAutonoma = Boolean.parseBoolean(value);
            } else {
                System.out.println("Opzione sconosciuta: " + entity + ":" + value);
                System.exit(0);
            }
        }
    }

    /**
     * Carica dinamicamente un'istanza del driver specificato dal nome della classe.
     * Tenta prima un costruttore con un parametro boolean, poi uno senza parametri.
     * @param name Il nome completo della classe del driver (es. "scr.HumanDriver").
     * @return Un'istanza del Controller (il driver).
     */
    private static Controller load(String name) {
        Controller controller = null;
        try {
            Class<?> driverClass = Class.forName(name);
            
            // 1. Tenta di trovare e usare il costruttore con un parametro boolean
            try {
                Constructor<?> constructor = driverClass.getConstructor(boolean.class);
                controller = (Controller) constructor.newInstance(guidaAutonoma);
                if (verbose) System.out.println("Driver " + name + " caricato con costruttore (boolean), guidaAutonoma=" + guidaAutonoma);
            } catch (NoSuchMethodException e) {
                // 2. Se non esiste un costruttore con boolean, tenta il costruttore senza parametri
                try {
                    Constructor<?> constructor = driverClass.getConstructor();
                    controller = (Controller) constructor.newInstance();
                    if (verbose) System.out.println("Driver " + name + " caricato con costruttore senza parametri.");
                } catch (NoSuchMethodException ex) {
                    // Se nessuno dei due costruttori è disponibile
                    System.err.println("Errore: Il driver " + name + " non ha un costruttore (boolean) né un costruttore senza parametri.");
                    // QUI VIENE RILANCIATA L'ECCEZIONE
                    throw ex; // Questo 'ex' è di tipo NoSuchMethodException
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Errore: La classe '" + name + "' non è stata trovata. Assicurati che il nome sia corretto e il file nel classpath.");
            e.printStackTrace();
            System.exit(1); // Uscita con codice di errore
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | SecurityException | IllegalArgumentException | NoSuchMethodException e) {
            // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            // AGGIUNGI QUI: | NoSuchMethodException
            System.err.println("Errore durante l'istanza del driver " + name + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1); // Uscita con codice di errore
        }
        return controller;
    }
}