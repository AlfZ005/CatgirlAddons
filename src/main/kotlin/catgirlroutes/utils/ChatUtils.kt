package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.impl.render.ClickGui
import catgirlroutes.utils.ChatUtils.chatMessage
import catgirlroutes.utils.ChatUtils.command
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ChatUtils.sendChat
import catgirlroutes.utils.Utils.runOnMCThread
import net.minecraft.event.HoverEvent
import net.minecraft.util.*
import net.minecraftforge.client.ClientCommandHandler

/**
 * ## A collection of utility functions for creating and sending or displaying chat messages.
 *
 * Use [chatMessage] to put messages in chat which are only visible locally and are not sent to the server.
 *
 * Use [modMessage] for client side messages in chat that start with mods chat prefix.
 *
 * Use [sendChat] for sending a player message to the server.
 *
 * Use [command] to execute commands either client side or send them to the server.
 *
 * @author Aton
 */
@Suppress("unused")
object ChatUtils {
    val BLACK         = EnumChatFormatting.BLACK.toString()
    val DARK_BLUE     = EnumChatFormatting.DARK_BLUE.toString()
    val DARK_GREEN    = EnumChatFormatting.DARK_GREEN.toString()
    val DARK_AQUA     = EnumChatFormatting.DARK_AQUA.toString()
    val DARK_RED      = EnumChatFormatting.DARK_RED.toString()
    val DARK_PURPLE   = EnumChatFormatting.DARK_PURPLE.toString()
    val GOLD          = EnumChatFormatting.GOLD.toString()
    val GRAY          = EnumChatFormatting.GRAY.toString()
    val DARK_GRAY     = EnumChatFormatting.DARK_GRAY.toString()
    val BLUE          = EnumChatFormatting.BLUE.toString()
    val GREEN         = EnumChatFormatting.GREEN.toString()
    val AQUA          = EnumChatFormatting.AQUA.toString()
    val RED           = EnumChatFormatting.RED.toString()
    val LIGHT_PURPLE  = EnumChatFormatting.LIGHT_PURPLE.toString()
    val YELLOW        = EnumChatFormatting.YELLOW.toString()
    val WHITE         = EnumChatFormatting.WHITE.toString()
    val OBFUSCATED    = EnumChatFormatting.OBFUSCATED.toString()
    val BOLD          = EnumChatFormatting.BOLD.toString()
    val STRIKETHROUGH = EnumChatFormatting.STRIKETHROUGH.toString()
    val UNDERLINE     = EnumChatFormatting.UNDERLINE.toString()
    val ITALIC        = EnumChatFormatting.ITALIC.toString()
    val RESET         = EnumChatFormatting.RESET.toString()

    /**
     * Pattern to replace formatting codes with & with the § equivalent.
     *
     * This Regex will match all "&" that are directly followed by one of 0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,k,l,m,n,o,r.
     *
     * This regex is the same as
     *
     *     Regex("(?i)&(?=[0-9A-FK-OR])").
     */
    private val formattingCodePattern = Regex("&(?=[0-9A-FK-OR])", RegexOption.IGNORE_CASE)

    /**
     * Replaces chat formatting codes using "&" as escape character with "§" as the escape character.
     * Example: "&aText &r" as input will return "§aText §r".
     */
    private fun reformatString(text: String): String {
        return formattingCodePattern.replace(text, "§")
    }

    /**
     * Gets selected mod prefix in [ClickGui]
     */
    fun getPrefix(): String {
        return when (ClickGui.prefixStyle.index) {
            0 -> CatgirlRoutes.CHAT_PREFIX;
            1 -> CatgirlRoutes.SHORT_PREFIX
            else -> reformatString( ClickGui.customPrefix.text)
        }
    }

    /**
     * Remove control codes from the [receiver][String] with the [vanilla function][StringUtils.stripControlCodes] for it.
     */
    fun String.stripControlCodes(): String {
        return StringUtils.stripControlCodes(this)
    }

    /**
     * Puts a message in chat client side with the mod prefix.
     * @param reformat Replace the "&" in formatting strings with "§".
     * @see chatMessage
     */
    fun modMessage(message: Any?, prefix: String = getPrefix(), chatStyle: ChatStyle? = null) {
        val chatComponent = ChatComponentText("$prefix §8 §r $message")
        chatStyle?.let { chatComponent.setChatStyle(it) } // Set chat style using setChatStyle method
        runOnMCThread { mc.thePlayer?.addChatMessage(chatComponent) }
    }

    /**
     * Puts a message in chat client side with the mod prefix.
     * @see chatMessage
     */
    fun modMessage(iChatComponent: IChatComponent) = chatMessage(
        ChatComponentText(getPrefix() + " ").appendSibling(iChatComponent)
    )

    /**
     * Print a message in chat **client side**.
     * @param reformat Replace the "&" in formatting strings with "§".
     * @see modMessage
     * @see sendChat
     */
    fun chatMessage(text: String, reformat: Boolean = true) {
        val message: IChatComponent = ChatComponentText(if (reformat) reformatString(text) else text)
        chatMessage(message)
    }

    /**
     * Print a message in chat **client side**.
     * @see modMessage
     * @see sendChat
     */
    fun chatMessage(iChatComponent: IChatComponent) {
        mc.thePlayer?.addChatMessage(iChatComponent)
    }

    /**
     * Print a message in chat if devMode is enabled
     * @see chatMessage
     */
    fun devMessage(message: Any?) {
        if (!ClickGui.devMode.enabled) return;
        modMessage(message, prefix = "§5[§dCga§cDev§5]")
    }

    /**
     * Print a message in chat if debugMode is enabled
     * @see chatMessage
     */
    fun debugMessage(message: Any?) {
        if (!ClickGui.debugMode.enabled) return;
        modMessage(message, prefix = "§5[§dCga§fDebug§5]")
    }

    /**
     * **Send player message to the server**.
     *
     * This mimics the player using the chat gui to send the message.
     * @see chatMessage
     * @see modMessage
     */
    fun sendChat(message: String) {
        mc.thePlayer?.sendChatMessage(message)
    }

    /**
     * Runs the specified command. Per default sends it to the server but has client side option.
     * The input is assumed to **not** include the slash "/" that signals a command.
     */
    fun command(text: String, clientSide: Boolean = true) {
        if (clientSide && mc.thePlayer != null) ClientCommandHandler.instance.executeCommand(mc.thePlayer, "/$text")
        else mc.thePlayer?.sendChatMessage("/$text")
    }

    /**
     * Runs the specified command. It doesn't matter if it's client or server sided command. It would send it anyway
     * ClientCommandHandler.instance.executeCommand(mc.thePlayer, text) == 0 will NOT give any feedback in chat if the command isn't client sided
     */
    fun commandAny(text: String) {
        mc.thePlayer?.let {
            val command = text.removePrefix("/")
            if (ClientCommandHandler.instance.executeCommand(it, "/$command") == 0) {
                it.sendChatMessage("/$command")
            }
        }
    }

    /**
     * Creates a new IChatComponent displaying [text] and showing [hoverText] when it is hovered.
     * [hoverText] can include "\n" for new lines.
     *
     * Use [IChatComponent.appendSibling] to combine multiple Chat components into one.
     * Use the formatting characters to format the text.
     */
    fun createHoverableText(text: String, hoverText: String): IChatComponent {
        val message: IChatComponent = ChatComponentText(text)
        val style = ChatStyle()
        style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText(hoverText))
        message.chatStyle = style
        return message
    }
}
