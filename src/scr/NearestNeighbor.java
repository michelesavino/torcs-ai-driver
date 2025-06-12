//K-NearestNieighbor
package scr;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NearestNeighbor {

    private List<Sample> trainingData;
    private KDgraph kdgraph;

    private final String firstLineOfTheFile =
        "angle,trackPos,speedX,speedY,rpm,track0,track1,track2,track3,track4,track5,track6,track7,track8,track9,track10,track11,track12,track13,track14,track15,track16,track17,track18,accel,brake,steering,gear";


    // Costruttore: carica i dati dal file CSV
    public NearestNeighbor(String filename) {
        trainingData = new ArrayList<>();
        loadCSV(filename);
    }

    // Carica i dati dal CSV saltando l'intestazione
    private void loadCSV(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(firstLineOfTheFile.trim())) {
                    continue;
                }
                try {
                    trainingData.add(new Sample(line));
                } catch (Exception e) {
                    System.err.println("Errore nel parsing della riga: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file CSV: " + filename);
            e.printStackTrace();
        }
        if (!trainingData.isEmpty()) {
            this.kdgraph = new KDgraph(trainingData);
        } else {
            System.err.println("Dataset vuoto! KDgraph non sarà inizializzato.");
        }
    }

    // Metodo classico 1-NN
    public Output classify(Sample input) {
        return classify(input, 1);
    }

    // Metodo k-NN
    public Output classify(Sample input, int k) {
        if (trainingData.isEmpty()) {
            System.err.println("Dataset vuoto: impossibile classificare.");
            return null;
        }

        List<Sample> neighbors = kdgraph.kNearestNeighbors(input,k);
        double sumAccel = 0;
        double sumSteer = 0;
        double sumBr = 0;
        int sumGear = 0;

        for (Sample s : neighbors) {
            sumAccel += s.getAcceleration();
            sumSteer += s.getSteering();
            sumBr += s.getBreaking();
            sumGear += s.getGear();
        }

        int actualK = Math.min(k, neighbors.size());
        return new Output(
            sumAccel / actualK,
            sumBr / actualK,
            sumSteer / actualK,
            (int) Math.round((double) sumGear / actualK)
        );
    }

    public static class Output {
        public final double acceleration;
        public final double breaking;
        public final double steering;
        public final int gear;

        public Output(double acceleration, double breaking, double steering, int gear) {
            this.acceleration = acceleration;
            this.breaking = breaking;
            this.steering = steering;
            this.gear = gear;
        }
    }

    public List<Sample> getTrainingData() {
        return trainingData;
    }
}
