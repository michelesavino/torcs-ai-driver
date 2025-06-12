package scr;

import scr.NearestNeighbor.Output;

public class SimpleDriver extends Controller {

    private int stuck = 0;
    private NearestNeighbor knn;
    private String prototypes_filename;
    private Action action;
    private double[] features = new double[24];
    private double angolo;
    private float clutch = 0;

    public SimpleDriver() {
        action = new Action();
        prototypes_filename = "dataset.csv";
        this.knn = new NearestNeighbor(prototypes_filename); // Usa KDgraph internamente
    }

    public void reset() {
        System.out.println("Restarting the race!");
    }

    public void shutdown() {
        System.out.println("Bye bye!");
    }

    public double normalizzatoreMinMax(double data, double min, double max) {
        return (data - min) / (max - min);
    }

    @Override
    public Action control(SensorModel sensors) {
        features[0] = normalizzatoreMinMax(sensors.getAngleToTrackAxis(), -Math.PI, Math.PI);
        features[1] = normalizzatoreMinMax(sensors.getTrackPosition(), -2.0, 2.0);
        features[2] = normalizzatoreMinMax(sensors.getSpeed(), 0.0, 250);
        features[3] = normalizzatoreMinMax(sensors.getLateralSpeed(), -1.0, 200);
        features[4] = normalizzatoreMinMax(sensors.getRPM(), 0, 10000000.0);

        for (int i = 0; i < 19; i++) {
            features[5 + i] = normalizzatoreMinMax(sensors.getTrackEdgeSensors()[i], -1.0, 200);
        }

        double angolo = sensors.getAngleToTrackAxis();
        predAction();
        return action;
    }

    public void predAction() {
        Sample input = new Sample(features, 0.0, 0.0, 0.0, -1);
        Output predicted = knn.classify(input, 3); // Usa KDGraph internamente

        if (predicted != null) {
            action.accelerate = (float) predicted.acceleration;
            action.brake = (float) predicted.breaking;
            action.steering = (float) predicted.steering;
            action.gear = predicted.gear;
        } else {
            fallback();
        }
    }

    private void fallback() {
        // Azione di default in caso di output nullo
        action.accelerate = 0;
        action.brake = 1;
        action.steering = 0;
        action.gear = 1;
    }

    public int getStuck() {
        return stuck;
    }

    public void setStuck(int stuck) {
        this.stuck = stuck;
    }

    public NearestNeighbor getNn() {
        return knn;
    }

    public void setNn(NearestNeighbor nn) {
        this.knn = nn;
    }

    public String getPrototypes_filename() {
        return prototypes_filename;
    }

    public void setPrototypes_filename(String prototypes_filename) {
        this.prototypes_filename = prototypes_filename;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public float getClutch() {
        return clutch;
    }

    public void setClutch(float clutch) {
        this.clutch = clutch;
    }
}
