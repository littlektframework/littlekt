package com.littlekt.graph.node.ui

import com.littlekt.graph.node.Node
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.util.SingleSignal
import com.littlekt.graph.util.signal1v
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Groups the buttons built in [block] so only one can be pressed at a time, like radio buttons.
 * The [ButtonGroupScope.button]s are added to this [Node] as toggle buttons in the group. Any other
 * nodes built in [block] are added as usual.
 *
 * ```
 * column {
 *     label("Difficulty")
 *     buttonGroup(onSelected = { println((it as Button).text) }) {
 *         button("Easy") { pressed = true }
 *         button("Normal")
 *         button("Hard")
 *     }
 * }
 * ```
 *
 * @param group the group to add the buttons to
 * @param onSelected called when a button of the group is pressed, after the buttons are built
 * @param block the block to build the buttons in
 * @return the [ButtonGroup]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.buttonGroup(
    group: ButtonGroup = ButtonGroup(),
    noinline onSelected: ((BaseButton) -> Unit)? = null,
    block: ButtonGroupScope.() -> Unit,
): ButtonGroup {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    ButtonGroupScope(this, group).block()
    if (onSelected != null) group.onSelected += onSelected
    return group
}

/**
 * A group of toggle [BaseButton]s where only one can be pressed at a time, like radio buttons. Add
 * a button with [BaseButton.setButtonGroup] or build them in a [buttonGroup].
 *
 * @author Colton Daily
 * @date 1/17/2022
 */
class ButtonGroup {
    internal val members = mutableSetOf<BaseButton>()

    /** The buttons in this group. */
    val buttons: Set<BaseButton>
        get() = members

    /** The pressed button of this group, if any. */
    val selected: BaseButton?
        get() = members.firstOrNull { it.pressed }

    /** Emitted with the button of this group that was pressed. */
    val onSelected: SingleSignal<BaseButton> = signal1v()
}

/**
 * The scope of a [buttonGroup]. Buttons built with [button] join the [group] as toggle buttons.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class ButtonGroupScope
@PublishedApi
internal constructor(
    @PublishedApi internal val parent: Node,
    /** The group the buttons join. */
    val group: ButtonGroup,
) {
    /**
     * Adds a toggle [Button] in the [group] to the parent [Node] and then triggers the [callback].
     *
     * @param text the text of the button
     * @param modifier the [Modifier] to apply
     * @param callback the block to configure the button
     */
    @OptIn(ExperimentalContracts::class)
    inline fun button(
        text: String? = null,
        modifier: Modifier = Modifier,
        callback: @SceneGraphDslMarker Button.() -> Unit = {},
    ): Button {
        contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
        return createGroupButton(text, group).addTo(parent, modifier, callback)
    }
}

@PublishedApi
internal fun createGroupButton(text: String?, group: ButtonGroup): Button =
    Button().also {
        if (text != null) it.text = text
        it.toggleMode = true
        it.setButtonGroup(group)
    }
