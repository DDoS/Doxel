package ca.sapon.doxel.polygonizer;

import org.lwjgl.util.vector.Vector3f;

/**
 * Represents a cell within a grid of values.
 * <p/>
 * This is the numbering of the corners.
 * <p/>
 * <pre>
 * /\
 * | y
 * |
 * |     x
 * ------->
 * \
 *  \
 *   \ z
 *   \/
 *
 * 4-----5
 * |\    |\
 * | 7-----6
 * | |   | |
 * 0-|---1 |
 *  \|    \|
 *   3-----2
 * </pre>
 * <p/>
 * The vectors represent the positions of the corners. The floats are the values at the corners.
 *
 * @author DDoS
 */
public class GridCell {
    public final Vector3f p0 = new Vector3f();
    public double v0;
    public final Vector3f p1 = new Vector3f();
    public double v1;
    public final Vector3f p2 = new Vector3f();
    public double v2;
    public final Vector3f p3 = new Vector3f();
    public double v3;
    public final Vector3f p4 = new Vector3f();
    public double v4;
    public final Vector3f p5 = new Vector3f();
    public double v5;
    public final Vector3f p6 = new Vector3f();
    public double v6;
    public final Vector3f p7 = new Vector3f();
    public double v7;
}
