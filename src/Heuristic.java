import javafx.util.Pair;

import java.util.List;

public interface Heuristic {
    boolean checkIfSamplesAreCorrect(List<Pair<Pair<List<Integer>, List<Integer>>, Pair<List<Integer>, List<Integer>>>> samples, double r);
}