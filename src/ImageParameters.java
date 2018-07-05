import javafx.util.Pair;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import javax.media.jai.PerspectiveTransform;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

class ImageParameters {
    private int numberOfFeatures;
    private int numberOfPixels;
    List<Pair<List<Integer>, List<Integer>>> pixelsFeatures;

    private ImageParameters(int numberOfFeatures, int numberOfPixels, List<Pair<List<Integer>, List<Integer>>> pixelsFeatures) {
        this.numberOfFeatures = numberOfFeatures;
        this.numberOfPixels = numberOfPixels;
        this.pixelsFeatures = pixelsFeatures;
    }

    static List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> findClosestNeighbours(ImageParameters firstImageParameters, ImageParameters secondImageParameters) {
        if (firstImageParameters.numberOfFeatures != secondImageParameters.numberOfFeatures) {
            throw new IllegalArgumentException("Images have different numbers of features");
        }
        boolean wereImagesSwapped = false;
        if (firstImageParameters.numberOfPixels > secondImageParameters.numberOfPixels) {
            ImageParameters imageParameters = firstImageParameters;
            firstImageParameters = secondImageParameters;
            secondImageParameters = imageParameters;
            wereImagesSwapped = true;
        }
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours = new ArrayList<>();
        for (Pair<List<Integer>, List<Integer>> firstImageSinglePixelFeatures : firstImageParameters.pixelsFeatures) {
            double minimumEuclideanDistance = Double.MAX_VALUE;
            List<Pair<List<Integer>, List<Integer>>> closestPixels = new ArrayList<>();
            for (Pair<List<Integer>, List<Integer>> secondImageSinglePixelFeatures : secondImageParameters.pixelsFeatures) {
                double euclideanDistance = 0;
                for (int i = 0; i < firstImageParameters.numberOfFeatures; i++) {
                    euclideanDistance += Math.pow(firstImageSinglePixelFeatures.getValue().get(i) - secondImageSinglePixelFeatures.getValue().get(i), 2);
                }
                euclideanDistance = Math.sqrt(euclideanDistance);
                if (euclideanDistance < minimumEuclideanDistance) {
                    minimumEuclideanDistance = euclideanDistance;
                    closestPixels = new ArrayList<>();
                    closestPixels.add(secondImageSinglePixelFeatures);
                } else if (euclideanDistance == minimumEuclideanDistance) {
                    closestPixels.add(secondImageSinglePixelFeatures);
                }
            }
            Pair<List<Integer>, List<Integer>> closestNeighbour = null;
            minimumEuclideanDistance = Double.MAX_VALUE;
            for (Pair<List<Integer>, List<Integer>> closestPixelsSinglePixelFeatures : closestPixels) {
                double anotherMinimumEuclideanDistance = Double.MAX_VALUE;
                Pair<List<Integer>, List<Integer>> closestPixel = null;
                for (Pair<List<Integer>, List<Integer>> anotherFirstImageSinglePixelFeatures : firstImageParameters.pixelsFeatures) {
                    double euclideanDistance = 0;
                    for (int i = 0; i < firstImageParameters.numberOfFeatures; i++) {
                        euclideanDistance += Math.pow(closestPixelsSinglePixelFeatures.getValue().get(i) - anotherFirstImageSinglePixelFeatures.getValue().get(i), 2);
                    }
                    euclideanDistance = Math.sqrt(euclideanDistance);
                    if (euclideanDistance < anotherMinimumEuclideanDistance) {
                        anotherMinimumEuclideanDistance = euclideanDistance;
                        closestPixel = anotherFirstImageSinglePixelFeatures;
                    }
                }
                assert closestPixel != null;
                if (closestPixel.equals(firstImageSinglePixelFeatures)) {
                    if (anotherMinimumEuclideanDistance < minimumEuclideanDistance) {
                        minimumEuclideanDistance = anotherMinimumEuclideanDistance;
                        closestNeighbour = closestPixelsSinglePixelFeatures;
                    }
                }
            }
            if (closestNeighbour != null) {
                if (wereImagesSwapped) {
                    closestNeighbours.add(new Pair<>(closestNeighbour, firstImageSinglePixelFeatures));
                } else {
                    closestNeighbours.add(new Pair<>(firstImageSinglePixelFeatures, closestNeighbour));
                }
            }
        }
        return closestNeighbours;
    }

