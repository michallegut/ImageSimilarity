import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedImage firstImage = ImageIO.read(new File(args[0]));
        double biggerDimension = firstImage.getHeight() > firstImage.getHeight() ? firstImage.getHeight() : firstImage.getWidth();
        double error = biggerDimension * Double.parseDouble(args[7]);
        double r = biggerDimension * Double.parseDouble(args[8]);

        ImageParameters firstImageParameters = ImageParameters.parseFromHaraffSift(args[2]);
        ImageParameters secondImageParameters = ImageParameters.parseFromHaraffSift(args[3]);

        long startTime = System.currentTimeMillis();
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours = ImageParameters.findClosestNeighbours(firstImageParameters, secondImageParameters);
        System.out.println("Closest neighbours time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(closestNeighbours, "results/closestneighbours.txt");

        startTime = System.currentTimeMillis();
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> coherentNeighbours = ImageParameters.findCoherentNeigbours(closestNeighbours, Integer.parseInt(args[4]), Double.parseDouble(args[5]));
        System.out.println("Coherent neighbours time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(coherentNeighbours, "results/coherentneighbours.txt");
        DrawingPanel.drawAndSave(args[0], args[1], firstImageParameters, secondImageParameters, closestNeighbours, coherentNeighbours, "results/coherentneighbours.png");

        startTime = System.currentTimeMillis();
        Object affineTransform = ImageParameters.ransac(closestNeighbours, Integer.parseInt(args[6]), error, true, null, r);
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> neighboursChoosenByAffineTransform = ImageParameters.chooseByTransform(closestNeighbours, affineTransform, true, error);
        System.out.println("RANSAC with affinite transform and without heuristic time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(neighboursChoosenByAffineTransform, "results/neighbourschoosenbyaffinetransformwithoutheuristic.txt");
        DrawingPanel.drawAndSave(args[0], args[1], firstImageParameters, secondImageParameters, closestNeighbours, neighboursChoosenByAffineTransform, "results/neighbourschoose    nbyaffinetransformwithoutheuristic.png");

        startTime = System.currentTimeMillis();
        affineTransform = ImageParameters.ransac(closestNeighbours, Integer.parseInt(args[6]), error, true, new SameAreaHeuristic(), r);
        neighboursChoosenByAffineTransform = ImageParameters.chooseByTransform(closestNeighbours, affineTransform, true, error);
        System.out.println("RANSAC with affinite transform and with heuristic time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(neighboursChoosenByAffineTransform, "results/neighbourschoosenbyaffinetransformwithheuristic.txt");
        DrawingPanel.drawAndSave(args[0], args[1], firstImageParameters, secondImageParameters, closestNeighbours, neighboursChoosenByAffineTransform, "results/neighbourschoosenbyaffinetransformwithheuristic.png");

        startTime = System.currentTimeMillis();
        Object perspectiveTransform = ImageParameters.ransac(closestNeighbours, Integer.parseInt(args[6]), error, false, null, r);
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> neighboursChoosenByPerspectiveTransform = ImageParameters.chooseByTransform(closestNeighbours, perspectiveTransform, false, error);
        System.out.println("RANSAC with perspective transform and without heuristic time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(neighboursChoosenByPerspectiveTransform, "results/neighbourschoosenbyperspectivetransformwithoutheuristic.txt");
        DrawingPanel.drawAndSave(args[0], args[1], firstImageParameters, secondImageParameters, closestNeighbours, neighboursChoosenByPerspectiveTransform, "results/neighbourschoosenbyperspectivetransformwithoutheuristic.png");

        startTime = System.currentTimeMillis();
        perspectiveTransform = ImageParameters.ransac(closestNeighbours, Integer.parseInt(args[6]), error, false, new SameAreaHeuristic(), r);
        neighboursChoosenByPerspectiveTransform = ImageParameters.chooseByTransform(closestNeighbours, perspectiveTransform, false, error);
        System.out.println("RANSAC with perspective transform and with heuristic time: " + (System.currentTimeMillis() - startTime) + " ms");
        ImageParameters.printNeighboursToFile(neighboursChoosenByPerspectiveTransform, "results/neighbourschoosenbyperspectivetransformwithheuristic.txt");
        DrawingPanel.drawAndSave(args[0], args[1], firstImageParameters, secondImageParameters, closestNeighbours, neighboursChoosenByPerspectiveTransform, "results/neighbourschoosenbyperspectivetransformwithheuristic.png");
    }
}