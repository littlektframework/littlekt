package com.lehaine.littlekt.util.internal

@JsFun("() => { return Date.now(); }")
private external fun Date_now(): Double

@JsFun("() => { return performance.now(); }")
private external fun Performance_now(): Double

internal actual fun epochMillis(): Long = Date_now().toLong()

internal actual fun now(): Double = Performance_now()

actual inline fun <R> lock(lock: Any, block: () -> R): R = block()

@JsFun("(text) => { return alert(text); }")
external fun alert(text: String)

@JsFun("() => { return {}; }")
internal external fun jsObject(): JsAny

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: JsAny?): JsAny?

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: Int): JsAny?

@JsFun("(obj, key) => { return obj[key]; }")
internal external fun JsAny_get(obj: JsAny, key: String): JsAny?

@JsFun("(obj, key, value) => { obj[key] = value; }")
internal external fun JsAny_set(obj: JsAny, key: JsAny?, value: JsAny?)

@JsFun("(obj, value) => { obj.push(value); }")
internal external fun JsArray_push(obj: JsAny, value: JsAny?)

@JsFun("(obj, key) => { return obj[key] !== undefined; }")
internal external fun JsAny_has(obj: JsAny, key: JsAny?): Boolean

@JsFun("(obj, key, params) => { return obj[key].apply(obj, params); }")
internal external fun JsAny_invokeApply(obj: JsAny, key: JsAny?, params: JsArray<JsAny?>): JsAny?

internal fun JsAny.getAny(key: Int): JsAny? = JsAny_get(this, key)
internal fun JsAny.getAny(key: String): JsAny? = JsAny_get(this, key)
internal fun JsAny.getAny(key: JsAny?): JsAny? = JsAny_get(this, key)

internal fun JsAny.setAny(key: Int, value: JsAny?) = setAny(key.toJsNumber(), value)
internal fun JsAny.setAny(key: String, value: JsAny?) = setAny(key.toJsString(), value)
internal fun JsAny.setAny(key: JsAny?, value: JsAny?) = JsAny_set(this, key, value)

internal fun JsAny.hasAny(key: Int): Boolean = JsAny_has(this, key.toJsNumber())
internal fun JsAny.hasAny(key: JsString): Boolean = JsAny_has(this, key)
internal fun JsAny.hasAny(key: JsAny?): Boolean = JsAny_has(this, key)