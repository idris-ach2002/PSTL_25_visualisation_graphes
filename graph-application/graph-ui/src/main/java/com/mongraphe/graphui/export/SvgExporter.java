package com.mongraphe.graphui.export;

import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphRenderOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class SvgExporter {

    private static final double LOOP_OFFSET = 22.0;

    public static void export(File file,
            List<Vertex> vertices,
            List<Edge> edges,
            double worldWidth,
            double worldHeight,
            GraphRenderOptions renderOptions) throws IOException {

        // Taille du SVG (viewBox carrée)
        double svgSize = 1000.0;
        double scale = svgSize / Math.max(worldWidth, worldHeight);
        double offsetX = (svgSize - worldWidth * scale) / 2.0;
        double offsetY = (svgSize - worldHeight * scale) / 2.0;

        double halfW = worldWidth / 2.0;
        double halfH = worldHeight / 2.0;

        java.util.function.Function<Double, Double> worldToSvgX = x -> offsetX + (x + halfW) * scale;
        java.util.function.Function<Double, Double> worldToSvgY = y -> offsetY + (halfH - y) * scale;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %.2f %.2f'>\n",
                svgSize, svgSize));

        // Pas de bordure par défaut, arêtes avec la bonne épaisseur
        double edgeWidth = renderOptions != null ? renderOptions.edgeLineWidth() : 1.0;
        sb.append("<defs>\n");
        sb.append("  <style>\n");
        sb.append(String.format(Locale.US, "    .edge { stroke-width: %.2f; fill: none; }\n", edgeWidth));
        sb.append("    .node { stroke: none; }\n");
        sb.append("  </style>\n");
        sb.append("</defs>\n");

        // Arêtes
        sb.append("<g id='edges'>\n");
        boolean curved = renderOptions != null &&
                renderOptions.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC;

        for (Edge e : edges) {
            if (!e.isVisible())
                continue;

            Vertex start = e.getStart();
            Vertex end = e.getEnd();
            double x1 = worldToSvgX.apply(start.getX());
            double y1 = worldToSvgY.apply(start.getY());
            double x2 = worldToSvgX.apply(end.getX());
            double y2 = worldToSvgY.apply(end.getY());

            String color = String.format("#%02x%02x%02x",
                    (int) (e.getR() * 255),
                    (int) (e.getG() * 255),
                    (int) (e.getB() * 255));

            if (!curved) {
                sb.append(String.format(Locale.US,
                        "  <line x1='%.2f' y1='%.2f' x2='%.2f' y2='%.2f' stroke='%s' class='edge' />\n",
                        x1, y1, x2, y2, color));
            } else {
                double dx = x2 - x1;
                double dy = y2 - y1;
                double len = Math.hypot(dx, dy);

                if (len < 0.0001) {
                    double cx = x1 + LOOP_OFFSET;
                    double cy = y1 + LOOP_OFFSET;
                    sb.append(String.format(Locale.US,
                            "  <path d='M %.2f,%.2f Q %.2f,%.2f %.2f,%.2f' stroke='%s' class='edge' />\n",
                            x1, y1, cx, cy, x2, y2, color));
                } else {
                    double nx = -dy / len;
                    double ny = dx / len;
                    double angleRad = Math.toRadians(renderOptions.curveAngleDegrees());
                    double offset = Math.tan(angleRad) * len * 0.5;
                    if (renderOptions.maxCurveOffset() > 0) {
                        offset = Math.min(offset, renderOptions.maxCurveOffset());
                    }
                    double sign = 1.0;
                    if (renderOptions.alternateCurveDirection()) {
                        int a = Math.min(e.getStartId(), e.getEndId());
                        int b = Math.max(e.getStartId(), e.getEndId());
                        int hash = (31 * a) ^ (17 * b);
                        sign = (hash & 1) == 0 ? 1.0 : -1.0;
                    }
                    double mx = (x1 + x2) * 0.5;
                    double my = (y1 + y2) * 0.5;
                    double cx = mx + nx * offset * sign;
                    double cy = my + ny * offset * sign;

                    sb.append(String.format(Locale.US,
                            "  <path d='M %.2f,%.2f Q %.2f,%.2f %.2f,%.2f' stroke='%s' class='edge' />\n",
                            x1, y1, cx, cy, x2, y2, color));
                }
            }
        }
        sb.append("</g>\n");

        // Noeuds
        sb.append("<g id='nodes'>\n");
        for (Vertex v : vertices) {
            if (!v.isVisible() || v.isDeleted())
                continue;
            double cx = worldToSvgX.apply(v.getX());
            double cy = worldToSvgY.apply(v.getY());
            double r = (v.getDiameter() / 2.0) * scale;

            String fillColor = "#888888";
            if (v.getCommunity() != null) {
                fillColor = String.format("#%02x%02x%02x",
                        (int) (v.getCommunity().getR() * 255),
                        (int) (v.getCommunity().getG() * 255),
                        (int) (v.getCommunity().getB() * 255));
            }
            sb.append(String.format(Locale.US,
                    "  <circle cx='%.2f' cy='%.2f' r='%.2f' fill='%s' class='node' />\n",
                    cx, cy, r, fillColor));
        }
        sb.append("</g>\n");

        sb.append("</svg>");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }
    }
}