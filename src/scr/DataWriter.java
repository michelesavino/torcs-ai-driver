//aggiunta

package scr; // Stesso package degli altri tuoi driver

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays; // per facilitare la stampa degli array

public class DataWriter {

    private PrintWriter writer; // Oggetto per scrivere sul file CSV
    private boolean headerWritten = false; // Flag per assicurarsi di scrivere l'intestazione solo una volta

    /**
     * Costruttore della classe DataWriter.
     * Crea un nuovo file CSV con il nome specificato e prepara lo stream di scrittura.
     * @param filename Il nome del file CSV da creare (es. "torcs_manual_data_TIMESTAMP.csv")
     * @throws IOException Se si verifica un errore durante la creazione o l'apertura del file
     */
    public DataWriter(String filename) throws IOException {
        // 'true' come secondo argomento in FileWriter significa appendere al file se esiste,
        // ma dato che il nome del file include un timestamp, sarà sempre un nuovo file.
        writer = new PrintWriter(new FileWriter(filename));
        System.out.println("DataWriter: File CSV aperto per scrittura: " + filename);
    }

    /**
     * Scrive una singola riga di dati nel file CSV.
     * Include i dati dei sensori (input) e le azioni del driver (output/target).
     * @param sensors Il SensorModel corrente fornito da TORCS.
     * @param action L'Action calcolata dal driver per il frame corrente.
     */
    public void writeLine(SensorModel sensors, Action action) {
        // Scrivi l'intestazione solo la prima volta che viene chiamato writeLine
        if (!headerWritten) {
            writeHeader();
            headerWritten = true;
        }

        StringBuilder sb = new StringBuilder();

        // 1. Aggiungi le Features (Input dei Sensori)
        // Questi sono i dati che il tuo modello di AI userà come input.
        sb.append(sensors.getAngleToTrackAxis()).append(",");
        sb.append(sensors.getTrackPosition()).append(",");
        sb.append(sensors.getSpeed()).append(",");         // Velocità longitudinale
        sb.append(sensors.getLateralSpeed()).append(",");   // Velocità laterale
        sb.append(sensors.getRPM()).append(",");

        // I 19 sensori del bordo pista
        for (double sensorValue : sensors.getTrackEdgeSensors()) {
            sb.append(sensorValue).append(",");
        }

        // Puoi aggiungere altri sensori se li ritieni utili per l'AI.
        // Esempio:
        // sb.append(sensors.getFuel()).append(",");
        // sb.append(sensors.getDistanceFromStartLine()).append(",");
        // sb.append(sensors.getDamage()).append(",");
        // sb.append(Arrays.toString(sensors.getWheelSpinVelocity()).replace("[", "").replace("]", "").replace(" ", "")).append(",");


        // 2. Aggiungi i Target (Output/Azioni del Driver)
        // Queste sono le azioni che il tuo modello di AI dovrà imparare a predire.
        sb.append(action.accelerate).append(",");
        sb.append(action.brake).append(",");
        sb.append(action.steering).append(",");
        sb.append(action.gear); // L'ultima colonna non ha la virgola finale

        // Scrivi la riga completa nel file
        writer.println(sb.toString());
        // Facoltativo: Puoi aggiungere un contatore o un log per vedere quante righe hai scritto
        // System.out.println("DataWriter: Scritta riga di dati.");
    }

    /**
     * Scrive l'intestazione (header) del file CSV.
     * Definisce i nomi delle colonne per i sensori e le azioni.
     */
    private void writeHeader() {
        StringBuilder sb = new StringBuilder();

        // Nomi delle colonne per le Features (Input)
        sb.append("angle,trackPos,speedX,speedY,rpm,");

        // Nomi delle colonne per i 19 sensori di bordo pista
        for (int i = 0; i < 19; i++) {
            sb.append("track").append(i).append(",");
        }

        // Puoi aggiungere intestazioni per altri sensori se li aggiungi in writeLine()
        // Esempio:
        // sb.append("fuel,distanceFromStartLine,damage,wheelSpinVelocity0,wheelSpinVelocity1,wheelSpinVelocity2,wheelSpinVelocity3,");


        // Nomi delle colonne per i Target (Output)
        sb.append("accel,brake,steering,gear"); // L'ultima colonna senza virgola finale

        writer.println(sb.toString());
        System.out.println("DataWriter: Intestazione CSV scritta.");
    }

    /**
     * Chiude lo stream di scrittura, assicurandosi che tutti i dati vengano salvati sul file.
     * Questo metodo è CRUCIALE e dovrebbe essere chiamato quando il driver si spegne (nel metodo shutdown()).
     */
    public void close() {
        if (writer != null) {
            try {
                writer.flush(); // Assicura che tutti i dati in buffer vengano scritti su disco
                writer.close(); // Chiude lo stream
                System.out.println("DataWriter: File CSV chiuso.");
            } catch (Exception e) {
                System.err.println("DataWriter: Errore durante la chiusura del file CSV: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
