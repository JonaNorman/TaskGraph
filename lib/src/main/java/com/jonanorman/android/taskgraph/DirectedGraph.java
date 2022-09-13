package com.jonanorman.android.taskgraph;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


class DirectedGraph<V> implements Cloneable {

    public static class Vertex<V> implements Cloneable {

        private V value;

        public Vertex(V v) {
            this.value = v;
        }

        public V getValue() {
            return value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vertex)) return false;
            Vertex<?> vertex = (Vertex<?>) o;
            return Objects.equals(value, vertex.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Vertex " + value;
        }


        @Override
        protected Object clone() throws CloneNotSupportedException {
            Vertex clone = (Vertex) super.clone();
            clone.value = value;
            return clone;
        }
    }

    public static class Edge<V> implements Cloneable {
        private Vertex<V> from;
        private Vertex<V> to;

        public Edge(Vertex<V> from, Vertex<V> to) {
            this.from = from;
            this.to = to;
        }

        public Vertex<V> getFrom() {
            return from;
        }

        public Vertex<V> getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;
            Edge<?> edge = (Edge<?>) o;
            return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }

        @Override
        public String toString() {
            return "from=" + from + ", to=" + to;
        }


        @Override
        protected Object clone() throws CloneNotSupportedException {
            Edge clone = (Edge) super.clone();
            clone.from = (Vertex) from.clone();
            clone.to = (Vertex) to.clone();
            return clone;
        }
    }


    private static class Node<V> implements Cloneable {

        private Vertex<V> vertex;

        private Set<Edge<V>> incomingEdges;

        private Set<Edge<V>> outgoingEdges;


        public Node(Vertex<V> vertex) {
            this.vertex = vertex;
            this.incomingEdges = new HashSet<>();
            this.outgoingEdges = new HashSet<>();
        }


        public int getInDegree() {
            return incomingEdges.size();
        }

        public int getOutDegree() {
            return outgoingEdges.size();
        }

        private void addIncomingEdge(Edge<V> edge) {
            if (!edge.getTo().equals(vertex)) {
                throw new IllegalArgumentException("incoming edge is not to " + vertex);
            }
            if (!incomingEdges.contains(edge)) {
                incomingEdges.add(edge);
            }
        }

        private void addOutgoingEdge(Edge<V> edge) {
            if (!edge.getFrom().equals(vertex)) {
                throw new IllegalArgumentException("outgoing edge is not from " + vertex);
            }
            if (!outgoingEdges.contains(edge)) {
                outgoingEdges.add(edge);
            }
        }

        private void removeIncomingEdge(Edge<V> edge) {
            if (!edge.getTo().equals(vertex)) {
                throw new IllegalArgumentException("incoming edge  is not to " + vertex);
            }
            if (incomingEdges.contains(edge)) {
                incomingEdges.remove(edge);
            }

        }


        private void removeOutgoingEdge(Edge<V> edge) {
            if (!edge.getFrom().equals(vertex)) {
                throw new IllegalArgumentException("outgoing edge  is not from " + vertex);
            }
            if (outgoingEdges.contains(edge)) {
                outgoingEdges.remove(edge);
            }

        }

        public boolean containsIncomingEdge(Edge<V> edge) {
            if (!edge.getTo().equals(vertex)) {
                return false;
            }
            return incomingEdges.contains(edge);
        }

        public boolean containsOutgoingEdge(Edge<V> edge) {
            if (!edge.getFrom().equals(vertex)) {
                return false;
            }
            return outgoingEdges.contains(edge);
        }

        @Override
        public String toString() {
            return "Node " + vertex +
                    ", incomingEdges=" + incomingEdges +
                    ", outgoingEdges=" + outgoingEdges;
        }


        @Override
        protected Object clone() throws CloneNotSupportedException {
            Node clone = (Node) super.clone();
            clone.vertex = (Vertex) vertex.clone();
            clone.incomingEdges = new HashSet();
            clone.outgoingEdges = new HashSet();
            for (Edge<V> incomingEdge : incomingEdges) {
                clone.incomingEdges.add(incomingEdge.clone());
            }
            for (Edge<V> outgoingEdge : outgoingEdges) {
                clone.outgoingEdges.add(outgoingEdge.clone());
            }
            return clone;
        }
    }

    private Map<Vertex<V>, Node<V>> graphMap;


    public DirectedGraph() {
        graphMap = new HashMap<>();
    }


    public void addVertex(Vertex<V> vertex) {
        Node<V> node = graphMap.get(vertex);
        if (node == null) {
            node = new Node<>(vertex);
            graphMap.put(vertex, node);
        }
    }

    public boolean containsVertex(Vertex<V> vertex) {
        return graphMap.containsKey(vertex);
    }

    public void removeVertex(Vertex<V> vertex) {
        Node<V> node = graphMap.remove(vertex);
        if (node == null) {
            return;
        }
        Iterator<Edge<V>> incomingEdgeIterator = node.incomingEdges.iterator();
        while (incomingEdgeIterator.hasNext()) {
            Edge<V> incomingEdge = incomingEdgeIterator.next();
            incomingEdgeIterator.remove();
            Vertex<V> from = incomingEdge.getFrom();
            Node<V> fromNode = graphMap.get(from);
            fromNode.removeOutgoingEdge(incomingEdge);
        }
        Iterator<Edge<V>> outgoingEdgeIterator = node.outgoingEdges.iterator();
        while (outgoingEdgeIterator.hasNext()) {
            Edge<V> outgoingEdge = outgoingEdgeIterator.next();
            outgoingEdgeIterator.remove();
            Vertex<V> to = outgoingEdge.getTo();
            Node<V> toNode = graphMap.get(to);
            toNode.removeIncomingEdge(outgoingEdge);
        }
    }


    public void addEdge(Edge<V> edge) {
        Vertex<V> from = edge.getFrom();
        Node<V> fromNode = graphMap.get(from);
        if (fromNode == null) {
            fromNode = new Node<>(from);
            graphMap.put(from, fromNode);
        }
        fromNode.addOutgoingEdge(edge);

        Vertex<V> to = edge.getTo();
        Node<V> toNode = graphMap.get(to);
        if (toNode == null) {
            toNode = new Node<>(to);
            graphMap.put(to, toNode);
        }
        toNode.addIncomingEdge(edge);
    }

    public void removeEdge(Edge<V> edge) {
        Vertex<V> from = edge.getFrom();
        Node<V> fromNode = graphMap.get(from);
        if (fromNode == null) {
            throw new NullPointerException("remove edge fail, not exist from " + from);
        }
        fromNode.removeOutgoingEdge(edge);
        Vertex<V> to = edge.getTo();
        Node<V> toNode = graphMap.get(to);
        if (toNode == null) {
            throw new NullPointerException("remove edge fail,  not exist to " + to);
        }
        toNode.removeIncomingEdge(edge);
    }

    public boolean containsEdge(Edge<V> edge) {
        Node<V> fromNode = graphMap.get(edge.getFrom());
        Node<V> toNode = graphMap.get(edge.getTo());
        if (fromNode == null || toNode == null) {
            return false;
        }
        return fromNode.containsOutgoingEdge(edge) && toNode.containsIncomingEdge(edge);
    }


    public Set<Vertex<V>> getVertexSet() {
        return graphMap.keySet();
    }


    public int getInDegree(Vertex<V> vertex) {
        Node<V> node = graphMap.get(vertex);
        if (node == null) {
            return 0;
        }
        return node.getInDegree();
    }

    public int getOutDegree(Vertex<V> vertex) {
        Node<V> node = graphMap.get(vertex);
        if (node == null) {
            return 0;
        }
        return node.getOutDegree();
    }

    public Set<Edge<V>> getIncomingEdgeSet(Vertex<V> vertex) {
        Node<V> node = graphMap.get(vertex);
        if (node == null) {
            throw new IllegalArgumentException("not exist IncomingEdge " + vertex);
        }
        return node.incomingEdges;
    }

    public Set<Edge<V>> getOutgoingEdgeSet(Vertex<V> vertex) {
        Node<V> node = graphMap.get(vertex);
        if (node == null) {
            throw new IllegalArgumentException("not exist OutgoingEdge " + vertex);
        }
        return node.outgoingEdges;
    }


    public boolean hasCycle() {
        DirectedGraph<V> graph;
        try {
            graph = (DirectedGraph<V>) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
        Set<Vertex<V>> vertexSet = graph.getVertexSet();
        Queue<Vertex<V>> queue = new LinkedList<>();
        int vertexSize = vertexSet.size();
        int bfsSize = 0;
        for (Vertex vertex : vertexSet) {
            if (getInDegree(vertex) == 0) {
                queue.offer(vertex);
            }
        }
        while (!queue.isEmpty()) {
            Vertex<V> vertex = queue.poll();
            bfsSize++;
            Set<Edge<V>> outgoingEdgeSet = getOutgoingEdgeSet(vertex);
            graph.removeVertex(vertex);
            for (Edge<V> edge : outgoingEdgeSet) {
                if (graph.getInDegree(edge.getTo()) == 0) {
                    queue.offer(edge.getTo());
                }
            }
        }
        return bfsSize != vertexSize;
    }


    @Override
    public String toString() {
        return "DirectedGraph " + graphMap;
    }


    @Override
    protected Object clone() throws CloneNotSupportedException {
        DirectedGraph clone = (DirectedGraph) super.clone();
        clone.graphMap = new HashMap();
        for (Vertex<V> vertex : graphMap.keySet()) {
            Node<V> node = graphMap.get(vertex);
            clone.graphMap.put(vertex.clone(), node.clone());
        }
        return clone;
    }
}
