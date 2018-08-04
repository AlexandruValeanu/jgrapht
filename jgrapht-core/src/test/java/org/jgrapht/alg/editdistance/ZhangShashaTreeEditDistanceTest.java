package org.jgrapht.alg.editdistance;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

public class ZhangShashaTreeEditDistanceTest {

    @Test
    public void test(){
        Graph<String, DefaultEdge> tree1 = new SimpleGraph<>(DefaultEdge.class);

        for (char c = 'a'; c <= 'f'; c++){
            tree1.addVertex(String.valueOf(c));
        }

        tree1.addEdge("f", "d");
        tree1.addEdge("f", "e");
        tree1.addEdge("d", "a");
        tree1.addEdge("d", "c");
        tree1.addEdge("c", "b");

        Graph<String, DefaultEdge> tree2 = new SimpleGraph<>(DefaultEdge.class);

        for (char c = 'a'; c <= 'f'; c++){
            tree2.addVertex(String.valueOf(c));
        }

        tree2.addEdge("f", "c");
        tree2.addEdge("f", "e");
        tree2.addEdge("d", "a");
        tree2.addEdge("d", "b");
        tree2.addEdge("c", "d");

        ZhangShashaTreeEditDistance<String, DefaultEdge> distance =
                new ZhangShashaTreeEditDistance<>(tree1, "f", tree2, "f");

        System.out.println(distance.getDistance());
        System.out.println(distance.getOperations());
    }

}