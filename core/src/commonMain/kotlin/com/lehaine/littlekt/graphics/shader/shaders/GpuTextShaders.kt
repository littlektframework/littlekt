package com.lehaine.littlekt.graphics.shader.shaders

import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.VertexShaderModel

/**
 * @author Colton Daily
 * @date 12/12/2021
 */
class GpuTextVertexShader : VertexShaderModel() {
    val uTexture = ShaderParameter.UniformSample2D("u_texture")
    val uTexelSize = ShaderParameter.UniformVec2("u_texelSize")
    val uProjTrans = ShaderParameter.UniformMat4("u_projTrans")
    val aPosition = ShaderParameter.AttributeVec2("a_position")
    val aColor = ShaderParameter.AttributeVec4("a_color")
    val aTexCoord0 = ShaderParameter.AttributeVec2("a_texCoord0")

    override val parameters: MutableList<ShaderParameter> =
        mutableListOf(
            uTexture, uTexelSize, uProjTrans, aPosition, aColor, aTexCoord0
        )

    // language=GLSL
    override var source: String = """
        uniform sampler2D u_texture;
        uniform vec2 u_texelSize;
        uniform mat4 u_projTrans;
        
        attribute vec2 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        
        varying vec4 v_color;
        varying vec2 v_bezierCoord;
        varying vec2 v_normCoord;
        varying vec4 v_gridRect;
        
        float ushortFromVec2(vec2 v) {
        	return (v.y * 65280.0 + v.x * 255.0);
        }
        
        vec2 vec2FromPixel(vec2 coord) {
        	vec4 pixel = texture2D(u_texture, (coord+0.5)*u_texelSize);
        	return vec2(ushortFromVec2(pixel.xy), ushortFromVec2(pixel.zw));
        }
        
        void main()  {
            v_color = a_color;
            v_bezierCoord = floor(a_texCoord0 * 0.5);
            v_normCoord = mod(a_texCoord0, 2.0) * 1.1;
            v_gridRect = vec4(vec2FromPixel(v_bezierCoord), vec2FromPixel(v_bezierCoord + vec2(1,0)));
            gl_Position = u_projTrans*vec4(a_position, 0.0, 1.0);
        }
    """.trimIndent()
}

class GpuTextFragmentShader : FragmentShaderModel() {
    val uTexture get() = parameters[0] as ShaderParameter.UniformSample2D
    val uTexelSize get() = parameters[1] as ShaderParameter.UniformVec2

    override val parameters: MutableList<ShaderParameter> =
        mutableListOf(
            ShaderParameter.UniformSample2D("u_texture"),
            ShaderParameter.UniformVec2("u_texelSize")
        )

