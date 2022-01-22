package org.moeaframework.core.operator.subset;

import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.Variable;
import org.moeaframework.core.Variation;
import org.moeaframework.core.variable.Subset;
import org.workflowsim.utils.CommonUtils;

import java.util.*;

public class WSSX implements Variation {
    /**
     * The probability of applying this operator.
     */
    private final double probability;

    /**
     * Constructs a SSX operator.
     *
     * @param probability the probability of applying this operator
     */
    public WSSX(double probability) {
        super();
        this.probability = probability;
    }

    /**
     * Evolves the specified variables using the SSX operator.
     *
     * @param s1 the first subset
     * @param s2 the second subset
     */
    public static void evolve(Subset s1, Subset s2) {
        Set<Integer> s1set = s1.getSet();
        Set<Integer> s2set = s2.getSet();

        Set<Integer> intersection = new HashSet<Integer>(s1set);
        intersection.retainAll(s2set);

        s1set.removeAll(intersection);
        s2set.removeAll(intersection);

        while (!s1set.isEmpty() && !s2set.isEmpty()) {
            int s1member = PRNG.nextItem(s1set);
            int s2member = PRNG.nextItem(s2set);

            if (PRNG.nextBoolean()) {
                s1.replace(s1member, s2member);
                s2.replace(s2member, s1member);
            }

            s1set.remove(s1member);
            s2set.remove(s2member);
        }
    }

    @Override
    public Solution[] evolve(Solution[] parents) {
        Solution result1 = parents[0].copy();
        Solution result2 = parents[1].copy();
        Map<Integer, List<Integer>> vMap1 = getResultMap(result1);
        Map<Integer, List<Integer>> vMap2 = getResultMap(result2);
        Set<Integer> handleNum1 = new HashSet<>();
        Set<Integer> handleNum2 = new HashSet<>();
        int[] randArray = CommonUtils.getRandomArray(result1.getNumberOfVariables());

        for (int i = 0; i < result1.getNumberOfVariables(); i++) {
            int index = randArray[i];
            Variable variable1 = result1.getVariable(index);
            Variable variable2 = result2.getVariable(index);

            if ((PRNG.nextDouble() <= probability)
                    && (variable1 instanceof Subset)
                    && (variable2 instanceof Subset)
                    && !(((Subset)variable1).equals((Subset) variable2))
                    && !handleNum1.contains(index)
                    && !handleNum2.contains(index)) {
                List<Integer> vList1 = vMap1.get((int) ((Subset)variable1).getSet().toArray()[0]);
                evolve((Subset) variable1, result2, vList1, index);
                handleNum2.addAll(vList1);
                List<Integer> vList2 = vMap2.get((int) ((Subset)variable2).getSet().toArray()[0]);
                evolve((Subset) variable2, result1, vList2, index);
                handleNum1.addAll(vList2);
                evolve((Subset)variable1, (Subset)variable2);
            }
        }

        return new Solution[] { result1, result2 };
    }

    private void evolve(Subset variable1, Solution result2, List<Integer> vList1, int index) {
        if(vList1 == null)
            return;
        for(int i : vList1){
            if(i != index){
                Subset var = (Subset)result2.getVariable(i);
                var.clear();
                var.add((Integer) variable1.getSet().toArray()[0]);
            }
        }
    }

    private Map<Integer, List<Integer>> getResultMap(Solution result) {
        Map<Integer, List<Integer>> vMap = new HashMap<>();

        for (int i = 0; i < result.getNumberOfVariables(); i++) {
            Variable variable1 = result.getVariable(i);
            int r = (Integer) ((Subset)variable1).getSet().toArray()[0];
            if(vMap.containsKey(r)){
                vMap.get(r).add(i);
            }else{
                List<Integer> v = new ArrayList<>();
                v.add(i);
                vMap.put(r, v);
            }
        }
        return vMap;
    }

    @Override
    public int getArity() {
        return 2;
    }
}
