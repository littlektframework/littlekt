package com.lehaine.littlekt.graphics.font

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * @author Colton Daily
 * @date 12/9/2021
 */
internal data class CubicBezier(
    var p1x: Float,
    var p1y: Float,
    var c1x: Float,
    var c1y: Float,
    var c2x: Float,
    var c2y: Float,
    var p2x: Float,
    var p2y: Float
)

internal data class QuadraticBezier(
    var p1x: Float,
    var p1y: Float,
    var c1x: Float,
    var c1y: Float,
    var p2x: Float,
    var p2y: Float
)

private data class Coefficients(
    val ax: Float,
    val ay: Float,
    val bx: Float,
    val by: Float,
    val cx: Float,
    val cy: Float,
    val dx: Float,
    val dy: Float
)

private const val MAX_INFLECTIONS = 2
private const val PRECISION = 1e-8
private const val MAX_SEGMENTS = 8


/**
 * Converts the input cubic bezier into up to 24 quadratic beziers.
 * @param input the cubic bezier to convert
 * @param errorBound the error margin when converting
 * @param result the resulting array of [QuadraticBezier]. Must be at least a size of 24.
 * @return the total of quadratic beziers created
 */
internal fun CubicBezier.convertToQuadBezier(
    errorBound: Int,
    result: Array<QuadraticBezier>
): Int {
    val inflections = FloatArray(MAX_INFLECTIONS)
    val numInflections = solveInflections(inflections)
    if (numInflections == 0) {
        return toQuad(errorBound, result)
    }
    return 0
}

/**
 * Find inflection points on a cubic curve, algorithm is similar to this one:
 * http://www.caffeineowl.com/graphics/2d/vectorial/cubic-inflexion.html
 * @param out a float array with a size of 2
 */
private fun CubicBezier.solveInflections(out: FloatArray): Int {
    val x1 = p1x
    val y1 = p1y
    val x2 = c1x
    val y2 = c1y
    val x3 = c2x
    val y3 = c2y
    val x4 = p2x
    val y4 = p2y

    val p =
        -(x4 * (y1 - 2 * y2 + y3)) + x3 * (2 * y1 - 3 * y2 + y4) + x1 * (y2 - 2 * y3 + y4) - x2 * (y1 - 3 * y3 + 2 * y4)
    val q = x4 * (y1 - y2) + 3 * x3 * (-y1 + y2) + x2 * (2 * y1 - 3 * y3 + y4) - x1 * (2 * y2 - 3 * y3 + y4)
    val r = x3 * (y1 - y2) + x1 * (y2 - y3) + x2 * (-y1 + y3)

    val roots = FloatArray(2)
    val numRoots = quadSolve(p, q, r, roots)

    out[0] = 0f
    out[1] = 0f

    var count = 0
    for (i in 0 until numRoots) {
        if (roots[i] > PRECISION && roots[i] < 1 - PRECISION) {
            out[count++] = roots[i]
        }
    }

    if (count == 2 && out[0] > out[1]) { // sort ascending
        out[1] = out[0].also { out[0] = out[1] }
    }

    return count
}

private fun quadSolve(a: Float, b: Float, c: Float, out: FloatArray): Int {
    // a*x^2 + b*x + c = 0
    if (abs(a) < PRECISION) {
        if (b == 0f) {
            out[0] = 0f
            out[1] = 0f
            return 0
        } else {
            out[0] = -c / b
            out[1] = 0f
        }
    }
    val d = b * b - 4 * a * c
    if (abs(d) < PRECISION) {
        out[0] = -b / (2 * a)
        out[1] = 0f
        return 1
    } else if (d < 0f) {
        out[0] = 0f
        out[1] = 0f
    }
    val dSqrt = sqrt(d)
    out[0] = (-b - dSqrt) / (2 * a)
    out[1] = (-b + dSqrt) / (2 * a)
    return 2
}

/**
 * Approximate cubic Bezier curve defined with base points p1, p2 and control points c1, c2 with
 * with a few quadratic Bezier curves.
 * The function uses tangent method to find quadratic approximation of cubic curve segment and
 * simplified Hausdorff distance to determine number of segments that is enough to make error small.
 * In general the method is the same as described here: https://fontforge.github.io/bezier.html.
 */