    // language=GLSL
    override var source: String = """
        #define numSS 4
        #define pi 3.1415926535897932384626433832795
        #define kPixelWindowSize 1.0
        
        uniform sampler2D u_texture;
        uniform vec2 u_texelSize;

        varying vec4 v_color;
        varying vec2 v_bezierCoord;
        varying vec2 v_normCoord;
        varying vec4 v_gridRect;

        float positionAt(float p0, float p1, float p2, float t) {
            float mt = 1.0 - t;
            return mt*mt*p0 + 2.0*t*mt*p1 + t*t*p2;
        }

        float tangentAt(float p0, float p1, float p2, float t) {
            return 2.0 * (1.0-t) * (p1 - p0) + 2.0 * t * (p2 - p1);
        }

        bool almostEqual(float a, float b) {
            return abs(a-b) < 1e-5;
        }

        float normalizedUshortFromVec2(vec2 v) {
            return (v.y * 65280.0 + v.x * 255.0) / 65536.0;
        }    
        
        vec4 getPixelByXY(vec2 coord) {
        	return texture2D(u_texture, (coord + 0.5) * u_texelSize);
        }

        void fetchBezier(int coordIndex, out vec2 p[3]) {
            for (int i = 0; i < 3; i++) {
                vec4 pixel = getPixelByXY(vec2(v_bezierCoord.x + float(2 + coordIndex*3 + i), v_bezierCoord.y));
                p[i] = vec2(normalizedUshortFromVec2(pixel.xy), normalizedUshortFromVec2(pixel.zw)) - v_normCoord;
            }
        }

        int getAxisIntersections(float p0, float p1, float p2, out vec2 t) {
            if (almostEqual(p0, 2.0*p1 - p2)) {
                t[0] = 0.5 * (p2 - 2.0*p1) / (p2 - p1);
                return 1;
            }

            float sqrtTerm = p1*p1 - p0*p2;
            if (sqrtTerm < 0.0) return 0;
            sqrtTerm = sqrt(sqrtTerm);
            float denom = p0 - 2.0*p1 + p2;
            t[0] = (p0 - p1 + sqrtTerm) / denom;
            t[1] = (p0 - p1 - sqrtTerm) / denom;
            return 2;
        }

        float integrateWindow(float x) {
            float xsq = x*x;
            return sign(x) * (0.5 * xsq*xsq - xsq) + 0.5;           // parabolic window
            //return 0.5 * (1.0 - sign(x) * xsq);                     // box window
        }

        mat2 getUnitLineMatrix(vec2 b1, vec2 b2) {
            vec2 V = b2 - b1;
            float normV = length(V);
            V = V / (normV*normV);

            return mat2(V.x, -V.y, V.y, V.x);
        }

        void updateClosestCrossing(in vec2 porig[3], mat2 M, inout float closest, vec2 integerCell) {
            vec2 p[3];
            for (int i = 0; i < 3; i++) {
                p[i] = M * porig[i];
            }
            vec2 t;
            int numT = getAxisIntersections(p[0].y, p[1].y, p[2].y, t);
            for (int i=0; i<2; i++) {
                if (i == numT) {
                    break;
                }
                if (t[i] > 0.0 && t[i] < 1.0) {
                    float posx = positionAt(p[0].x, p[1].x, p[2].x, t[i]);
//                    vec2 op = vec2(positionAt(porig[0].x, porig[1].x, porig[2].x, t[i]),
//                                   positionAt(porig[0].y, porig[1].y, porig[2].y, t[i]));
//                    op += v_normCoord;
               //     bool sameCell = floor(clamp(op * v_gridRect.zw, vec2(0.5), vec2(v_gridRect.zw)-0.5)) == integerCell;
                    //if (posx > 0.0 && posx < 1.0 && posx < abs(closest)) {
                    if (posx > 0.0 && abs(posx) < abs(closest)) {
                        float derivy = tangentAt(p[0].y, p[1].y, p[2].y, t[i]);
                        closest = (derivy < 0.0) ? -posx : posx;
                    }
                }
            }
        }

        mat2 inverseMat(mat2 m) {
            return mat2(m[1][1],-m[0][1], -m[1][0], m[0][0]) / (m[0][0]*m[1][1] - m[0][1]*m[1][0]);
        }

        void main() {
            vec2 integerCell = floor(clamp(v_normCoord * v_gridRect.zw, vec2(0.5), vec2(v_gridRect.zw)-0.5));
            vec2 indicesCoord = v_gridRect.xy + integerCell + 0.5;
            vec2 cellMid = (integerCell + 0.5) / v_gridRect.zw;
            
            mat2 initrot = inverseMat(mat2(dFdx(v_normCoord) * kPixelWindowSize, dFdy(v_normCoord) * kPixelWindowSize));
            
            float theta = pi / float(numSS);
            mat2 rotM = mat2(cos(theta), sin(theta), -sin(theta), cos(theta)); // note this is column major ordering
            
            ivec4 indices1 = ivec4(texture2D(u_texture, indicesCoord*u_texelSize) * 255.0 + 0.5);
            // indices2 = ivec4(texture2D(uAtlasSampler, vec2(indicesCoord.x + vGridSize.x, indicesCoord.y) * uTexelSize) * 255.0 + 0.5);
            // bool moreThanFourIndices = indices1[0] < indices1[1];
            
            // The mid-inside flag is encoded by the order of the beziers indices.
            bool midInside = indices1[0] > indices1[1];
            
            float midClosest = midInside ? -2.0 : 2.0;
            
            float firstIntersection[numSS];
            for (int ss=0; ss<numSS; ss++) {
                firstIntersection[ss] = 2.0;
            }
            
            float percent = 0.0;
            
            mat2 midTransform = getUnitLineMatrix(v_normCoord, cellMid);
            
            for (int bezierIndex = 0; bezierIndex < 4; bezierIndex++) {
                int coordIndex = indices1[bezierIndex];
                
                // Indices 0 and 1 are both "no bezier"
                if (coordIndex < 2) {
                    continue;
                }
                vec2 p[3];
                fetchBezier(coordIndex - 2, p);
                updateClosestCrossing(p, midTransform, midClosest, integerCell);
                // Transform p so fragment in glyph space is a unit circle
                for (int i = 0; i < 3; i++) {
                    p[i] = initrot * p[i];
                }
                // Iterate through angles
                for (int ss=0; ss<numSS; ss++) {
                    vec2 t;
                    int numT = getAxisIntersections(p[0].x, p[1].x, p[2].x, t);
                    
                    for (int tindex = 0; tindex < 2; tindex++) {
                        if (tindex == numT) break;
                        
                        if (t[tindex] > 0.0 && t[tindex] <= 1.0) {
                            float derivx = tangentAt(p[0].x, p[1].x, p[2].x, t[tindex]);
                            float posy = positionAt(p[0].y, p[1].y, p[2].y, t[tindex]);
                            if (posy > -1.0 && posy < 1.0) {
                                // Note: whether to add or subtract in the next statement is determined
                                // by which convention the path uses: moving from the bezier start to end,
                                // is the inside to the right or left?
                                // The wrong operation will give buggy looking results, not a simple inverse.
                                float delta = integrateWindow(posy);
                                percent = percent + (derivx < 0.0 ? delta : -delta);
                                float intersectDist = posy + 1.0;
                                if (intersectDist < abs(firstIntersection[ss])) {
                                    firstIntersection[ss] = derivx < 0.0 ? -intersectDist : intersectDist;
                                }
                            }
                        }
                    }
                    if (ss + 1 < numSS) {
                        for (int i = 0; i < 3; i++) {
                            p[i] = rotM * p[i];
                        }
                    }
                } // ss
            }
            bool midVal = midClosest < 0.0;
            // Add contribution from rays that started inside
            for (int ss = 0; ss < numSS; ss++) {
                if ((firstIntersection[ss] >= 2.0 && midVal) || (firstIntersection[ss] > 0.0 && abs(firstIntersection[ss]) < 2.0)) {
                    percent = percent + 1.0 /*integrateWindow(-1.0)*/;
                }
            }
            
            percent = percent / float(numSS);
            gl_FragColor = v_color;
            gl_FragColor.a *= percent;
            
//            vec2 gridCell = mod(floor(integerCell), 2.0);
//            gl_FragColor.r = (gridCell.x - gridCell.y) * (gridCell.x - gridCell.y);
//            gl_FragColor.a += 0.3;
        }
    """.trimIndent()
}