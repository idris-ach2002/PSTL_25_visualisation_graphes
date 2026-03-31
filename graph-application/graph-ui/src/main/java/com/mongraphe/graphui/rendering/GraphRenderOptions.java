package com.mongraphe.graphui.rendering;

public final class GraphRenderOptions {

    public enum EdgeStyle {
        STRAIGHT,
        CURVED_PARABOLIC
    }

    private final EdgeStyle edgeStyle;
    private final int curveSegments;
    private final float curveAngleDegrees;
    private final float maxCurveOffset;
    private final float edgeLineWidth;
    private final boolean alternateCurveDirection;

    public GraphRenderOptions(
            EdgeStyle edgeStyle,
            int curveSegments,
            float curveAngleDegrees,
            float maxCurveOffset,
            float edgeLineWidth,
            boolean alternateCurveDirection) {
        this.edgeStyle = edgeStyle == null ? EdgeStyle.STRAIGHT : edgeStyle;
        this.curveSegments = Math.max(1, curveSegments);
        this.curveAngleDegrees = clampAngle(curveAngleDegrees);
        this.maxCurveOffset = Math.max(0f, maxCurveOffset);
        this.edgeLineWidth = Math.max(1f, edgeLineWidth);
        this.alternateCurveDirection = alternateCurveDirection;
    }

    public static GraphRenderOptions straight() {
        return new GraphRenderOptions(EdgeStyle.STRAIGHT, 1, 0f, 0f, 1f, false);
    }

    public static GraphRenderOptions previewView() {
        return previewView(true, 22f);
    }

    public static GraphRenderOptions previewView(boolean curved, float angleDegrees) {
        return curved
                ? new GraphRenderOptions(EdgeStyle.CURVED_PARABOLIC, 20, angleDegrees, 64f, 1.35f, true)
                : straight();
    }

    public EdgeStyle edgeStyle() {
        return edgeStyle;
    }

    public int curveSegments() {
        return curveSegments;
    }

    public float curveAngleDegrees() {
        return curveAngleDegrees;
    }

    public float maxCurveOffset() {
        return maxCurveOffset;
    }

    public float edgeLineWidth() {
        return edgeLineWidth;
    }

    public boolean alternateCurveDirection() {
        return alternateCurveDirection;
    }

    private static float clampAngle(float angleDegrees) {
        if (Float.isNaN(angleDegrees) || Float.isInfinite(angleDegrees)) {
            return 0f;
        }
        return Math.max(0f, Math.min(85f, angleDegrees));
    }
}