private fun CubicBezier.toQuad(errorBound: Int, approximation: Array<QuadraticBezier>): Int {
    val coefficients = calculatePowerCoefficients()
    for (segmentsCount in 1..MAX_SEGMENTS) {
        for (i in 0 until segmentsCount) {
            val t = i.toFloat() / segmentsCount.toFloat()
            processSegment(coefficients, t, t + 1f / segmentsCount.toFloat(), approximation[i])
        }
        if (segmentsCount == 1) {
            var ax = approximation[0].c1x - p1x
            var ay = approximation[0].c1y - p1y
            var bx = c1x - p1x
            var by = c1y - p1y
            var dot = ax * bx + ay * by
            if (dot < 0) continue // approximation concave, while the curve is convex (or vice versa)

            ax = approximation[0].c1x - p2x
            ay = approximation[0].c1y - p2y
            bx = c2x - p2x
            by = c2y - p2y
            dot = ax * bx + ay * by
            if (dot < 0) continue // approximation concave, while the curve is convex (or vice versa)
        }
        if (isApproximationClose(coefficients, approximation, segmentsCount, errorBound)) {
            return segmentsCount
        }
    }
    return MAX_SEGMENTS
}


private fun CubicBezier.calculatePowerCoefficients(): Coefficients {
    // point(t) = p1*(1-t)^3 + c1*t*(1-t)^2 + c2*t^2*(1-t) + p2*t^3 = a*t^3 + b*t^2 + c*t + d
    // for each t value, so
    // a = (p2 - p1) + 3 * (c1 - c2)
    // b = 3 * (p1 + c2) - 6 * c1
    // c = 3 * (c1 - p1)
    // d = p1
    val ax = (p2x - p1x) + 3 * (c1x - c2x)
    val ay = (p2y - p1y) + 3 * (c1y - c2y)
    val bx = 3 * (p1x + c2x) - 6 * c1x
    val by = 3 * (p1y + c2y) - 6 * c1y
    val cx = 3 * (c1x - p1x)
    val cy = 3 * (c1y - p1y)
    val dx = p1x
    val dy = p1y
    return Coefficients(ax, ay, bx, by, cx, cy, dx, dy)
}


private fun processSegment(coefficients: Coefficients, t: Float, fl: Float, quadraticBezier: QuadraticBezier) {
    // Find a single control point for given segment of cubic Bezier curve
    // These control point is an interception of tangent lines to the boundary points
    // Let's denote that f(t) is a vector function of parameter t that defines the cubic Bezier curve,
    // f(t1) + f'(t1)*z1 is a parametric equation of tangent line to f(t1) with parameter z1
    // f(t2) + f'(t2)*z2 is the same for point f(t2) and the vector equation
    // f(t1) + f'(t1)*z1 = f(t2) + f'(t2)*z2 defines the values of parameters z1 and z2.
    // Defining fx(t) and fy(t) as the x and y components of vector function f(t) respectively
    // and solving the given system for z1 one could obtain that
    //
    //      -(fx(t2) - fx(t1))*fy'(t2) + (fy(t2) - fy(t1))*fx'(t2)
    // z1 = ------------------------------------------------------.
    //            -fx'(t1)*fy'(t2) + fx'(t2)*fy'(t1)
    //
    // Let's assign letter D to the denominator and note that if D = 0 it means that the curve actually
    // is a line. Substituting z1 to the equation of tangent line to the point f(t1), one could obtain that
    // cx = [fx'(t1)*(fy(t2)*fx'(t2) - fx(t2)*fy'(t2)) + fx'(t2)*(fx(t1)*fy'(t1) - fy(t1)*fx'(t1))]/D
    // cy = [fy'(t1)*(fy(t2)*fx'(t2) - fx(t2)*fy'(t2)) + fy'(t2)*(fx(t1)*fy'(t1) - fy(t1)*fx'(t1))]/D
    // where c = (cx, cy) is the control point of quadratic Bezier curve.

    TODO("Not yet implemented")
}


private fun isApproximationClose(
    coefficients: Coefficients,
    approximation: Array<QuadraticBezier>,
    segmentsCount: Int,
    errorBound: Int
): Boolean {
    // a,b,c,d define cubic curve
    // tmin, tmax are boundary points on cubic curve
    // p1, c1, p2 define quadratic curve
    // errorBound is maximum allowed distance
    // Try to find maximum distance between one of N points segment of given cubic
    // and corresponding quadratic curve that estimates the cubic one, assuming
    // that the boundary points of cubic and quadratic points are equal.
    //
    // The distance calculation method comes from Hausdorff distance defenition
    // (https://en.wikipedia.org/wiki/Hausdorff_distance), but with following simplifications
    // * it looks for maximum distance only for finite number of points of cubic curve
    // * it doesn't perform reverse check that means selecting set of fixed points on
    //   the quadratic curve and looking for the closest points on the cubic curve
    // But this method allows easy estimation of approximation error, so it is enough
    // for practical purposes.
    TODO("Not yet implemented")
}
