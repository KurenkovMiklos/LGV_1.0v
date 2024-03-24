import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 *
 * @author Thanasis1101
 * @version 1.0
 */
public class VisualizationWindow extends JFrame {

    private MainPanel mainPanel;
    private OptionsPanel optionsPanel;
    private JLabel infoLabel;
    BufferedImage image = null;
    public VisualizationWindow() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setSize(2000,1000);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(null);
        setTitle("Zoomable Panel");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        VertexPlacementAlgorithm VPA = new ForceAtlas2();

        // Load the image that will be shown in the panel

        //image = ImageIO.read(new File(".idea/graph.png"));

        try {
            mainPanel = new MainPanel(width - 100, height - 150, VPA);
            mainPanel.setBounds(50, 50, width - 100, height - 150);
            mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
            this.add(mainPanel);
            mainPanel.setVisible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*
        optionsPanel = new OptionsPanel(VPA);
        optionsPanel.setBounds(50, height - 260, width - 100, height - 50);
        optionsPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        this.add(optionsPanel);
        optionsPanel.setVisible(true);

        image = drawer.Draw(width - 100, height - 250, VPA);
        */

        optionsPanel = new OptionsPanel(VPA, mainPanel);
        optionsPanel.setBounds(50, height - 100, width - 100, 80);
        this.add(optionsPanel);
        optionsPanel.setVisible(true);
    }
}
