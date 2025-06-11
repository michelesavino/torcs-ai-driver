//aggiunta

package scr; // Stesso package degli altri tuoi driver

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataWriter {

    private PrintWriter writer; // per scrivere sul file CSV
    private boolean headerWritten = false; // per assicurarsi di scrivere l'intestazione solo una volta

    //creo csv
    public DataWriter(String filenamePre)  throws IOException {
        // Genera un nome di file unico usando la data e l'ora attuali.
        // Il formato "yyyyMMdd_HHmmss" crea una stringa come "20250611_122325".
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = filenamePre + "_" + timestamp + ".csv";
        
        writer = new PrintWriter(new FileWriter(filename));
        System.out.println("File CSV aperto per scrittura: " + filename);
    }

    // Scrive una singola riga di dati nel file CSV.
    // Include dati e azioni.
    public void writeLine(SensorModel sensors, Action action) {
        if (!headerWritten) {
            writeHeader();
            headerWritten = true; //la metto a true così la prossima volta non viene riscritta l'intestazione
        }

        StringBuilder sb = new StringBuilder();

        // Aggiungo features
        sb.append(sensors.getAngleToTrackAxis()).append(",");
        sb.append(sensors.getTrackPosition()).append(",");
        sb.append(sensors.getSpeed()).append(",");         // Velocità longitudinale
        sb.append(sensors.getLateralSpeed()).append(",");   // Velocità laterale
        sb.append(sensors.getRPM()).append(",");

        // I 19 sensori del bordo pista
        for (double sensorValue : sensors.getTrackEdgeSensors()) {
            sb.append(sensorValue).append(",");
        }

        // Aggiungo target (azioni)
        sb.append(action.accelerate).append(",");
        sb.append(action.brake).append(",");
        sb.append(action.steering).append(",");
        sb.append(action.gear); // L'ultima colonna non ha la virgola finale

        // Scrivo la riga completa nel file
        writer.println(sb.toString());
    }

    // scrivo l'header
    private void writeHeader() {
        StringBuilder sb = new StringBuilder();

        // Nomi colonne per le features 
        sb.append("angle,trackPos,speedX,speedY,rpm,");

        // Nomicolonne per i 19 sensori di bordo pista
        for (int i = 0; i < 19; i++) {
            sb.append("track").append(i).append(",");
        }

        // Nomi colonne per gli output
        sb.append("accel,brake,steering,gear"); // L'ultima colonna senza virgola finale

        writer.println(sb.toString());
        System.out.println("DataWriter: Intestazione CSV scritta.");
    }

    // Chiude lo stream di scrittura
   public void close() {
    if (writer != null) {
        try {
            writer.close();
            System.out.println("File CSV chiuso.");
        } catch (Exception e) {
            System.err.println("Errore durante la chiusura del file CSV: " + e.getMessage());
        }
    }
}
}