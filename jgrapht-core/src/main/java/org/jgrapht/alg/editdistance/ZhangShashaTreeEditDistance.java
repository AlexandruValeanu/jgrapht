package org.jgrapht.alg.editdistance;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.util.VertexToIntegerMapping;

import java.util.*;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

public class ZhangShashaTreeEditDistance<V, E> {

    /* First input tree */
    private final Graph<V, E> tree1;

    /* Root of the first tree */
    private final V root1;

    /* Second input tree */
    private final Graph<V, E> tree2;

    /* Root of the second tree */
    private final V root2;

    /* Cost of inserting a node */
    private final ToIntFunction<V> insertCost;

    /* Cost of removing a node */
    private final ToIntFunction<V> removeCost;

    /* Cost of updating the label of a node */
    private final ToIntBiFunction<V, V> updateCost;

    private int[][] treedists;
    private List<Operation<V>>[][] operations;

    private VertexToIntegerMapping<V> mapping1;
    private VertexToIntegerMapping<V> mapping2;

    private int cachedDistance;
    private List<Operation<V>> cachedOperations;

    public ZhangShashaTreeEditDistance(Graph<V, E> tree1, V root1, Graph<V, E> tree2, V root2){
        this(tree1, root1, tree2, root2, x -> 1, x -> 1, (x, y) -> {
            if (x.equals(y))
                return 0;
            else
                return 1;
        });
    }

    public ZhangShashaTreeEditDistance(Graph<V, E> tree1, V root1, Graph<V, E> tree2, V root2,
                                       ToIntFunction<V> insertCost, ToIntFunction<V> removeCost,
                                       ToIntBiFunction<V, V> updateCost) {
        this.tree1 = Objects.requireNonNull(tree1, "first input tree cannot be null");
        this.root1 = Objects.requireNonNull(root1, "first input root cannot be null");
        this.tree2 = Objects.requireNonNull(tree2, "second input tree cannot be null");
        this.root2 = Objects.requireNonNull(root2, "second input root cannot be null");
        this.insertCost = Objects.requireNonNull(insertCost, "insertCost function cannot be null");
        this.removeCost = Objects.requireNonNull(removeCost, "removeCost function cannot be null");
        this.updateCost = Objects.requireNonNull(updateCost, "updateCost function cannot be null");
    }

    private void normalizeGraph(){
        mapping1 = Graphs.getVertexToIntegerMapping(tree1);
        mapping2 = Graphs.getVertexToIntegerMapping(tree2);
    }

    @SuppressWarnings("unchecked")
    private void compute(){
        if (operations != null){
            return;
        }

        normalizeGraph();

        AnnotatedTree A = new AnnotatedTree(tree1, root1, mapping1);
        AnnotatedTree B = new AnnotatedTree(tree2, root2, mapping2);

        int size_a = A.postorder.size();
        int size_b = B.postorder.size();

        treedists = new int[size_a][size_b];
        operations = new List[size_a][size_b];

        for (int i = 0; i < size_a; i++) {
            for (int j = 0; j < size_b; j++) {
                operations[i][j] = new ArrayList<>();
            }
        }

        for (V i: A.keyroots) {
            for (V j: B.keyroots) {
                treedist(i, j, A, B);
            }
        }

        cachedDistance = treedists[size_a - 1][size_b - 1];
        cachedOperations = operations[size_a - 1][size_b - 1];
    }

    public int getDistance(){
        compute();
        return cachedDistance;
    }

    public List<Operation<V>> getOperations(){
        compute();
        return Collections.unmodifiableList(cachedOperations);
    }

