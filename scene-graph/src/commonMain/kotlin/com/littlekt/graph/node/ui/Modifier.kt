package com.littlekt.graph.node.ui

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * An ordered chain of changes applied to a [Control] when it is built, similar to Jetpack Compose
 * modifiers. Start a chain with the [Modifier] companion and add to it with extension functions.
 *
 * ```
 * val primary = Modifier.minSize(120f, 40f).fontColor(Color.WHITE)
 *
 * button("Play", modifier = primary.expandWidth()) { onPressed { start() } }
 * ```
 *
 * Custom modifiers are extension functions that call [then]:
 * ```
 * fun Modifier.card() = padding(12).background(FlatDrawable(Color.DARK_GRAY, cornerRadius = 6))
 *
 * fun Modifier.named(name: String) = then(Modifier.Element { it.name = name })
 * ```
 *
 * Elements are applied in order from the outside in. An [Element] after a [Wrapper] applies to the
 * wrapped control and an [Element] before it applies to the wrapper.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
interface Modifier {

    /** Calls [action] on each [Element] & [Wrapper] of this modifier, outermost first. */
    fun forEachElement(action: (Modifier) -> Unit)

    /** @return a modifier that applies this modifier and then [other] */
    infix fun then(other: Modifier): Modifier =
        if (other === Modifier) this else CombinedModifier(this, other)

    /** A modifier that changes the [Control] it is applied to. */
    fun interface Element : Modifier {
        /** Applies this element to the [control]. */
        fun apply(control: Control)

        override fun forEachElement(action: (Modifier) -> Unit) = action(this)
    }

    /** A modifier that wraps the [Control] in another [Control], such as a [PaddedContainer]. */
    fun interface Wrapper : Modifier {
        /** @return a new [Control] that has [control] as a child */
        fun wrap(control: Control): Control

        override fun forEachElement(action: (Modifier) -> Unit) = action(this)
    }

    /** The empty [Modifier] that every chain starts from. */
    companion object : Modifier {
        override fun forEachElement(action: (Modifier) -> Unit) = Unit

        override fun then(other: Modifier): Modifier = other

        override fun toString(): String = "Modifier"
    }
}

private class CombinedModifier(private val outer: Modifier, private val inner: Modifier) :
    Modifier {
    override fun forEachElement(action: (Modifier) -> Unit) {
        outer.forEachElement(action)
        inner.forEachElement(action)
    }

    override fun toString(): String = "$outer.then($inner)"
}

/**
 * Applies [modifier] to this [Control], then the [callback], and adds the outermost [Control] to
 * the [parent]. Used by the scene graph DSL builders.
 *
 * @return this [Control]
 */
@OptIn(ExperimentalContracts::class)
inline fun <T : Control> T.addTo(parent: Node, modifier: Modifier, callback: T.() -> Unit): T {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    val outerIndex = applyInnerModifiers(this, modifier)
    callback()
    addWithOuterModifiers(this, parent, modifier, outerIndex)
    return this
}

/**
 * Applies the elements of [modifier] after its last [Modifier.Wrapper] to [control].
 *
 * @return the index of the last [Modifier.Wrapper], or `-1`
 */
@PublishedApi
internal fun applyInnerModifiers(control: Control, modifier: Modifier): Int {
    if (modifier === Modifier) return -1
    val elements = modifierElements(modifier)
    var i = elements.lastIndex
    while (i >= 0) {
        val element = elements[i]
        if (element is Modifier.Wrapper) break
        (element as Modifier.Element).apply(control)
        i--
    }
    return i
}

/**
 * Applies the elements of [modifier] up to [index], wrapping [control] for each
 * [Modifier.Wrapper], and adds the outermost [Control] to [parent].
 */
@PublishedApi
internal fun addWithOuterModifiers(control: Control, parent: Node, modifier: Modifier, index: Int) {
    var outer = control
    if (index >= 0) {
        val elements = modifierElements(modifier)
        var i = index
        while (i >= 0) {
            when (val element = elements[i]) {
                is Modifier.Wrapper -> outer = element.wrap(outer)
                is Modifier.Element -> element.apply(outer)
            }
            i--
        }
    }
    outer.addTo(parent)
}

private fun modifierElements(modifier: Modifier): List<Modifier> {
    val elements = mutableListOf<Modifier>()
    modifier.forEachElement { elements += it }
    return elements
}
