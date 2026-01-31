package com.mongraphe.interfaces;

import com.mongraphe.graphui.Vertex;

public interface VertexRenderI {

    /**
     * Trouve le sommet à la position (x, y)
     * 
     * @param x Position x
     * @param y Position y
     * @return le sommet trouvé, ou null s'il n'y en a pas
     */
    public Vertex findVertexAt(double x, double y, double zoomFactor);
    
}
