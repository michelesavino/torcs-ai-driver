package scr;

public class Sample {
    private double features[];
    private double acceleration;
    private double steering;
    private double breaking; // Mantenuto il nome 'breaking'
    private int gear;

    public double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }
    public double[] getFeatures() {
        return features;
    }
    
    public double getSteering() {
        return steering;
    }

    public void setSteering(double steering) {
        this.steering = steering;
    }

    
    public double getBreaking() { // Mantenuto il nome 'breaking'
        return breaking;
    }

    public void setBreaking(double breaking) { // Mantenuto il nome 'breaking'
        this.breaking = breaking;
    }

    
    public int getGear() {
        return gear;
    }

    public void setGear(int gear) {
        this.gear = gear;
    }

    // Costruttore con valori pre-parsati (usato per l'input al KNN)
    public Sample(double[] features, double acceleration, double breaking, double steering, int gear) {
        this.features = features;
        this.acceleration = acceleration;
        this.steering = steering;
        this.breaking = breaking;
        this.gear = gear;
    }

    // Crea Sample da riga CSV
    public Sample(String csvLine) {
        // *** LA CORREZIONE È QUI: CAMBIA ";" in "," ***
        String[] parts = csvLine.split(","); 
        
        // Rimuovi la System.err.println() e lancia un'eccezione IllegalArgumentException
        // in caso di numero di colonne errato. Questo è un approccio migliore
        // perché NearestNeighbor può catturare l'eccezione e gestirla.
        if (parts.length != 28) {
            throw new IllegalArgumentException("Numero di colonne errato. Atteso 28, trovato " + parts.length + " nella riga: " + csvLine);
        }
        
        this.features = new double[24];
        
        // Parsing delle 24 features
        for (int i = 0; i < 24; i++) {
            this.features[i] = Double.parseDouble(parts[i].trim());
        }
        
        // Parsing delle 4 azioni/labels
        this.acceleration = Double.parseDouble(parts[24].trim());
        this.breaking = Double.parseDouble(parts[25].trim()); // Corretto: ora corrisponde a 'brake' nell'header
        this.steering = Double.parseDouble(parts[26].trim()); // Corretto: ora corrisponde a 'steering' nell'header
        this.gear = Integer.parseInt(parts[27].trim());
    }

    // Calcola distanza euclidea nello spazio a 24 dimensioni
    public double distance(Sample other) {
        double sum = 0.0;
        for (int i = 0; i < this.features.length; i++) {
            double diff = this.features[i] - other.features[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}