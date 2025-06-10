package scr;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class SimpleDriver extends Controller {

	/* Costanti di cambio marcia */
	final int[] gearUp = { 5000, 6000, 6000, 6500, 7000, 0 };
	final int[] gearDown = { 0, 2500, 3000, 3000, 3500, 3500 };

	/* Constanti */
	final int stuckTime = 25;
	final float stuckAngle = (float) 0.523598775; // PI/6

	/* Costanti di accelerazione e di frenata */
	final float maxSpeedDist = 70;
	final float maxSpeed = 150;
	final float sin5 = (float) 0.08716;
	final float cos5 = (float) 0.99619;

	/* Costanti di sterzata */
	final float steerLock = (float) 0.785398;
	final float steerSensitivityOffset = (float) 80.0;
	final float wheelSensitivityCoeff = 1;

	/* Costanti del filtro ABS */
	final float wheelRadius[] = { (float) 0.3179, (float) 0.3179, (float) 0.3276, (float) 0.3276 };
	final float absSlip = (float) 2.0;
	final float absRange = (float) 3.0;
	final float absMinSpeed = (float) 3.0;

	/* Costanti da stringere */
	final float clutchMax = (float) 0.5;
	final float clutchDelta = (float) 0.05;
	final float clutchRange = (float) 0.82;
	final float clutchDeltaTime = (float) 0.02;
	final float clutchDeltaRaced = 10;
	final float clutchDec = (float) 0.01;
	final float clutchMaxModifier = (float) 1.3;
	final float clutchMaxTime = (float) 1.5;



	private int stuck = 0;

	// current clutch
	private float clutch = 0;

	// VARIABILI AGGIUNTE PER LA RACCOLTA DATI
    private PrintWriter dataWriter;
    private boolean collectingData = true; // fase raccolta
    private int dataCounter = 0; // contatore per il numero di righe scritte

	 public SimpleDriver() {
        if (collectingData) {
            try {
                // Il file verrà salvato nella directory da cui viene eseguito TORCS
                dataWriter = new PrintWriter(new FileWriter("torcs_dataset.csv"));
                
                // INTESTAZIONE DEL CSV:
                // ABBIAMO SCELTO QUESTE 24 FEATURES:
                // angle, trackPos, speedX, speedY, rpm
                // e i 19 trackSensors (track0 a track18)
                // Poi i 4 TARGET: accel, brake, steering, gear
                dataWriter.println("angle,trackPos,speedX,speedY,rpm," +
                                   "track0,track1,track2,track3,track4,track5,track6,track7,track8,track9," +
                                   "track10,track11,track12,track13,track14,track15,track16,track17,track18," +
                                   "accel,brake,steering,gear");
                System.out.println("Inizializzato il data writer per torcs_dataset.csv");
            } catch (IOException e) {
                System.err.println("Errore nell'inizializzazione del data writer: " + e.getMessage());
                e.printStackTrace();
                collectingData = false; // Disabilita la raccolta dati se c'è un errore
            }
        }
    }

	public void reset()  {
        System.out.println("Restarting the race!");
        stuck = 0; // Resetta il contatore "bloccato"
        clutch = 0; // Resetta la frizione
    }
	

	public void shutdown() {
        System.out.println("Bye bye!");
        if (dataWriter != null) {
            try {
                dataWriter.close(); // CHIUDE IL FILE CSV E SALVA I DATI
                System.out.println("Dataset salvato su torcs_dataset.csv");
            } catch (Exception e) {
                System.err.println("Errore nella chiusura del data writer: " + e.getMessage());
            }
        }
    }

	private int getGear(SensorModel sensors) {
		int gear = sensors.getGear();
		double rpm = sensors.getRPM();

		// Se la marcia è 0 (N) o -1 (R) restituisce semplicemente 1
		if (gear < 1)
			return 1;

		// Se il valore di RPM dell'auto è maggiore di quello suggerito
		// sale di marcia rispetto a quella attuale
		if (gear < 6 && rpm >= gearUp[gear - 1])
			return gear + 1;
		else

		// Se il valore di RPM dell'auto è inferiore a quello suggerito
		// scala la marcia rispetto a quella attuale
		if (gear > 1 && rpm <= gearDown[gear - 1])
			return gear - 1;
		else // Altrimenti mantenere l'attuale
			return gear;
	}

	private float getSteer(SensorModel sensors) {
		/** L'angolo di sterzata viene calcolato correggendo l'angolo effettivo della vettura
		 * rispetto all'asse della pista [sensors.getAngle()] e regolando la posizione della vettura
		 * rispetto al centro della pista [sensors.getTrackPos()*0,5].
		 */
		float targetAngle = (float) (sensors.getAngleToTrackAxis() - sensors.getTrackPosition() * 0.5);
		// ad alta velocità ridurre il comando di sterzata per evitare di perdere il controllo
		if (sensors.getSpeed() > steerSensitivityOffset)
			return (float) (targetAngle
					/ (steerLock * (sensors.getSpeed() - steerSensitivityOffset) * wheelSensitivityCoeff));
		else
			return (targetAngle) / steerLock;
	}

	private float getAccel(SensorModel sensors) {
		// controlla se l'auto è fuori dalla carreggiata
		if (sensors.getTrackPosition() > -1 && sensors.getTrackPosition() < 1) {
			// lettura del sensore a +5 gradi rispetto all'asse dell'automobile
			float rxSensor = (float) sensors.getTrackEdgeSensors()[10];
			// lettura del sensore parallelo all'asse della vettura
			float sensorsensor = (float) sensors.getTrackEdgeSensors()[9];
			// lettura del sensore a -5 gradi rispetto all'asse dell'automobile
			float sxSensor = (float) sensors.getTrackEdgeSensors()[8];

			float targetSpeed;

			// Se la pista è rettilinea e abbastanza lontana da una curva, quindi va alla massima velocità
			if (sensorsensor > maxSpeedDist || (sensorsensor >= rxSensor && sensorsensor >= sxSensor))
				targetSpeed = maxSpeed;
			else {
				// In prossimità di una curva a destra
				if (rxSensor > sxSensor) {

					// Calcolo dell'"angolo" di sterzata
					float h = sensorsensor * sin5;
					float b = rxSensor - sensorsensor * cos5;
					float sinAngle = b * b / (h * h + b * b);

					// Set della velocità in base alla curva
					targetSpeed = maxSpeed * (sensorsensor * sinAngle / maxSpeedDist);
				}
				// In prossimità di una curva a sinistra
				else {
					// Calcolo dell'"angolo" di sterzata
					float h = sensorsensor * sin5;
					float b = sxSensor - sensorsensor * cos5;
					float sinAngle = b * b / (h * h + b * b);

					// eSet della velocità in base alla curva
					targetSpeed = maxSpeed * (sensorsensor * sinAngle / maxSpeedDist);
				}
			}

			/**
			 * Il comando di accelerazione/frenata viene scalato in modo esponenziale rispetto
			 * alla differenza tra velocità target e quella attuale
			 */
			return (float) (2 / (1 + Math.exp(sensors.getSpeed() - targetSpeed)) - 1);
		} else
			// Quando si esce dalla carreggiata restituisce un comando di accelerazione moderata
			return (float) 0.3;
	}

	public Action control(SensorModel sensors) {
		// Controlla se l'auto è attualmente bloccata
		/**
			Se l'auto ha un angolo, rispetto alla traccia, superiore a 30°
			incrementa "stuck" che è una variabile che indica per quanti cicli l'auto è in
			condizione di difficoltà.
			Quando l'angolo si riduce, "stuck" viene riportata a 0 per indicare che l'auto è
			uscita dalla situaizone di difficoltà
		 **/
		if (Math.abs(sensors.getAngleToTrackAxis()) > stuckAngle) {
			// update stuck counter
			stuck++;
		} else {
			// if not stuck reset stuck counter
			stuck = 0;
		}
		Action action = new Action();
		// Applicare la polizza di recupero o meno in base al tempo trascorso
		/**
		Se "stuck" è superiore a 25 (stuckTime) allora procedi a entrare in situaizone di RECOVERY
		per far fronte alla situazione di difficoltà
		 **/

		if (stuck > stuckTime) { //Auto Bloccata
			/**
			 * Impostare la marcia e il comando di sterzata supponendo che l'auto stia puntando
			 * in una direzione al di fuori di pista
			 **/

			// Per portare la macchina parallela all'asse TrackPos
			float steer = (float) (-sensors.getAngleToTrackAxis() / steerLock);
			int gear = -1; // Retromarcia

			// Se l'auto è orientata nella direzione corretta invertire la marcia e sterzare
			if (sensors.getAngleToTrackAxis() * sensors.getTrackPosition() > 0) {
				gear = 1;
				steer = -steer;
			}
			clutch = clutching(sensors, clutch);
			// Costruire una variabile CarControl e restituirla
			action.gear = gear;
			action.steering = steer;
			action.accelerate = 1.0;
			action.brake = 0;
			action.clutch = clutch;
			return action;
		}

		else //Auto non Bloccata
		{
			// Calcolo del comando di accelerazione/frenata
			float accel_and_brake = getAccel(sensors);

			// Calcolare marcia da utilizzare
			int gear = getGear(sensors);

			// Calcolo angolo di sterzata
			float steer = getSteer(sensors);

			// Normalizzare lo sterzo
			if (steer < -1)
				steer = -1;
			if (steer > 1)
				steer = 1;

			// Impostare accelerazione e frenata dal comando congiunto accelerazione/freno
			float accel, brake;
			if (accel_and_brake > 0) {
				accel = accel_and_brake;
				brake = 0;
			} else {
				accel = 0;

				// Applicare l'ABS al freno
				brake = filterABS(sensors, -accel_and_brake);
			}
			clutch = clutching(sensors, clutch);

			// Costruire una variabile CarControl e restituirla
			action.gear = gear;
			action.steering = steer;
			action.accelerate = accel;
			action.brake = brake;
			action.clutch = clutch;
		}
		// Questa sezione deve accedere all'oggetto 'action' che contiene le decisioni di guida.
    // Assicurati che 'action' sia stato popolato correttamente dai blocchi if/else sopra.
    if (collectingData && dataWriter != null) {
        StringBuilder sb = new StringBuilder();
        // Aggiungi le features (input dei sensori)
        sb.append(sensors.getAngleToTrackAxis()).append(",");
        sb.append(sensors.getTrackPosition()).append(",");
        sb.append(sensors.getSpeed()).append(","); //per la velocità complessiva
        sb.append(sensors.getLateralSpeed()).append(","); //per la velocità laterale
        sb.append(sensors.getRPM()).append(",");
        
        // Aggiungi tutti i 19 sensori della pista
        for (double sensorValue : sensors.getTrackEdgeSensors()) {
            sb.append(sensorValue).append(",");
        }
        
        // Aggiungi i target (le azioni decise dal driver in questo istante)
        sb.append(action.accelerate).append(",");
        sb.append(action.brake).append(",");
        sb.append(action.steering).append(",");
        sb.append(action.gear); // L'ultima colonna non ha la virgola finale

        dataWriter.println(sb.toString()); // Scrivi la riga nel CSV
        dataCounter++; 
    }

    return action; // Questo deve essere l'UNICA istruzione return del metodo control.
}

	private float filterABS(SensorModel sensors, float brake) {
		// Converte la velocità in m/s
		float speed = (float) (sensors.getSpeed() / 3.6);

		// Quando la velocità è inferiore alla velocità minima per l'abs non interviene in caso di frenata
		if (speed < absMinSpeed)
			return brake;

		// Calcola la velocità delle ruote in m/s
		float slip = 0.0f;
		for (int i = 0; i < 4; i++) {
			slip += sensors.getWheelSpinVelocity()[i] * wheelRadius[i];
		}

		// Lo slittamento è la differenza tra la velocità effettiva dell'auto e la velocità media delle ruote
		slip = speed - slip / 4.0f;

		// Quando lo slittamento è troppo elevato, si applica l'ABS
		if (slip > absSlip) {
			brake = brake - (slip - absSlip) / absRange;
		}

		// Controlla che il freno non sia negativo, altrimenti lo imposta a zero
		if (brake < 0)
			return 0;
		else
			return brake;
	}

	float clutching(SensorModel sensors, float clutch) {

		float maxClutch = clutchMax;

		// Controlla se la situazione attuale è l'inizio della gara
		if (sensors.getCurrentLapTime() < clutchDeltaTime && getStage() == Stage.RACE
				&& sensors.getDistanceRaced() < clutchDeltaRaced)
			clutch = maxClutch;

		// Regolare il valore attuale della frizione
		if (clutch > 0) {
			double delta = clutchDelta;
			if (sensors.getGear() < 2) {

				// Applicare un'uscita più forte della frizione quando la marcia è una e la corsa è appena iniziata.
				delta /= 2;
				maxClutch *= clutchMaxModifier;
				if (sensors.getCurrentLapTime() < clutchMaxTime)
					clutch = maxClutch;
			}

			// Controllare che la frizione non sia più grande dei valori massimi
			clutch = Math.min(maxClutch, clutch);

			// Se la frizione non è al massimo valore, diminuisce abbastanza rapidamente
			if (clutch != maxClutch) {
				clutch -= delta;
				clutch = Math.max((float) 0.0, clutch);
			}
			// Se la frizione è al valore massimo, diminuirla molto lentamente.
			else
				clutch -= clutchDec;
		}
		return clutch;
	}

	public float[] initAngles() {

		float[] angles = new float[19];

		/*
		 * set angles as
		 * {-90,-75,-60,-45,-30,-20,-15,-10,-5,0,5,10,15,20,30,45,60,75,90}
		 */
		for (int i = 0; i < 5; i++) {
			angles[i] = -90 + i * 15;
			angles[18 - i] = 90 - i * 15;
		}

		for (int i = 5; i < 9; i++) {
			angles[i] = -20 + (i - 5) * 5;
			angles[18 - i] = 20 - (i - 5) * 5;
		}
		angles[9] = 0;
		return angles;
	}
}
