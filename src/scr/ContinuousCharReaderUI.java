package scr;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ContinuousCharReaderUI extends JFrame {
    private JTextField inputField;

    public ContinuousCharReaderUI() {
        // Set up the frame
        setTitle("Continuous Character Reader");
        setSize(300, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Initialize the text field for input
        inputField = new JTextField(20);
        add(inputField);

        // Add key listener to the text field
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Converti il carattere in minuscolo per una gestione più semplice (w e W sono uguali)
                char ch = Character.toLowerCase(e.getKeyChar());

                // Invia il carattere premuto alla coda del HumanDriver
                // HumanDriver.keyboardInputQueue deve essere public static
                HumanDriver.keyboardInputQueue.offer(ch);

                // DEBUG: Stampa solo se necessario
                // System.out.println("Key pressed: " + ch);

                // Se premi 'q', chiudi l'applicazione UI (e quindi il JVM)
                if (ch == 'q') {
                    System.exit(0);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                char ch = Character.toLowerCase(e.getKeyChar());

                // Quando un tasto di azione viene rilasciato, invia un carattere specifico
                // per segnalare al driver di smettere di applicare quell'azione.
                if (ch == 'w') { // Rilasciato l'acceleratore
                    HumanDriver.keyboardInputQueue.offer('W'); // Usa un maiuscolo per il rilascio
                } else if (ch == 's') { // Rilasciato il freno
                    HumanDriver.keyboardInputQueue.offer('S'); // Usa un maiuscolo per il rilascio
                } else if (ch == 'a') { // Rilasciato sterzo sinistra
                    HumanDriver.keyboardInputQueue.offer('A'); // Segnale per centrare
                } else if (ch == 'd') { // Rilasciato sterzo destra
                    HumanDriver.keyboardInputQueue.offer('D'); // Segnale per centrare
                }
            }
        });

        // Questo è CRUCIALE: Assicura che il campo di testo abbia il focus
        // altrimenti non riceverà gli eventi della tastiera.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        // Make the frame visible
        setVisible(true);
    }

    public static void main(String[] args) {
        // Run the UI in the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new ContinuousCharReaderUI());
    }
}
