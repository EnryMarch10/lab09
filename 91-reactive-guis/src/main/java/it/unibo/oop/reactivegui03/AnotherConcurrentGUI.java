package it.unibo.oop.reactivegui03;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Third experiment with reactive gui.
 */
public final class AnotherConcurrentGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final double WIDTH_PERC = 0.2;
    private static final double HEIGHT_PERC = 0.1;
    private final JLabel display = new JLabel();
    private final JButton up = new JButton("up");
    private final JButton down = new JButton("down");
    private final JButton wait = new JButton("wait");

    private static final long LIFE_TIME = 10_000L;
    private static final long SECOND = 1000L;

    private void buttonsDisable() {
        AnotherConcurrentGUI.this.up.setEnabled(false);
        AnotherConcurrentGUI.this.down.setEnabled(false);
        AnotherConcurrentGUI.this.wait.setEnabled(false);
    }

    /**
     * Builds a new CGUI.
     */
    public AnotherConcurrentGUI() {
        super();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize((int) (screenSize.getWidth() * WIDTH_PERC), (int) (screenSize.getHeight() * HEIGHT_PERC));
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationByPlatform(true);
        final JPanel panel = new JPanel();
        panel.add(display);
        panel.add(up);
        panel.add(down);
        panel.add(wait);
        this.getContentPane().add(panel);
        this.setVisible(true);        
        /*
         * Create the counter agent and start it. This is actually not so good:
         * thread management should be left to
         * java.util.concurrent.ExecutorService
         */
        final Agent agent = new Agent();
        wait.addActionListener((e) -> agent.disable());
        up.addActionListener((e) -> agent.up());
        down.addActionListener((e) -> agent.down());
        new Thread(agent).start();
        /*
         * Register a listener that stops it
         */
        new Thread(() -> {
            try {
                Thread.sleep(LIFE_TIME);
                agent.disable();
            } catch (InterruptedException e1) {
                e1.printStackTrace(); // NOPMD
            }
        }).start();
    }

    private class Agent implements Runnable {
        private volatile boolean stop;
        private volatile boolean up;
        private int counter;

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    // The EDT doesn't access `counter` anymore, it doesn't need to be volatile 
                    final var nextText = Integer.toString(this.counter);
                    SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.display.setText(nextText));
                    if (up) {
                        this.counter++;
                    } else {
                        this.counter--;
                    }
                    Thread.sleep(SECOND);
                } catch (InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace(); // NOPMD
                }
            }
        }

        /**
         * External command to stop counting.
         */
        public void disable() {
            this.stop = true;
            try {
                SwingUtilities.invokeAndWait(() -> AnotherConcurrentGUI.this.buttonsDisable());
             } catch (InvocationTargetException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        /**
         * 
        */
        public void up() {
            this.up = true;
        }
        /**
         * 
         */
        public void down() {
            this.up = false;
        }
    }
}
