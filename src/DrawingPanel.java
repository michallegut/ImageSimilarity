import javafx.util.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

class DrawingPanel extends JPanel {
    private static final long serialVersionUID = -4559408638276405147L;

    private Image image;
    private ImageParameters firstImageParameters;
    private ImageParameters secondImageParameters;
    private List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours;
    private List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> coherentNeighbours;
    private int firstImageWidth;
    private double ratio;

    private DrawingPanel(Image image, ImageParameters firstImageParameters, ImageParameters secondImageParameters, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> coherentNeighbours, int firstImageWidth, double ratio) {
        this.image = image;
        this.firstImageParameters = firstImageParameters;
        this.secondImageParameters = secondImageParameters;
        this.closestNeighbours = closestNeighbours;
        this.coherentNeighbours = coherentNeighbours;
        this.firstImageWidth = firstImageWidth;
        this.ratio = ratio;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.setColor(Color.red);
        for (Pair<List<Integer>, List<Integer>> firstImagePixel : firstImageParameters.pixelsFeatures) {
            graphics2D.drawOval((int) Math.round(firstImagePixel.getKey().get(0) * ratio), (int) Math.round(firstImagePixel.getKey().get(1) * ratio), 1, 1);
        }
        for (Pair<List<Integer>, List<Integer>> secondImagePixel : secondImageParameters.pixelsFeatures) {
            graphics2D.drawOval((int) Math.round((secondImagePixel.getKey().get(0) + firstImageWidth) * ratio), (int) Math.round(secondImagePixel.getKey().get(1) * ratio), 1, 1);
        }
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
            graphics2D.setColor(Color.blue);
            graphics2D.drawOval((int) Math.round(closestNeighboursPair.getKey().getKey().get(0) * ratio), (int) Math.round(closestNeighboursPair.getKey().getKey().get(1) * ratio), 1, 1);
            graphics2D.drawOval((int) Math.round((closestNeighboursPair.getValue().getKey().get(0) + firstImageWidth) * ratio), (int) Math.round(closestNeighboursPair.getValue().getKey().get(1) * ratio), 1, 1);
//            Image is not readable after drawing lines between closest neighbours
//            graphics2D.setColor(Color.green);
//            graphics2D.drawLine((int) Math.round(closestNeighboursPair.getKey().getKey().get(0) * ratio), (int) Math.round(closestNeighboursPair.getKey().getKey().get(1)), (int) Math.round((closestNeighboursPair.getValue().getKey().get(0) + firstImageWidth) * ratio), (int) Math.round(closestNeighboursPair.getValue().getKey().get(1) * ratio));
        }
        graphics2D.setColor(Color.yellow);
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> coherentNeighboursPair : coherentNeighbours) {
            graphics2D.drawLine((int) Math.round(coherentNeighboursPair.getKey().getKey().get(0) * ratio), (int) Math.round(coherentNeighboursPair.getKey().getKey().get(1) * ratio), (int) Math.round((coherentNeighboursPair.getValue().getKey().get(0) + firstImageWidth) * ratio), (int) Math.round(coherentNeighboursPair.getValue().getKey().get(1) * ratio));
        }
    }

    static void drawAndSave(String firstImagePath, String secondImagePath, ImageParameters firstImageParameters, ImageParameters secondImageParameters, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> coherentNeighbours, String path) throws IOException {
        BufferedImage firstImage = ImageIO.read(new File(firstImagePath));
        BufferedImage secondImage = ImageIO.read(new File(secondImagePath));
        BufferedImage bufferedImage = joinImages(firstImage, secondImage);
        Pair<Image, Double> imageAndRatio = scaleImageToFitScreen(bufferedImage);
        Image image = imageAndRatio.getKey();
        bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        JFrame jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JLayeredPane jLayeredPane = new JLayeredPane();
        JPanel drawingPanel = new DrawingPanel(image, firstImageParameters, secondImageParameters, closestNeighbours, coherentNeighbours, firstImage.getWidth(), imageAndRatio.getValue());
        drawingPanel.paint(graphics2D);
        ImageIO.write(bufferedImage, "png", new File(path));
        jFrame.setPreferredSize(new Dimension(bufferedImage.getWidth(), bufferedImage.getHeight()));
        jFrame.setLayout(new BorderLayout());
        jFrame.add(jLayeredPane, BorderLayout.CENTER);
        jLayeredPane.setBounds(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        drawingPanel.setBounds(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        drawingPanel.setOpaque(true);
        jLayeredPane.add(drawingPanel, 0, 0);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private static Pair<Image, Double> scaleImageToFitScreen(Image joinedImage) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double screenWidth = screenSize.getWidth();
        double screenHeight = screenSize.getHeight();
        double imageWidth = joinedImage.getWidth(null);
        double imageHeight = joinedImage.getHeight(null);
        double widthRatio = screenWidth / imageWidth;
        double heightRatio = screenHeight / imageHeight;
        if (widthRatio < 1 && heightRatio < 1) {
            if (widthRatio < heightRatio) {
                return new Pair<>(joinedImage.getScaledInstance((int) Math.round(imageWidth * widthRatio), (int) Math.round(imageHeight * widthRatio), Image.SCALE_SMOOTH), widthRatio);
            } else {
                return new Pair<>(joinedImage.getScaledInstance((int) Math.round(imageWidth * heightRatio), (int) Math.round(imageHeight * heightRatio), Image.SCALE_SMOOTH), heightRatio);
            }
        } else {
            if (widthRatio < 1) {
                return new Pair<>(joinedImage.getScaledInstance((int) Math.round(imageWidth * widthRatio), (int) Math.round(imageHeight * widthRatio), Image.SCALE_SMOOTH), widthRatio);
            }
            if (heightRatio < 1) {
                return new Pair<>(joinedImage.getScaledInstance((int) Math.round(imageWidth * heightRatio), (int) Math.round(imageHeight * heightRatio), Image.SCALE_SMOOTH), heightRatio);
            }
        }
        return new Pair<>(joinedImage, 1.0);
    }

    private static BufferedImage joinImages(BufferedImage firstImage, BufferedImage secondImage) throws IOException {
        int width = firstImage.getWidth() + secondImage.getWidth();
        int height = Math.max(firstImage.getHeight(), secondImage.getHeight());
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(firstImage, null, 0, 0);
        g2.drawImage(secondImage, null, firstImage.getWidth(), 0);
        g2.dispose();
        return newImage;
    }
}