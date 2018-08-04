package org.jgrapht.alg.editdistance;

public class Operation<V> {

    final OperationType op;
    final V firstNode;
    final V secondNode;

    public Operation(OperationType op, V firstNode, V secondNode){
        this.op = op;
        this.firstNode = firstNode;
        this.secondNode = secondNode;
    }

    @Override
    public String toString() {
        switch (op){
            case REMOVE: return "<OperationType Remove: " + firstNode + ">";
            case INSERT: return "<OperationType Insert: " + secondNode + ">";
            case UPDATE: return "<OperationType Update: " + firstNode + " to " + secondNode + ">";
            case MATCH:  return "<OperationType Match: " + firstNode + " to " + secondNode + ">";
        }

        return "error";
    }
}
