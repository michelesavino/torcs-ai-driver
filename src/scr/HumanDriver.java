package scr;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.SwingUtilities; // Necessario per avviare la GUI in modo sicuro

public class HumanDriver extends Controller {

    // Questa è una coda  per i caratteri che premiamo sulla tastiera.
    // Praticamente io dal CharReader prendo i caratteri che utilizzo per guidare e poi vedo a quale azione corrispondono
    public static ConcurrentLinkedQueue<Character> keyboardInputQueue = new ConcurrentLinkedQueue<>();
    
    // Per scrivere i dati su un file CSV.
    private DataWriter dataRecorder;
    
    // Questo oggetto contiene le azioni che l'auto farà: accelerazione, freno, sterzo, marcia.
    private Action carActions = new Action();

    // Ci dicono se stiamo premendo un tasto per accelerare, frenare, ecc.
    private boolean isAccelerating = false; //accelerare
    private boolean isBraking = false; //frenare
    private boolean isTurningLeft = false; //giro a sinistra
    private boolean isTurningRight = false; //giro a destra

    //marce
    private final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 }; 
    private final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };
    // Soglia di velocità sotto la quale consideriamo la macchina quasi ferma per la prima marcia
    private static final double MIN_SPEED_FOR_1ST_GEAR = 5.0; 
    // Soglia di velocità per la retromarcia (da usare solo quando la macchina è quasi ferma)
    private static final double SPEED_MAX_FOR_REVERSE = 1.0; 

    // flag CHE ci permette di attivare o disattivare la registrazione dei dati.
    private boolean shouldRecordData = true;

    // Questi valori ci dicono di quanto "spostare" acceleratore/freno/sterzo ad ogni piccolo passo.
    private static final double STEERING_INCREMENT_STEP = 0.05; //quanto lo sterzo gira ad ogni pressione del tasto
    private static final double ACCEL_BRAKE_INCREMENT_STEP = 0.05; //quanto accelera ad ogni pressione del tasto
    private static final double STEERING_CENTER_THRESHOLD = 0.03; // quanto sterzo deve essere vicino allo zero per essere dirtto

    //costruttore
    public HumanDriver() {
        carActions.gear = 1; // La macchina inizia sempre in prima marcia.

        // Inizializziamo il nostro DataWriter.
        try {
            dataRecorder = new DataWriter("manual_driving_data");
            System.out.println("File creato correttamente");
        } catch (IOException e) {
            // Se c'è un errore a creare il file, lo stampiamo e disabilitiamo la raccolta dati.
            System.err.println("Errore creazione file" + e.getMessage());
            e.printStackTrace();
            this.shouldRecordData = false; 
        }
        
        // Avviamo la finestra per la tastiera (CharReader).
        SwingUtilities.invokeLater(() -> new CharReader(this));
        System.out.println("Finestra avviata");
    }

    /**
     * Questo è il cuore del driver. TORCS lo chiama di continuo,
     * passandoci i dati dei sensori della macchina.
     * Noi dobbiamo decidere cosa fare (accelerare, frenare, sterzare).
     * @param sensors I dati che la macchina "vede" (velocità, posizione sulla pista, sensori di distanza, ecc.).
     * @return L'oggetto Action con le decisioni che la macchina deve eseguire.
     */
    @Override
    public Action control(SensorModel sensors) {
        // ascoltiamo e interpretiamo i tasti premuto.
        handleKeyboardInputs();

        // Traduciamo i tasti premuti in azioni (accelera ecc).
        updateCarControls();

        // metodo per la marcia automatica
        updateAutomaticGear(sensors);

        // Se la registrazione è attiva e la macchina si sta muovendo (o stiamo dando input),
        // scriviamo i dati (sensori + azioni) nel nostro file CSV.
        if (shouldRecordData && dataRecorder != null && 
    (sensors.getSpeed() > 1.0 || isAccelerating || isBraking || carActions.steering > 0.01 || carActions.steering < -0.01)) {
    dataRecorder.writeLine(sensors, carActions);
}

        return carActions; // Diamo a TORCS le azioni 
    }

    //Legge i caratteri dalla coda della tastiera e aggiorna i nostri "interruttori" (isAccelerating, isTurningLeft, ecc.).
     
    private void handleKeyboardInputs() {
        while (!keyboardInputQueue.isEmpty()) {
            char keyChar = keyboardInputQueue.poll(); // Prendo il tasto dalla coda

        if (keyChar == 'w') {
            // Tasti per accelerare,frenare, girare
                isAccelerating = true;
                isBraking = false; 
            } else if (keyChar == 's') {
                isBraking = true;
                isAccelerating = false;
            } else if (keyChar == 'a') {
                isTurningLeft = true;
                isTurningRight = false;
            } else if (keyChar == 'd') {
                isTurningRight = true;
                isTurningLeft = false;
            }

            // Tasti rilasciati (maiuscoli): disattiviamo l'azione.
            // Questo permette alla macchina di rilasciare gradualmente acceleratore/freno/sterzo.
            else if (keyChar == 'W') {
                isAccelerating = false;
            } else if (keyChar == 'S') {
                isBraking = false;
            } else if (keyChar == 'A') {
                isTurningLeft = false;
            } else if (keyChar == 'D') {
                isTurningRight = false;
            }
            // Tasto 'p' per attivare/disattivare la registrazione
            else if (keyChar == 'p') {
                setRecordingEnabled(!isRecordingEnabled());
            }
        }
    }

    /**
     * Questa funzione prende lo stato dei nostri "interruttori" (isAccelerating, ecc.)
     * e aggiorna i valori di accelerazione, freno e sterzo dell'auto.
     * Lo fa a piccoli passi per rendere la guida più fluida e realistica.
     */
    private void updateCarControls() {
        // Logica per Acceleratore
        if (isAccelerating) {
            carActions.accelerate += ACCEL_BRAKE_INCREMENT_STEP;
            // Se acceleriamo troppo, lo limitiamo a 1.0 (il massimo).
            if (carActions.accelerate > 1.0) carActions.accelerate = 1.0;
            carActions.brake = 0.0; // Non possiamo accelerare e frenare allo stesso tempo.
        } else if (isBraking) {
            carActions.brake += ACCEL_BRAKE_INCREMENT_STEP;
            // Se freniamo troppo, lo limitiamo a 1.0 (il massimo).
            if (carActions.brake > 1.0) carActions.brake = 1.0;
            carActions.accelerate = 0.0; // Non possiamo frenare e accelerare allo stesso tempo.
        } else {
            // Se non stiamo accelerando né frenando, i valori scendono gradualmente a zero.
            carActions.accelerate -= ACCEL_BRAKE_INCREMENT_STEP;
            if (carActions.accelerate < 0.0) carActions.accelerate = 0.0;

            carActions.brake -= ACCEL_BRAKE_INCREMENT_STEP;
            if (carActions.brake < 0.0) carActions.brake = 0.0;
        }

        // Logica per Sterzo
        if (isTurningLeft) {
            carActions.steering += STEERING_INCREMENT_STEP;
            // Limita lo sterzo al massimo valore positivo (tutto a sinistra).
            if (carActions.steering > 1.0) carActions.steering = 1.0;
        } else if (isTurningRight) {
            carActions.steering -= STEERING_INCREMENT_STEP;
            // Limita lo sterzo al minimo valore negativo (tutto a destra).
            if (carActions.steering < -1.0) carActions.steering = -1.0;
        } else {
            // Se non stiamo girando, raddrizziamo gradualmente lo sterzo.
            if (carActions.steering > STEERING_CENTER_THRESHOLD) {
                carActions.steering -= STEERING_INCREMENT_STEP;
                // Se supera lo zero, lo mettiamo a zero per precisione.
                if (carActions.steering < 0.0) carActions.steering = 0.0; 
            } else if (carActions.steering < -STEERING_CENTER_THRESHOLD) {
                carActions.steering += STEERING_INCREMENT_STEP;
                // Se supera lo zero, lo mettiamo a zero per precisione.
                if (carActions.steering > 0.0) carActions.steering = 0.0;
            } else {
            // Se lo sterzo è molto vicino allo zero, lo impostiamo a zero preciso.
                carActions.steering = 0.0;
            }
        }
    }

    private void updateAutomaticGear(SensorModel sensors) {
        int currentGear = carActions.gear;
        double rpm = sensors.getRPM();
        double speed = sensors.getSpeed();

        // se non sto in sesta o retro e gli RPM sono superiori alla soglia definita per quella marcia e sto accelerando, salgo di marcia
        if (currentGear >= 1 && currentGear < 6 && rpm >= gearUp[currentGear - 1] && carActions.accelerate > 0) {
            carActions.gear++;
            // System.out.println("Cambiato in su a marcia: " + carActions.gear); // Puoi decommentare per debug
        }
        
        // se non sto né in prima né in retro e gli RPM sono inferiori alla soglia per quella marcia, e non sto accelerando o frenando, scalo
        if (currentGear > 1 && rpm <= gearDown[currentGear - 1] && carActions.accelerate <= 0) {
            carActions.gear--;
            // System.out.println("Cambiato in giù a marcia: " + carActions.gear); // Puoi decommentare per debug
        }
        
        // Se la macchina è quasi ferma e si frena intensamente, e sono in folle, passa in retromarcia.
        if (speed < SPEED_MAX_FOR_REVERSE && carActions.brake > 0.5 && carActions.gear == 0) {
            carActions.gear = -1;
        }
        // invece se sto in retro e mi sto inziiando a muovere davanti, passo in prima
        else if (currentGear == -1 && speed > 0.5) {
            carActions.gear = 0;
        
        }
        // se sono fermo e non sto accelerando e non sto in retro, metto la prima
        else if (speed < MIN_SPEED_FOR_1ST_GEAR && carActions.accelerate == 0.0 && carActions.gear != -1) {
            carActions.gear = 1;
        }
    }

     // Questo metodo permette al CharReader di dire se dobbiamo registrare i dati o meno.

    public void setRecordingEnabled(boolean enabled) {
        this.shouldRecordData = enabled;
        if (enabled) {
        System.out.println("Registrazione dati: ATTIVA");
    } else {
        System.out.println("Registrazione dati: DISATTIVA");
    }
}
    

    //Questo metodo dice al CharReader se la registrazione è attualmente attiva.
    public boolean isRecordingEnabled() {
        return this.shouldRecordData; //vero se attiva, falso se disattiva
    }

    //Aggiunge un carattere premuto dalla tastiera alla nostra coda di input.
     
    public void enqueueKeyboardInput(char keyChar) {
        keyboardInputQueue.offer(keyChar); // "Metti" il carattere nella coda.
    }

    // reset (es. nuova gara)
    @Override
    public void reset() {
        carActions = new Action(); // Reset delle azioni a zero.
        carActions.gear = 1; // Metto la prima
        keyboardInputQueue.clear(); // Svuoto la coda dei tasti.
        // Resetto i flag dei tasti premuti.
        isAccelerating = false;
        isBraking = false;
        isTurningLeft = false;
        isTurningRight = false;
        // Non chiudo il datawriter perché per il dataset potrei voler continuare a registrare per più gare
    }

    //Questo metodo viene chiamato da TORCS quando il driver viene "spento" (es. chiudi il gioco).
    
    @Override
    public void shutdown() {
        if (dataRecorder != null) {
            dataRecorder.close(); // Chiudo il file CSV in modo sicuro cosi mi salvo i dati.
            System.out.println("HumanDriver: Registratore dati chiuso. Dataset salvato correttamente!");
        }
    }
}