    static ImageParameters parseFromHaraffSift(String path) throws FileNotFoundException {
        File file = new File(path);
        Scanner scanner = new Scanner(file);
        int numberOfFeatures = Integer.parseInt(scanner.nextLine());
        int numberOfPixels = Integer.parseInt(scanner.nextLine());
        List<Pair<List<Integer>, List<Integer>>> pixelsFeatures = new ArrayList<>();
        for (int i = 0; i < numberOfPixels; i++) {
            String[] lineFragments = scanner.nextLine().split(" ");
            List<Integer> pixel = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                pixel.add(Math.round(Float.parseFloat(lineFragments[j])));
            }
            List<Integer> features = new ArrayList<>();
            for (int j = 5; j < numberOfFeatures + 5; j++) {
                features.add(Integer.parseInt(lineFragments[j]));
            }
            pixelsFeatures.add(new Pair<>(pixel, features));
        }
        scanner.close();
        return new ImageParameters(numberOfFeatures, numberOfPixels, pixelsFeatures);
    }

    static void printNeighboursToFile(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> neighbours, String path) throws FileNotFoundException {
        PrintWriter printWriter = new PrintWriter(path);
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : neighbours) {
            printWriter.println("(" + closestNeighboursPair.getKey().getKey().get(0) + ", " + closestNeighboursPair.getKey().getKey().get(1) + "), " + "(" + closestNeighboursPair.getValue().getKey().get(0) + ", " + closestNeighboursPair.getValue().getKey().get(1) + ")");
        }
        printWriter.close();
    }

    static List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> findCoherentNeigbours(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, int numberOfPhysicalNeighbours, double coherencyThreshold) {
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> coherentNeighbours = new ArrayList<>();
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
            if (checkIfNeighboursAreCoherent(closestNeighboursPair, closestNeighbours, numberOfPhysicalNeighbours, coherencyThreshold)) {
                coherentNeighbours.add(closestNeighboursPair);
            }
        }
        return coherentNeighbours;
    }

    private static boolean checkIfNeighboursAreCoherent(Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, int numberOfPhysicalNeighbours, double coherencyThreshold) {
        List<Pair<List<Integer>, List<Integer>>> firstPixelPhysicalNeighbours = findPhysicalNeighbours(true, closestNeighbours, closestNeighboursPair.getKey(), numberOfPhysicalNeighbours);
        List<Pair<List<Integer>, List<Integer>>> secondPixelPhysicalNeighbours = findPhysicalNeighbours(false, closestNeighbours, closestNeighboursPair.getValue(), numberOfPhysicalNeighbours);
        double coherency = 0;
        for (Pair<List<Integer>, List<Integer>> firstPixelPhysicalNeighbour : firstPixelPhysicalNeighbours) {
            if (secondPixelPhysicalNeighbours.contains(findFirstImagePixelsClosestNeighbour(firstPixelPhysicalNeighbour, closestNeighbours))) {
                coherency++;
            }
        }
        coherency /= numberOfPhysicalNeighbours;
        return coherency >= coherencyThreshold;
    }

    private static Pair<List<Integer>, List<Integer>> findFirstImagePixelsClosestNeighbour(Pair<List<Integer>, List<Integer>> pixel, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours) {
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
            if (closestNeighboursPair.getKey().equals(pixel)) {
                return closestNeighboursPair.getValue();
            }
        }
        return null;
    }

    private static List<Pair<List<Integer>, List<Integer>>> findPhysicalNeighbours(boolean isPixelFromFirstImage, List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, Pair<List<Integer>, List<Integer>> givenPixel, int numberOfPhysicalNeighbours) {
        if (numberOfPhysicalNeighbours == closestNeighbours.size()) {
            throw new IllegalArgumentException("ImageParameters contains not enough pixels");
        }
        List<Pair<List<Integer>, List<Integer>>> physicalNeighbours = new ArrayList<>();
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
            Pair<List<Integer>, List<Integer>> pixel;
            if (isPixelFromFirstImage) {
                pixel = closestNeighboursPair.getKey();
            } else {
                pixel = closestNeighboursPair.getValue();
            }
            if (!pixel.equals(givenPixel)) {
                if (physicalNeighbours.size() < numberOfPhysicalNeighbours) {
                    physicalNeighbours.add(pixel);
                } else {
                    boolean werePixelsSwapped = false;
                    for (int i = 0; i < numberOfPhysicalNeighbours && !werePixelsSwapped; i++) {
                        if (countPhysicalDistance(givenPixel, pixel) < countPhysicalDistance(givenPixel, physicalNeighbours.get(i))) {
                            physicalNeighbours.set(i, pixel);
                            werePixelsSwapped = true;
                        }
                    }
                }
            }
        }
        return physicalNeighbours;
    }

    private static double countPhysicalDistance(Pair<List<Integer>, List<Integer>> firstPixel, Pair<List<Integer>, List<Integer>> secondPixel) {
        return Math.sqrt(Math.pow(firstPixel.getKey().get(0) - secondPixel.getKey().get(0), 2) + Math.pow(firstPixel.getKey().get(1) - secondPixel.getValue().get(1), 2));
    }

    static Object ransac(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, int numberOfIterations, double maximumError, boolean useAffineTransform, Heuristic heuristic, double r) {
        Object bestModel = null;
        int bestScore = 0;
        for (int i = 0; i < numberOfIterations; i++) {
            Object model = null;
            while (model == null) {
                List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples = chooseSamples(closestNeighbours, useAffineTransform, heuristic, r);
                if (useAffineTransform) {
                    model = calculateAffineTransform(samples);
                } else {
                    model = calculatePerspectiveTransform(samples);
                }
            }
            int score = 0;
            for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
                Point2D point = new Point2D.Double(closestNeighboursPair.getKey().getKey().get(0), closestNeighboursPair.getKey().getKey().get(1));
                Point2D translation;
                Point2D expectedTranslation = new Point2D.Double(closestNeighboursPair.getValue().getKey().get(0), closestNeighboursPair.getValue().getKey().get(1));
                if (useAffineTransform) {
                    translation = ((AffineTransform) model).transform(point, new Point2D.Double());
                } else {
                    translation = ((PerspectiveTransform) model).transform(point, new Point2D.Double());
                }
                assert translation != null;
                double error = translation.distance(expectedTranslation);
                if (error <= maximumError) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestModel = model;
            }
        }
        return bestModel;
    }

    private static Object calculatePerspectiveTransform(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples) {
        if (samples.size() != 4) {
            throw new IllegalArgumentException("Wrong number of samples");
        }
        double[][] arrayFirstMatrix = {{samples.get(0).getKey().getKey().get(0), samples.get(0).getKey().getKey().get(1), 1, 0, 0, 0, -samples.get(0).getValue().getKey().get(0) * samples.get(0).getKey().getKey().get(0), -samples.get(0).getValue().getKey().get(0) * samples.get(0).getKey().getKey().get(1)},
                {samples.get(1).getKey().getKey().get(0), samples.get(1).getKey().getKey().get(1), 1, 0, 0, 0, -samples.get(1).getValue().getKey().get(0) * samples.get(1).getKey().getKey().get(0), -samples.get(1).getValue().getKey().get(0) * samples.get(1).getKey().getKey().get(1)},
                {samples.get(2).getKey().getKey().get(0), samples.get(2).getKey().getKey().get(1), 1, 0, 0, 0, -samples.get(2).getValue().getKey().get(0) * samples.get(2).getKey().getKey().get(0), -samples.get(2).getValue().getKey().get(0) * samples.get(2).getKey().getKey().get(1)},
                {samples.get(3).getKey().getKey().get(0), samples.get(3).getKey().getKey().get(1), 1, 0, 0, 0, -samples.get(3).getValue().getKey().get(0) * samples.get(3).getKey().getKey().get(0), -samples.get(3).getValue().getKey().get(0) * samples.get(3).getKey().getKey().get(1)},
                {0, 0, 0, samples.get(0).getKey().getKey().get(0), samples.get(0).getKey().getKey().get(1), 1, -samples.get(0).getValue().getKey().get(1) * samples.get(0).getKey().getKey().get(0), -samples.get(0).getValue().getKey().get(1) * samples.get(0).getKey().getKey().get(1)},
                {0, 0, 0, samples.get(1).getKey().getKey().get(0), samples.get(1).getKey().getKey().get(1), 1, -samples.get(1).getValue().getKey().get(1) * samples.get(1).getKey().getKey().get(0), -samples.get(1).getValue().getKey().get(1) * samples.get(1).getKey().getKey().get(1)},
                {0, 0, 0, samples.get(2).getKey().getKey().get(0), samples.get(2).getKey().getKey().get(1), 1, -samples.get(2).getValue().getKey().get(1) * samples.get(2).getKey().getKey().get(0), -samples.get(2).getValue().getKey().get(1) * samples.get(2).getKey().getKey().get(1)},
                {0, 0, 0, samples.get(3).getKey().getKey().get(0), samples.get(3).getKey().getKey().get(1), 1, -samples.get(3).getValue().getKey().get(1) * samples.get(3).getKey().getKey().get(0), -samples.get(3).getValue().getKey().get(1) * samples.get(3).getKey().getKey().get(1)}};
        RealMatrix firstMatrix = MatrixUtils.createRealMatrix(arrayFirstMatrix);
        if (new LUDecomposition(firstMatrix).getDeterminant() == 0) {
            return null;
        }
        RealMatrix inverseOfFirstMatrix = new LUDecomposition(firstMatrix).getSolver().getInverse();
        double[][] temporarySecondMatrix = {{samples.get(0).getValue().getKey().get(0)},
                {samples.get(1).getValue().getKey().get(0)},
                {samples.get(2).getValue().getKey().get(0)},
                {samples.get(3).getValue().getKey().get(0)},
                {samples.get(0).getValue().getKey().get(1)},
                {samples.get(1).getValue().getKey().get(1)},
                {samples.get(2).getValue().getKey().get(1)},
                {samples.get(3).getValue().getKey().get(1)}};
        RealMatrix secondMatrix = MatrixUtils.createRealMatrix(temporarySecondMatrix);
        double[] affineTransformParameters = inverseOfFirstMatrix.multiply(secondMatrix).getColumn(0);
        double[][] twoDimentionalAffineTransformParameters = {{affineTransformParameters[0], affineTransformParameters[1], affineTransformParameters[2]},
                {affineTransformParameters[3], affineTransformParameters[4], affineTransformParameters[5]},
                {affineTransformParameters[6], affineTransformParameters[7], 1}};
        return new PerspectiveTransform(twoDimentionalAffineTransformParameters);
    }

    private static List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> chooseSamples(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, boolean useAffineTransform, Heuristic heuristic, double r) {
        if (useAffineTransform) {
            if (closestNeighbours.size() < 3) {
                throw new IllegalArgumentException("Not enough pairs to assemble a sample");
            }
        } else {
            if (closestNeighbours.size() < 4) {
                throw new IllegalArgumentException("Not enough pairs to assemble a sample");
            }
        }
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples = null;
        Random random = new Random();
        int maximumSize = useAffineTransform ? 3 : 4;
        boolean areSamplesCorrect = false;
        while (!areSamplesCorrect) {
            samples = new ArrayList<>();
            while (samples.size() < maximumSize) {
                Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> pair = closestNeighbours.get(random.nextInt(closestNeighbours.size()));
                if (!samples.contains(pair)) {
                    samples.add(pair);
                }
            }
            areSamplesCorrect = heuristic == null || heuristic.checkIfSamplesAreCorrect(samples, r);
        }
        return samples;
    }

    private static AffineTransform calculateAffineTransform(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples) {
        if (samples.size() != 3) {
            throw new IllegalArgumentException("Wrong number of samples");
        }
        double[][] arrayFirstMatrix = {{samples.get(0).getKey().getKey().get(0), samples.get(0).getKey().getKey().get(1), 1, 0, 0, 0},
                {samples.get(1).getKey().getKey().get(0), samples.get(1).getKey().getKey().get(1), 1, 0, 0, 0},
                {samples.get(2).getKey().getKey().get(0), samples.get(2).getKey().getKey().get(1), 1, 0, 0, 0},
                {0, 0, 0, samples.get(0).getKey().getKey().get(0), samples.get(0).getKey().getKey().get(1), 1},
                {0, 0, 0, samples.get(1).getKey().getKey().get(0), samples.get(1).getKey().getKey().get(1), 1},
                {0, 0, 0, samples.get(2).getKey().getKey().get(0), samples.get(2).getKey().getKey().get(1), 1}};
        RealMatrix firstMatrix = MatrixUtils.createRealMatrix(arrayFirstMatrix);
        if (new LUDecomposition(firstMatrix).getDeterminant() == 0) {
            return null;
        }
        RealMatrix inverseOfFirstMatrix = new LUDecomposition(firstMatrix).getSolver().getInverse();
        double[][] temporarySecondMatrix = {{samples.get(0).getValue().getKey().get(0)},
                {samples.get(1).getValue().getKey().get(0)},
                {samples.get(2).getValue().getKey().get(0)},
                {samples.get(0).getValue().getKey().get(1)},
                {samples.get(1).getValue().getKey().get(1)},
                {samples.get(2).getValue().getKey().get(1)}};
        RealMatrix secondMatrix = MatrixUtils.createRealMatrix(temporarySecondMatrix);
        double[] affineTransformParameters = inverseOfFirstMatrix.multiply(secondMatrix).getColumn(0);
        return new AffineTransform(affineTransformParameters[0], affineTransformParameters[3], affineTransformParameters[1], affineTransformParameters[4], affineTransformParameters[2], affineTransformParameters[5]);
    }

    static List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> chooseByTransform(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> closestNeighbours, Object transform, boolean useAffineTransform, double maximumError) {
        List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> choosenNeighbours = new ArrayList<>();
        for (Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>> closestNeighboursPair : closestNeighbours) {
            Point2D point = new Point2D.Double(closestNeighboursPair.getKey().getKey().get(0), closestNeighboursPair.getKey().getKey().get(1));
            Point2D translation;
            Point2D expectedTranslation = new Point2D.Double(closestNeighboursPair.getValue().getKey().get(0), closestNeighboursPair.getValue().getKey().get(1));
            if (useAffineTransform) {
                translation = ((AffineTransform) transform).transform(point, new Point2D.Double());
            } else {
                translation = ((PerspectiveTransform) transform).transform(point, new Point2D.Double());
            }
            assert translation != null;
            double error = translation.distance(expectedTranslation);
            if (error <= maximumError) {
                choosenNeighbours.add(closestNeighboursPair);
            }
        }
        return choosenNeighbours;
    }
}