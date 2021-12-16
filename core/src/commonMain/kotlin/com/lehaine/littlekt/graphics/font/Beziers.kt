package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.isFuzzyEqual
import com.lehaine.littlekt.math.isFuzzyZero
import kotlin.math.*

/**
 * @author Colton Daily
 * @date 12/9/2021
 */

internal data class Bezier(
    val p0: MutableVec2f = MutableVec2f(),
    val p1: MutableVec2f = MutableVec2f(),
    val control: MutableVec2f = MutableVec2f()
) {

    /**
     * Taking a quadratic bezier curve and a horizontal line y=Y, finds the x
     * values of intersection of the line and the curve. Returns 0, 1, or 2,
     * depending on how many intersections were found, and outX is filled with
     * that many x values of intersection.
     *
     * Quadratic bezier curves are represented by the function
     * F(t) = (1-t)^2*A + 2*t*(1-t)*B + t^2*C
     * where F is a vector function, A and C are the endpoint vectors, C is
     * the control point vector, and 0 <= t <= 1.
     * Solving the bezier function for t gives:
     * t = (A - B [+-] sqrt(y*a + B^2 - A*C))/a , where  a = A - 2B + C.
     * http://www.wolframalpha.com/input/?i=y+%3D+(1-t)%5E2a+%2B+2t(1-t)*b+%2B+t%5E2*c+solve+for+t
     */
    fun intersectHorizontal(y: Float, outX: FloatArray): Int {
        val A = p0
        val B = control
        val C = p1
        var i = 0

        //Parts of the bezier function solved for t
        val a = A.y - 2 * B.y + C.y
        if (isFuzzyEqual(a, 0f)) {
            val t = (2 * B.y - C.y - y) / (2 * (B.y - C.y))
            if (t in 0.0..1.0) {
                outX[i++] = (1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x
                return i
            }
        }
        val sqrtTerm = sqrt(y * a + B.y * B.y - A.y * C.y)
        var t = (A.y - B.y + sqrtTerm) / a
        if (t in 0.0..1.0) {
            outX[i++] = (1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x
        }
        t = (A.y - B.y - sqrtTerm) / a
        if (t in 0.0..1.0) {
            outX[i++] = (1 - t) * (1 - t) * A.x + 2 * t * (1 - t) * B.x + t * t * C.x
        }
        return i
    }

    /**
     * Same as [intersectHorizontal], except finds the y values of an intersection
     * with the vertical line x=X.
     */
    fun intersectVertical(x: Float, outY: FloatArray): Int {
        val inverse = Bezier()
        inverse.p0.set(p0.y, p0.x)
        inverse.p1.set(p1.y, p1.x)
        inverse.control.set(control.y, control.x)
        return inverse.intersectHorizontal(x, outY)
    }
}


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
    val dy: Float,
) {
    val a: Vec2f = Vec2f(ax, ay)
    val b: Vec2f = Vec2f(bx, by)
    val c: Vec2f = Vec2f(cx, cy)
    val d: Vec2f = Vec2f(dx, dy)
}

private const val MAX_INFLECTIONS = 2
private const val PRECISION = 1e-8f
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
    if (a.isFuzzyZero(PRECISION)) {
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
    if (d.isFuzzyZero(PRECISION)) {
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


private fun processSegment(coefficients: Coefficients, t: Float, t2: Float, result: QuadraticBezier) {
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

    val f1 = coefficients.calcPoint(t)
    val f2 = coefficients.calcPoint(t2)
    val df1 = coefficients.calcPointDerivative(t)
    val df2 = coefficients.calcPointDerivative(t2)

    result.p1x = f1.x
    result.p1y = f1.y
    result.p2x = f2.x
    result.p2y = f2.y

    val d = -df1.x * df2.y + df2.x * df1.y

    if (d.isFuzzyZero(PRECISION)) {
        result.c1x = (f1.x + f2.x) / 2f
        result.c1y = (f1.y + f2.y) / 2f
        return
    }
    val cx = (df1.x * (f2.y * df2.x - f2.x * df2.y) + df2.x * (f1.x * df1.y - f1.y * df1.x)) / d
    val cy = (df1.y * (f2.y * df2.x - f2.x * df2.y) + df2.y * (f1.x * df1.y - f1.y * df1.x)) / d
    result.c1x = cx
    result.c1y = cy
}


private fun Coefficients.calcPoint(t: Float): Vec2f {
    val result = MutableVec2f()
    // a*t^3 + b*t^2 + c*t + d = ((a*t + b)*t + c)*t + d
    return a.scale(t, result).add(b).scale(t).add(c).scale(t).add(d)
}


private fun Coefficients.calcPointDerivative(t: Float): Vec2f {
    // d/dt[a*t^3 + b*t^2 + c*t + d] = 3*a*t^2 + 2*b*t + c = (3*a*t + 2*b)*t + c
    val result = MutableVec2f()
    val temp1 = MutableVec2f()
    val temp2 = MutableVec2f()
    a.scale(3 * t, temp1)
    b.scale(2f, temp2)
    return temp1.add(temp2, result).scale(t).add(c)
}

private fun isApproximationClose(
    coefficients: Coefficients,
    quadCurves: Array<QuadraticBezier>,
    quadCurvesLength: Int,
    errorBound: Int
): Boolean {

    val dt = 1f / quadCurvesLength
    for (i in 0 until quadCurvesLength) {
        val p1x = quadCurves[i].p1x
        val p1y = quadCurves[i].p1y
        val c1x = quadCurves[i].c1x
        val c1y = quadCurves[i].c1y
        val p2x = quadCurves[i].p2x
        val p2y = quadCurves[i].p2y
        if (!isSegmentApproximationClose(
                coefficients,
                i * dt,
                (i + 1) * dt,
                p1x,
                p1y,
                c1x,
                c1y,
                p2x,
                p2y,
                errorBound
            )
        ) {
            return false
        }
    }
    return true
}

private fun isSegmentApproximationClose(
    coefficients: Coefficients,
    tMin: Float,
    tMax: Float,
    p1x: Float,
    p1y: Float,
    c1x: Float,
    c1y: Float,
    p2x: Float,
    p2y: Float,
    errorBound: Int
): Boolean {
    // a,b,c,d define cubic curve
    // tMin, tMax are boundary points on cubic curve
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
    val n = 10 // number of points  +1
    val dt = (tMax - tMin) / n
    var t = tMin + dt
    while (t < tMax - dt) {
        val point = coefficients.calcPoint(t)
        if (minDistanceToQuad(point, p1x, p1y, c1x, c1y, p2x, p2y) > errorBound) {
            return false
        }
        t += dt
    }
    return true
}

private val a = MutableVec2f()
private val b = MutableVec2f()
private val c = MutableVec2f()
private val tempVec = MutableVec2f()
private fun minDistanceToQuad(
    point: Vec2f,
    p1x: Float,
    p1y: Float,
    c1x: Float,
    c1y: Float,
    p2x: Float,
    p2y: Float
): Float {
    // f(t) = (1-t)^2 * p1 + 2*t*(1 - t) * c1 + t^2 * p2 = a*t^2 + b*t + c, t in [0, 1],
    // a = p1 + p2 - 2 * c1
    // b = 2 * (c1 - p1)
    // c = p1; a, b, c are vectors because p1, c1, p2 are vectors too
    // The distance between given point and quadratic curve is equal to
    // sqrt((f(t) - point)^2), so these expression has zero derivative by t at points where
    // (f'(t), (f(t) - point)) = 0.
    // Substituting quadratic curve as f(t) one could obtain a cubic equation
    // e3*t^3 + e2*t^2 + e1*t + e0 = 0 with following coefficients:
    // e3 = 2 * a^2
    // e2 = 3 * a*b
    // e1 = (b^2 + 2 * a*(c - point))
    // e0 = (c - point)*b
    // One of the roots of the equation from [0, 1], or t = 0 or t = 1 is a value of t
    // at which the distance between given point and quadratic Bezier curve has minimum.
    // So to find the minimal distance one have to just pick the minimum value of
    // the distance on set {t = 0 | t = 1 | t is root of the equation from [0, 1] }.
    a.set(p1x, p1y)
        .add(p2x, p2y)
        .subtract(c1x * 2, c1y * 2)
    b.set(c1x, c1y)
        .subtract(p1x, p1y)
        .scale(2f)
    c.set(p1x, p1y)
    tempVec.set(c).subtract(point)
    val e3 = 2 * a.sqrLength()
    val e2 = 3 * a.dot(b)
    val e1 = (b.sqrLength() + 2 * a.dot(tempVec))
    val e0 = tempVec.dot(b)

    val roots = FloatArray(3)
    val numRoots = cubicSolve(e3, e2, e1, e0, roots)

    val candidates = FloatArray(5)
    var numCandidates = 0
    for (i in 0 until numRoots) {
        if (roots[i] > PRECISION && roots[i] < 1 - PRECISION) {
            candidates[numCandidates++] = roots[i]
        }
    }
    candidates[numCandidates++] = 0f
    candidates[numCandidates++] = 1f
    var minDistance = Float.POSITIVE_INFINITY
    for (i in 0 until numCandidates) {
        val distance = calcPointQuad(a, b, c, candidates[i]).subtract(point).length()
        if (distance < minDistance) {
            minDistance = distance
        }
    }
    return minDistance
}

private fun cubicSolve(a: Float, b: Float, c: Float, d: Float, out: FloatArray): Int {
    // a*x^3 + b*x^2 + c*x + d = 0
    if (a.isFuzzyZero(PRECISION)) {
        out[2] = 0f
        return quadSolve(b, c, d, out)
    }
    // solve using Cardan's method, which is described in paper of R.W.D. Nickals
    // http://www.nickalls.org/dick/papers/maths/cubic1993.pdf (doi:10.2307/3619777)
    val xn = -b / (3 * a) // point of symmetry x coordinate
    val yn = ((a * xn + b) * xn + c) * xn + d // point of symmetry y coordinate
    val deltaSqr = (b * b - 3 * a * c) / (9 * a * a)  // delta^2
    val hSqr = 4 * a * a * deltaSqr.pow(3)
    val d3 = yn * yn - hSqr
    if (d3.isFuzzyZero(PRECISION)) {
        val delta1 = cubicRoot(yn / (2 * a))
        out[0] = xn - 2 * delta1
        out[1] = xn + delta1
        out[2] = 0f
        return 2
    } else if (d3 > 0f) {
        val d3Sqrt = sqrt(d3)
        out[0] = xn + cubicRoot((-yn + d3Sqrt) / (2 * a)) + cubicRoot((-yn - d3Sqrt) / (2 * a))
        out[1] = 0f
        out[2] = 0f
        return 1
    }
    // 3 real roots
    val theta = acos(-yn / sqrt(hSqr)) / 3
    val delta = sqrt(deltaSqr)
    out[0] = xn + 2 * delta * cos(theta)
    out[1] = xn + 2 * delta * cos(theta + PI.toFloat() * 2f / 3f)
    out[2] = xn + 2 * delta * cos(theta + PI.toFloat() * 4f / 3f)
    return 3
}

private fun cubicRoot(x: Float): Float {
    return if (x < 0) -(-x.pow(1f / 3f)) else x.pow(1f / 3f)
}

private fun calcPointQuad(a: Vec2f, b: Vec2f, c: Vec2f, t: Float): MutableVec2f {
    return MutableVec2f().set(a).scale(t).add(b).scale(t).add(c)
}