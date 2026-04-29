package com.mongraphe.graphui.rendering;

/**
 * Conteneur immuable définissant les paramètres esthétiques et géométriques du
 * rendu des arêtes.
 * *
 * <p>
 * Cette classe permet de configurer si les arêtes doivent être dessinées comme
 * des lignes
 * droites ou des courbes paraboliques, ainsi que la précision de la
 * discrétisation (segments).
 * </p>
 * *
 * <h2>Concepts clés</h2>
 * <ul>
 * <li><b>Segments :</b> Plus le nombre de segments est élevé, plus la courbe
 * est lisse, mais plus le coût en performance augmente.</li>
 * <li><b>Angle de courbure :</b> Définit l'amplitude de la parabole par rapport
 * à la corde de l'arête.</li>
 * <li><b>Alternance :</b> Permet de varier la direction de la courbure pour
 * éviter que toutes les arêtes ne se chevauchent.</li>
 * </ul>
 */
public final class GraphRenderOptions {

    /**
     * Définit la primitive géométrique utilisée pour représenter une arête.
     */
    public enum EdgeStyle {
        /** Ligne directe entre deux sommets. */
        STRAIGHT,
        /** Courbe de Bézier quadratique approximant une parabole. */
        CURVED_PARABOLIC
    }

    private final EdgeStyle edgeStyle;
    private final int curveSegments;
    private final float curveAngleDegrees;
    private final float maxCurveOffset;
    private final float edgeLineWidth;
    private final boolean alternateCurveDirection;

    /**
     * Constructeur complet pour définir les options de rendu.
     * * @param edgeStyle Style de l'arête (Droit ou Courbé).
     * 
     * @param curveSegments           Nombre de divisions pour les courbes (min 1).
     * @param curveAngleDegrees       Angle d'inclinaison de la courbe en degrés (0
     *                                à 85°).
     * @param maxCurveOffset          Décalage maximal autorisé pour la courbe en
     *                                unités monde.
     * @param edgeLineWidth           Épaisseur de la ligne (pixels OpenGL).
     * @param alternateCurveDirection Si vrai, inverse la direction de la courbe
     *                                selon l'ID des sommets.
     */
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

    /**
     * @return Une configuration par défaut pour des arêtes rectilignes simples.
     */
    public static GraphRenderOptions straight() {
        return new GraphRenderOptions(EdgeStyle.STRAIGHT, 1, 0f, 0f, 1f, false);
    }

    /**
     * @return Une configuration de prévisualisation haute qualité avec courbure par
     *         défaut.
     */
    public static GraphRenderOptions previewView() {
        return previewView(true, 22f);
    }

    /**
     * Crée une configuration personnalisée pour la vue de prévisualisation.
     * * @param curved Active ou non la courbure.
     * 
     * @param angleDegrees L'angle de courbure souhaité.
     */
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

    /**
     * Sécurise l'angle de courbure pour éviter des déformations aberrantes ou des
     * divisions par zéro.
     * Les angles sont contraints dans l'intervalle $[0, 85]$.
     */
    private static float clampAngle(float angleDegrees) {
        if (Float.isNaN(angleDegrees) || Float.isInfinite(angleDegrees)) {
            return 0f;
        }
        return Math.max(0f, Math.min(85f, angleDegrees));
    }
}