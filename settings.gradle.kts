rootProject.name = "littlekt"
include("core")
include("samples")
if (System.getenv()["JITPACK"] == null) {
    include("samples")
}