    private int[] toIntArray(List<V> list, Map<V, Integer> map){
        int[] array = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            array[i] = map.get(list.get(i));
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    void treedist(V vi, V vj, AnnotatedTree A, AnnotatedTree B){
        final int i = A.postOrderIndex.get(vi);
        final int j = B.postOrderIndex.get(vj);

        int[] Al = toIntArray(A.lmlds, A.postOrderIndex);
        int[] Bl = toIntArray(B.lmlds, B.postOrderIndex);
        int[] An = toIntArray(A.postorder, A.postOrderIndex);
        int[] Bn = toIntArray(B.postorder, B.postOrderIndex);

        final int m = i - Al[i] + 2;
        final int n = j - Bl[j] + 2;

        int[][] fd = new int[m][n];
        List<Operation<V>>[][] partial_ops = new List[m][n];

        for (int k = 0; k < m; k++) {
            for (int l = 0; l < n; l++) {
                partial_ops[k][l] = new ArrayList<>();
            }
        }

        final int ioff = Al[i] - 1;
        final int joff = Bl[j] - 1;

        for (int x = 1; x < m; x++) { // δ(l(i1)..i, θ) = δ(l(1i)..1-1, θ) + γ(v → λ)
            int node = An[x + ioff];
            fd[x][0] = fd[x - 1][0] + removeCost.applyAsInt(A.postorder.get(node));
            Operation<V> op = new Operation<>(OperationType.REMOVE, A.postorder.get(node), null);
            partial_ops[x][0] = new ArrayList<>(partial_ops[x - 1][0]);
            partial_ops[x][0].add(op);
        }

        for (int y = 1; y < n; y++) { // δ(θ, l(j1)..j) = δ(θ, l(j1)..j-1) + γ(λ → w)
            int node = Bn[y + joff];
            fd[0][y] = fd[0][y - 1] + insertCost.applyAsInt(B.postorder.get(node));
            Operation<V> op = new Operation<>(OperationType.INSERT, null, B.postorder.get(node));
            partial_ops[0][y] = new ArrayList<>(partial_ops[0][y - 1]);
            partial_ops[0][y].add(op);
        }

        for (int x = 1; x < m; x++) {
            for (int y = 1; y < n; y++) {
                // x+ioff in the fd table corresponds to the same node as x in
                // the treedists table (same for y and y+joff)
                int node1 = An[x + ioff];
                int node2 = Bn[y + joff];
                // only need to check if x is an ancestor of i
                // and y is an ancestor of j
                if (Al[i] == Al[x + ioff] && Bl[j] == Bl[y + joff]){
                     //                   +-
                     //                   | δ(l(i1)..i-1, l(j1)..j) + γ(v → λ)
                     // δ(F1 , F2 ) = min-+ δ(l(i1)..i , l(j1)..j-1) + γ(λ → w)
                     //                   | δ(l(i1)..i-1, l(j1)..j-1) + γ(v → w)
                     //                   +-
                    int cost1 = fd[x - 1][y] + removeCost.applyAsInt(A.postorder.get(node1));
                    int cost2 = fd[x][y - 1] + insertCost.applyAsInt(B.postorder.get(node2));
                    int cost3 = fd[x - 1][y - 1] + updateCost.applyAsInt(A.postorder.get(node1), B.postorder.get(node2));

                    int minim = Math.min(cost1, Math.min(cost2, cost3));
                    fd[x][y] = minim;

                    if (cost1 == minim){
                        Operation<V> op = new Operation<>(OperationType.REMOVE, A.postorder.get(node1), null);
                        partial_ops[x][y] = new ArrayList<>(partial_ops[x - 1][y]);
                        partial_ops[x][y].add(op);
                    }
                    else if (cost2 == minim){
                        Operation<V> op = new Operation<>(OperationType.INSERT, null, B.postorder.get(node2));
                        partial_ops[x][y] = new ArrayList<>(partial_ops[x][y - 1]);
                        partial_ops[x][y].add(op);
                    }
                    else{
                        Operation<V> op;

                        if (fd[x][y] == fd[x - 1][y - 1])
                            op = new Operation<>(OperationType.MATCH, A.postorder.get(node1), B.postorder.get(node2));
                        else
                            op = new Operation<>(OperationType.UPDATE, A.postorder.get(node1), B.postorder.get(node2));

                        partial_ops[x][y] = new ArrayList<>(partial_ops[x - 1][y - 1]);
                        partial_ops[x][y].add(op);
                    }

                    operations[x + ioff][y + joff] = partial_ops[x][y];
                    treedists[x + ioff][y + joff] = fd[x][y];
                }
                else{
                    //                   +-
                    //                   | δ(l(i1)..i-1, l(j1)..j) + γ(v → λ)
                    // δ(F1 , F2 ) = min-+ δ(l(i1)..i , l(j1)..j-1) + γ(λ → w)
                    //                   | δ(l(i1)..l(i)-1, l(j1)..l(j)-1)
                    //                   |                     + treedist(i1,j1)
                    //                   +-
                    int p = Al[x + ioff] - 1 - ioff;
                    int q = Bl[y + joff] - 1 - joff;

                    int cost1 = fd[x - 1][y] + removeCost.applyAsInt(A.postorder.get(node1));
                    int cost2 = fd[x][y - 1] + insertCost.applyAsInt(B.postorder.get(node2));
                    int cost3 = fd[p][q] + treedists[x + ioff][y + joff];

                    int minim = Math.min(cost1, Math.min(cost2, cost3));
                    fd[x][y] = minim;

                    if (cost1 == minim){
                        Operation<V> op = new Operation<>(OperationType.REMOVE, A.postorder.get(node1), null);
                        partial_ops[x][y] = new ArrayList<>(partial_ops[x - 1][y]);
                        partial_ops[x][y].add(op);
                    }
                    else if (cost2 == minim){
                        Operation<V> op = new Operation<>(OperationType.INSERT, null, B.postorder.get(node2));
                        partial_ops[x][y] = new ArrayList<>(partial_ops[x][y - 1]);
                        partial_ops[x][y].add(op);
                    }
                    else{
                        partial_ops[x][y] = new ArrayList<>(partial_ops[p][q]);
                        partial_ops[x][y].addAll(operations[x + ioff][y + joff]);
                    }
                }
            }
        }
    }

    private class AnnotatedTree {
        // the postorder of the input tree
        List<V> postorder;

        // lmld(v) = leftmost leaf descendant of the subtree rooted at v
        Map<V, V> lmld;

        // keyroots = {k | there exists no k’ > k such that lmld(k) = lmld(k’)}.
        List<V> keyroots;

        // postOrderIndex[v] = the position of v in the postorder
        Map<V, Integer> postOrderIndex;

        // lmlds[i] = lmlds[postorder[i]]
        List<V> lmlds;

        AnnotatedTree(Graph<V, E> tree, V root, VertexToIntegerMapping<V> mapping) {
            Map<V, ArrayDeque<V>> ancestors = new HashMap<>();
            ArrayDeque<V> stack = new ArrayDeque<>();
            ArrayDeque<V> pstack = new ArrayDeque<>();

            ancestors.put(root, new ArrayDeque<>());
            stack.addLast(root);

            while (!stack.isEmpty()) {
                V node = stack.pollLast();
                ArrayDeque<V> ancs = ancestors.get(node);

                for (E edge : tree.edgesOf(node)) {
                    V son = Graphs.getOppositeVertex(tree, edge, node);

                    if (ancs.isEmpty() || !son.equals(ancs.getFirst())) {
                        ArrayDeque<V> newAncs = new ArrayDeque<>(ancs);
                        newAncs.addFirst(node);
                        ancestors.put(son, newAncs);
                        stack.addLast(son);
                    }
                }

                pstack.addLast(node);
            }

            lmld = new HashMap<>(tree.vertexSet().size());
            postorder = new ArrayList<>(tree.vertexSet().size());

            while (!pstack.isEmpty()){
                V node = pstack.pollLast();
                this.postorder.add(node);
                V lmd = node;

                for (E edge : tree.edgesOf(node)) {
                    V son = Graphs.getOppositeVertex(tree, edge, node);

                    // lmld[son] has already been computed
                    if (lmld.containsKey(son)) {
                        lmd = lmld.get(son);
                        break;
                    }
                }

                lmld.put(node, lmd);
            }

            keyroots = new ArrayList<>();
            postOrderIndex = new HashMap<>(tree.vertexSet().size());
            lmlds = new ArrayList<>(tree.vertexSet().size());

            for (int i = 0; i < postorder.size(); i++){
                V node = postorder.get(i);
                ArrayDeque<V> ancs = ancestors.get(node);

                // either node is the root of the tree or it has a left sibling
                if (ancs.isEmpty() || !lmld.get(node).equals(lmld.get(ancs.getFirst()))){
                    keyroots.add(node);
                }

                postOrderIndex.put(node, i);
                lmlds.add(lmld.get(node));
            }
        }
    }
}
