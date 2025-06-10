package scr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

// Ho rinominato la classe da ContinuousCharReaderUI a CharReader
public class CharReader extends JFrame {
    private JTextField inputField;
    // Riferimento al tuo HumanDriver per inviare gli input
    private final HumanDriver humanDriver;

    // Il costruttore ora accetta un HumanDriver
    public CharReader(HumanDriver humanDriver) {
        this.humanDriver = humanDriver;

        // Set up the frame
        setTitle("TORCS Human Driver Input"); // Titolo più specifico per TORCS
        setSize(300, 100);
        // NON chiamare setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // perché vogliamo che sia il Client (o TORCS) a terminare l'applicazione,
        // non la chiusura della finestra dell'input.
        setLayout(new FlowLayout());

        // Initialize the text field for input
        inputField = new JTextField(20);
        add(inputField);

        // Add key listener to the text field
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Invia il carattere premuto alla coda del HumanDriver
                humanDriver.enqueueKeyboardInput(e.getKeyChar());
                // Non stampiamo qui, lo farà il driver se necessario.
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Invia il carattere rilasciato alla coda del HumanDriver
                // I caratteri maiuscoli indicano il rilascio del tasto
                switch (e.getKeyChar()) {
                    case 'w': humanDriver.enqueueKeyboardInput('W'); break;
                    case 's': humanDriver.enqueueKeyboardInput('S'); break;
                    case 'a': humanDriver.enqueueKeyboardInput('A'); break;
                    case 'd': humanDriver.enqueueKeyboardInput('D'); break;
                    case 'p':
                        // Il tasto 'p' gestisce il toggle della registrazione nel driver
                        humanDriver.setRecordingEnabled(!humanDriver.isRecordingEnabled());
                        break;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // Puliamo il campo di testo dopo ogni input per mantenerlo pulito
                SwingUtilities.invokeLater(() -> inputField.setText(""));
                // Non usiamo 'q' per uscire da qui, la chiusura dell'applicazione è gestita dal Client.
            }
        });

        // Make the frame visible
        setVisible(true);
    }

    // Rimuoviamo il main() da qui, il CharReader sarà lanciato dal HumanDriver
    // quando necessario.
}