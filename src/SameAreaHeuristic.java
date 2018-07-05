import javafx.util.Pair;

import java.util.List;

public class SameAreaHeuristic implements Heuristic {
    @Override
    public boolean checkIfSamplesAreCorrect(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples, double r) {
        double firstPair = Math.sqrt(Math.pow(samples.get(0).getKey().getKey().get(0) - samples.get(0).getValue().getKey().get(0), 2) + Math.pow(samples.get(0).getKey().getKey().get(1) - samples.get(0).getKey().getValue().get(1), 2));
        double secondPair = Math.sqrt(Math.pow(samples.get(1).getKey().getKey().get(0) - samples.get(1).getValue().getKey().get(0), 2) + Math.pow(samples.get(1).getKey().getKey().get(1) - samples.get(1).getKey().getValue().get(1), 2));
        double thirdPair = Math.sqrt(Math.pow(samples.get(2).getKey().getKey().get(0) - samples.get(2).getValue().getKey().get(0), 2) + Math.pow(samples.get(2).getKey().getKey().get(1) - samples.get(2).getKey().getValue().get(1), 2));
        double fourthPair;
        if (samples.size() > 3) {
            fourthPair = Math.sqrt(Math.pow(samples.get(3).getKey().getKey().get(0) - samples.get(3).getValue().getKey().get(0), 2) + Math.pow(samples.get(3).getKey().getKey().get(1) - samples.get(3).getKey().getValue().get(1), 2));
        } else {
            fourthPair = r - 1;
        }
        return firstPair < r && secondPair < r && thirdPair < r && fourthPair < r;
    }
}