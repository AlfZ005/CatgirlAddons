package catgirlroutes.module.settings.impl

import catgirlroutes.module.settings.Setting
import catgirlroutes.module.settings.Visibility

/**
 * A clickable setting for Modules that runs code on click.
 *
 * Represented by a button in the GUI which when clicked will invoke [value].
 * @author Aton
 */
class ActionSetting(
    name: String,
    visibility: Visibility = Visibility.VISIBLE,
    description: String? = null,
    override val default: () -> Unit = {}
) : Setting<() -> Unit>(name, visibility, description) {

    override var value: () -> Unit = default

    var action: () -> Unit by this::value

    fun doAction() {
        action()
    }
}
