package com.mongraphe.graphui.rendering;

import java.util.ArrayList;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;

public class GraphScene {
    ArrayList<Vertex> vertices = new ArrayList<>();
    ArrayList<Edge> edges = new ArrayList<>();

    public GraphScene() {
    }

    public ArrayList<Vertex> vertices() {
        return vertices;
    }

    public ArrayList<Edge> edges() {
        return edges;
    }

    public void clear() {
        vertices.clear();
        edges.clear();
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getEdgeCount() {
        return edges.size();
    }

}
