package ugr.soundcompare;

import net.miginfocom.swing.MigLayout;
import org.openimaj.audio.AudioFormat;
import org.openimaj.audio.JavaSoundAudioGrabber;
import org.openimaj.audio.analysis.FourierTransform;
import org.openimaj.video.xuggle.XuggleAudio;
import org.openimaj.vis.audio.AudioSpectrogram;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Jeremy Wood
 * @version 5/10/2017
 */
public class SimpleGUI extends JFrame {

    public static void main(String[] args) throws Exception {
        SimpleGUI gui = new SimpleGUI();
        SwingUtilities.invokeLater(() -> {
            gui.setVisible(true);
        });
        gui.recordVisualization.addComponentListener(gui.recordVisualization);
        gui.recordVisualization.updateVis();
        gui.compareVisualization.addComponentListener(gui.recordVisualization);
        gui.compareVisualization.updateVis();
    }

    AudioSpectrogram recordVisualization = new AudioSpectrogram(800, 400);
    AudioSpectrogram compareVisualization = new AudioSpectrogram(800, 400);

    XuggleAudio sampleAudio;
    FourierTransform sampleAudioFT;

    JavaSoundAudioGrabber audioGrabber;
    FourierTransform audioGrabberFT;


    private SimpleGUI() throws HeadlessException, MalformedURLException {
        setTitle("Sound Compare WIP");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setJMenuBar(initMenuBar());
        getContentPane().add(initComponents());
        pack();
        setLocationRelativeTo(null);

        sampleAudio = new XuggleAudio(new URL("http://www.audiocheck.net/download.php?filename=Audio/audiocheck.net_sweep20-20klin.wav"));
        sampleAudioFT = new FourierTransform(sampleAudio);

        audioGrabber = new JavaSoundAudioGrabber(new AudioFormat(16, 44.1, 1));
        audioGrabberFT = new FourierTransform(audioGrabber);

        new Thread(() -> {
            while (sampleAudioFT.nextSampleChunk() != null) {
                float[][] fftData = sampleAudioFT.getNormalisedMagnitudes(1f/Integer.MAX_VALUE);
                compareVisualization.setData(fftData[0]);
            }
        }).start();
    }

    private JMenuBar initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        return menuBar;
    }

    private JPanel initComponents() {
        JPanel panel = new JPanel(new MigLayout("fill", "[grow]", "[grow][shrink][grow]"));

        JPanel recordPanel = new JPanel(new GridLayout(1, 1));
        recordPanel.add(recordVisualization);
        panel.add(recordPanel, "growx, wrap");

        JButton recordButton = new JButton("Record");
        recordButton.addActionListener(event -> {
            String buttonText = recordButton.getText();
            if (buttonText.equals("Record")) {
                new Thread(() -> {
                    // Wait until audio starts recording
                    while (audioGrabber.isStopped()) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignore) { }
                    }
                    // Run until audio stops recording
                    while (!audioGrabber.isStopped() && audioGrabberFT.nextSampleChunk() != null) {
                        float[][] fftData = audioGrabberFT.getNormalisedMagnitudes(1f/Integer.MAX_VALUE);
                        recordVisualization.setData(fftData[0]);
                    }
                }).start();

                new Thread(audioGrabber).start();

                recordButton.setText("Stop");
            } else {
                audioGrabber.stop();
                recordButton.setText("Record");
            }
        });
        panel.add(recordButton, "growx, wrap");

        JPanel comparePanel = new JPanel(new GridLayout(1, 1));
        comparePanel.add(compareVisualization);
        panel.add(comparePanel);

        return panel;
    }
}
