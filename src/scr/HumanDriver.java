package scr;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HumanDriver extends Controller {

	public static ConcurrentLinkedQueue<Character> keyboardInputQueue = new ConcurrentLinkedQueue<>();
	private DataWriter writer;
    private Action currentAction = new Action();

    private boolean acceleratePressed = false;
    private boolean brakePressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private boolean recordingEnabled = true;

    private static final double STEERING_DELTA = 0.1;
    private static final double ACCEL_BRAKE_DELTA = 0.1;
    private static final double STEERING_CENTER_THRESHOLD = 0.05;

    public HumanDriver() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "torcs_manual_data_" + timestamp + ".csv";
        try {
            this.writer = new DataWriter(filename);
            System.out.println("Data writer inizializzato per: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Errore nell'inizializzazione del DataWriter per " + filename + ". La registrazione dei dati potrebbe non funzionare.");
        }
        currentAction.gear = 1;
    }

    @Override
    public Action control(SensorModel sensors) {
        // ... (resto del codice invariato) ...
        double angle = sensors.getAngleToTrackAxis();
        double trackPos = sensors.getTrackPosition();
        double speedX = sensors.getSpeed();
        double speedY = sensors.getLateralSpeed();
        double rpm = sensors.getRPM();
        double[] trackSensors = sensors.getTrackEdgeSensors();

        while (!keyboardInputQueue.isEmpty()) {
            char ch = keyboardInputQueue.poll();

            switch (ch) {
                case 'w': acceleratePressed = true; brakePressed = false; break;
                case 's': brakePressed = true; acceleratePressed = false; break;
                case 'a': leftPressed = true; rightPressed = false; break;
                case 'd': rightPressed = true; leftPressed = false; break;

                case 'W': acceleratePressed = false; break;
                case 'S': brakePressed = false; break;
                case 'A': leftPressed = false; break;
                case 'D': rightPressed = false; break;

                case 'e': currentAction.gear = Math.min(6, currentAction.gear + 1); break;
                case 'q': currentAction.gear = Math.max(-1, currentAction.gear - 1); break;

                case 'p':
                    recordingEnabled = !recordingEnabled;
                    System.out.println("Registrazione dati: " + (recordingEnabled ? "ATTIVA" : "DISATTIVA"));
                    break;
            }
        }

        if (acceleratePressed) {
            currentAction.accelerate = Math.min(1.0, currentAction.accelerate + ACCEL_BRAKE_DELTA);
            currentAction.brake = 0.0;
        } else if (brakePressed) {
            currentAction.brake = Math.min(1.0, currentAction.brake + ACCEL_BRAKE_DELTA);
            currentAction.accelerate = 0.0;
        } else {
            currentAction.accelerate = Math.max(0.0, currentAction.accelerate - ACCEL_BRAKE_DELTA);
            currentAction.brake = Math.max(0.0, currentAction.brake - ACCEL_BRAKE_DELTA);
        }

        if (leftPressed) {
            currentAction.steering = Math.max(-1.0, currentAction.steering - STEERING_DELTA);
        } else if (rightPressed) {
            currentAction.steering = Math.min(1.0, currentAction.steering + STEERING_DELTA);
        } else {
            if (currentAction.steering > STEERING_CENTER_THRESHOLD) {
                currentAction.steering = Math.max(0.0, currentAction.steering - STEERING_DELTA);
            } else if (currentAction.steering < -STEERING_CENTER_THRESHOLD) {
                currentAction.steering = Math.min(0.0, currentAction.steering + STEERING_DELTA);
            }
            if (Math.abs(currentAction.steering) < STEERING_CENTER_THRESHOLD) {
                currentAction.steering = 0.0;
            }
        }

        if (sensors.getSpeed() < 5.0 && currentAction.accelerate == 0.0 && currentAction.gear == 0) {
            currentAction.gear = 1;
        }

        if (recordingEnabled && (sensors.getSpeed() > 1.0 || acceleratePressed || brakePressed || Math.abs(currentAction.steering) > 0.01)) {
            if (writer != null) {
                writer.writeLine(sensors, currentAction);
            }
        }

        return currentAction;
    }

    @Override
    public void reset() {
        System.out.println("HumanDriver reset. Resettando azioni e stato input.");
        currentAction = new Action();
        currentAction.gear = 1;
        keyboardInputQueue.clear();
        acceleratePressed = false;
        brakePressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    @Override
    public void shutdown() {
        if (writer != null) {
            writer.close();
            System.out.println("Data writer chiuso. Dataset salvato correttamente.");
        }
    }
}