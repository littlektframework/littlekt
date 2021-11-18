[1mdiff --git a/src/commonMain/kotlin/com.lehaine.littlekt/Graphics.kt b/src/commonMain/kotlin/com.lehaine.littlekt/Graphics.kt[m
[1mindex 650a0bc..bb2ab81 100644[m
[1m--- a/src/commonMain/kotlin/com.lehaine.littlekt/Graphics.kt[m
[1m+++ b/src/commonMain/kotlin/com.lehaine.littlekt/Graphics.kt[m
[36m@@ -12,7 +12,7 @@[m [minterface Graphics {[m
      * Enumeration describing different types of [Graphics] implementations.[m
      */[m
     enum class GraphicsType {[m
[31m-        AndroidGL, LWJGL, WebGL, iOSGL, JGLFW, Mock, LWJGL3[m
[32m+[m[32m        AndroidGL, WebGL, iOSGL, Mock, LWJGL3[m
     }[m
 [m
     /**[m
[1mdiff --git a/src/commonMain/kotlin/com.lehaine.littlekt/TypeAlias.kt b/src/commonMain/kotlin/com.lehaine.littlekt/TypeAlias.kt[m
[1mindex 391f8d8..41a7c4b 100644[m
[1m--- a/src/commonMain/kotlin/com.lehaine.littlekt/TypeAlias.kt[m
[1m+++ b/src/commonMain/kotlin/com.lehaine.littlekt/TypeAlias.kt[m
[36m@@ -4,10 +4,6 @@[m [mpackage com.lehaine.littlekt[m
  * @author Colton Daily[m
  * @date 9/28/2021[m
  */[m
[31m-/**[m
[31m- * Temporal Unit used for animation descriptions[m
[31m- */[m
[31m-typealias Milliseconds = Long[m
 [m
 /**[m
  * Temporal unit[m
[36m@@ -24,38 +20,14 @@[m [mtypealias Percent = Number[m
  */[m
 typealias ByteMask = Int[m
 [m
[31m-/**[m
[31m- * Degree: value between 0 and 360[m
[31m- */[m
[31m-typealias Degree = Number[m
[31m-[m
[31m-/**[m
[31m- * Coordinate in the related context (in general: world unit)[m
[31m- */[m
[31m-typealias Coordinate = Number[m
[31m-[m
 /**[m
  * Pixel unit. In general related to an image or the screen.[m
  */[m
 typealias Pixel = Int[m
 [m
[31m-/**[m
[31m- * Ratio. Used in general to deal with screen size.[m
[31m- */[m
[31m-typealias Ratio = Float[m
 [m
 fun Number.toPercent(): Float {[m
     val v = this.toFloat()[m
     require(v in 0.0..1.0)[m
     return v[m
[31m-}[m
[31m-[m
[31m-/**[m
[31m- * Position in the screen device[m
[31m- */[m
[31m-typealias DevicePosition = Float[m
[31m-[m
[31m-/**[m
[31m- * Position in the game device (ie: in the game viewport)[m
[31m- */[m
[31m-typealias GamePosition = Float[m
\ No newline at end of file[m
[32m+[m[32m}[m
\ No newline at end of file[m
[1mdiff --git a/src/jvmMain/kotlin/com/lehaine/littlekt/LwjglGraphics.kt b/src/jvmMain/kotlin/com/lehaine/littlekt/LwjglGraphics.kt[m
[1mindex 002bfe6..c5abcb5 100644[m
[1m--- a/src/jvmMain/kotlin/com/lehaine/littlekt/LwjglGraphics.kt[m
[1m+++ b/src/jvmMain/kotlin/com/lehaine/littlekt/LwjglGraphics.kt[m
[36m@@ -11,9 +11,9 @@[m [mclass LwjglGraphics : Graphics {[m
     override val gl: GL = LwjglGL()[m
 [m
     internal var _width: Int = 0[m
[31m-    internal var _height = 0[m
[31m-    internal var _backBufferWidth = 0[m
[31m-    internal var _backBufferHeight = 0[m
[32m+[m[32m    internal var _height: Int = 0[m
[32m+[m[32m    internal var _backBufferWidth: Int = 0[m
[32m+[m[32m    internal var _backBufferHeight: Int = 0[m
 [m
     override val width: Int[m
         get() = _width[m
[36m@@ -44,9 +44,7 @@[m [mclass LwjglGraphics : Graphics {[m
         TODO("Not yet implemented")[m
     }[m
 [m
[31m-    override fun getType(): Graphics.GraphicsType? {[m
[31m-        TODO("Not yet implemented")[m
[31m-    }[m
[32m+[m[32m    override fun getType(): Graphics.GraphicsType = Graphics.GraphicsType.LWJGL3[m
 [m
     override fun getPpiX(): Float {[m
         TODO("Not yet implemented")[m
