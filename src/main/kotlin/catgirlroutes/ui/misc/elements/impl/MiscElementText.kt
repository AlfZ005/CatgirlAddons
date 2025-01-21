package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.ColorUtil.withAlpha
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.clickgui.util.FontUtil.getStringWidth
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.utils.render.HUDRenderUtils.drawRoundedBorderedRect
import catgirlroutes.utils.render.HUDRenderUtils.renderRect
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

/**
 * A [MiscElement] that displays and optionally allows input for a string value with customizable options and style.
 *
 * @param x The x-coordinate position (default is 0.0).
 * @param y The y-coordinate position (default is 0.0).
 * @param width The width of the element (default is 100.0).
 * @param height The height of the element (default is 20.0).
 * @param value The initial text value (default is an empty string).
 * @param options An integer representing additional options (NUM_ONLY, NO_SPACE, FORCE_CAPS).
 * @param prependText Text that will be displayed before the text (default is none).
 * @param thickness The border thickness (default is 2.0).
 * @param radius The radius of the corners (default is 5.0).
 * @param bgColour The background color of the element (default is `ColorUtil.buttonColor`).
 */
class MiscElementText(
    x: Double = 0.0,
    y: Double = 0.0,
    width: Double = 100.0,
    height: Double = 20.0,
    var value: String = "",
    var options: Int = 0,
    var prependText: String = "",
    var thickness: Double = 2.0,
    var radius: Double = 5.0,
    val bgColour: Color = Color(ColorUtil.buttonColor)
) : MiscElement(x, y, width, height) {

    var focus: Boolean = false

    private val textField: GuiTextField = GuiTextField(
        0, mc.fontRendererObj, 0,
        0, 0, 0
    )

    var text: String
        get() = textField.text
        set(value) { textField.text = value }

    init {
        textField.isFocused = true
        textField.setCanLoseFocus(false)
        textField.maxStringLength = 9999
        textField.text = value
    }

    private fun getCursorPos(mouseX: Int): Int {
        val xComp = mouseX - x - getStringWidth(prependText)
        val renderText = prependText + textField.text

        val targetLine = renderText.lines().firstOrNull() ?: return renderText.length

        val padding = ((5).coerceAtMost(this.width.toInt() - strLenNoColor(targetLine))) / 2
        val adjustedX = (xComp - padding).coerceAtLeast(0.0)

        val trimmed = mc.fontRendererObj.trimStringToWidth(targetLine, adjustedX.toInt())
        return strLenNoColor(trimmed).coerceAtMost(renderText.length)
    }

    private var lastClickTime: Long = 0
    private var doubleClickStart: Int = -1
    private var doubleClickEnd: Int = -1

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isHovered(mouseX, mouseY)) { focus = false; return false }

        val currentTime = System.currentTimeMillis()
        val cursorPos = getCursorPos(mouseX)

        when(mouseButton) {
            0 -> {
                if (currentTime - lastClickTime < 250) {
                    val wordBounds = findWordBounds(text, cursorPos)
                    doubleClickStart = wordBounds.first
                    doubleClickEnd = wordBounds.second

                    textField.cursorPosition = doubleClickStart
                    textField.setSelectionPos(doubleClickEnd)
                } else textField.cursorPosition = cursorPos
            }
            1 -> text = ""
            else -> textField.cursorPosition = cursorPos
        }
        focus = true
        lastClickTime = currentTime

        return true
    }

    override fun otherComponentClick() {
        focus = false
        textField.setSelectionPos(textField.cursorPosition)
    }

    private fun strLenNoColor(str: String): Int {
        return str.replace("(?i)\\u00A7.".toRegex(), "").length
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (!focus || !this.isHovered(mouseX, mouseY)) return
        textField.setSelectionPos(getCursorPos(mouseX))
        return super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!focus) return false
        var typedChar2 = typedChar

        when {
            GuiScreen.isKeyComboCtrlV(keyCode) -> {
                textField.setEnabled(false)
                val (start, end) = listOf(textField.cursorPosition, textField.selectionEnd).sorted()

                textField.text = StringBuilder(text).replace(start, end, GuiScreen.getClipboardString()).toString()
                textField.cursorPosition = start + GuiScreen.getClipboardString().length
            }
            else -> textField.setEnabled(true)
        }

        val old = textField.text
        if ((options and FORCE_CAPS) != 0) typedChar2 = typedChar2.uppercaseChar()
        if ((options and NO_SPACE) != 0 && typedChar2 == ' ') return false

        textField.apply {
            isFocused = true
            textboxKeyTyped(typedChar2, keyCode)
            if ((options and NUM_ONLY) != 0 && text.any { it !in "0-9." }) text = old
        }

        return true
    }

    override fun render(mouseX: Int, mouseY: Int) { // todo fucking "scroll"
        val renderText = prependText + textField.text

        GlStateManager.pushMatrix()
        GlStateManager.color(1.0f, 1.0f, 1.0f)

        drawRoundedBorderedRect(x, y, this.width, this.height, this.radius, this.thickness, bgColour, if (focus) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor))

//        val textX = if (getStringWidth(renderText) > this.width - 10) {
//            x + this.width - getStringWidth(renderText) - 5
//        } else {
//            x + 5
//        }

        FontUtil.drawString(renderText, x + 5, y + (this.height - 8) / 2)

        if (focus && System.currentTimeMillis() % 1000 > 500) {
            val cursorText = renderText.substring(0, textField.cursorPosition + prependText.length)
            renderRect(x + 5 + getStringWidth(cursorText) - 0.5, y + (this.height - 8) / 2 - 1, 1.0, 10.0, Color.WHITE)
        }

        if (textField.selectedText.isNotEmpty()) {
            val (left, right) = listOf(textField.cursorPosition, textField.selectionEnd)
                .map { it + strLenNoColor(prependText) }
                .let { it.minOrNull() to it.maxOrNull() }

            var texX = 0.0
            val textNoColor = renderText.replace(Regex("(?i)\\u00A7([^\\u00B6\\n])(?!\\u00B6)")) { "\u00B6${it.groupValues[1]}" }

            textNoColor.forEachIndexed { i, c ->
                val len = getStringWidth(c.toString())
                if (i in left!! until right!!) {
                    renderRect(x + 5 + texX, y + (this.height - 8) / 2 - 1, len.toDouble(), 10.0, Color.WHITE.withAlpha(150))
                }
                texX += len
            }
        }

        GlStateManager.popMatrix()
    }

    private fun findWordBounds(text: String, cursorPos: Int): Pair<Int, Int> {
        if (text.isEmpty()) return Pair(0, 0)

        var start = cursorPos
        var end = cursorPos

        // find start
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }

        // find end
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }

        return Pair(start, end)
    }

    companion object { // todo: change
        const val NUM_ONLY = 0b10000
        const val NO_SPACE = 0b01000
        const val FORCE_CAPS = 0b00100
    }
}
