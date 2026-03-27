package com.mongraphe.graphui.model;

import java.util.ArrayList;

public class Vertex {

    public static int upscale = 8;
    public static double initial_node_size = 1;
    public static double degree_scale_factor = 0;

    private int id;
    private double x, y, diameter;
    private boolean isVisible = true;
    private boolean isDeleted = false;
    private final ArrayList<Edge> edges = new ArrayList<>();
    private Community community;

    public Vertex(double x, double y) {
        this.x = x * upscale;
        this.y = y * upscale;
        this.diameter = initial_node_size;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void updatePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    public double getDiameter() {
        return diameter;
    }

    public void updateDiameter() {
        if (degree_scale_factor < 0)
            degree_scale_factor = 0;
        diameter = initial_node_size + degree_scale_factor * getDegree();
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public void delete() {
        isDeleted = true;
        diameter = 0;
    }

    public void restore() {
        isDeleted = false;
        isVisible = true;
        updateDiameter();
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community c) {
        community = c;
    }

    public String getCommunityName() {
        return community != null ? community.getName() : "";
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    public int getDegree() {
        return edges.size();
    }

    @Override
    public String toString() {
        return id + " (" + x + ", " + y + ")";
    }
}