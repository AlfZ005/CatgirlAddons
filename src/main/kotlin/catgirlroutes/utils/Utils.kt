package catgirlroutes.utils

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.dungeon.tiles.Rotations
import gg.essential.universal.ChatColor.Companion.FORMATTING_CODE_PATTERN
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import kotlin.math.round

object Utils {
    fun relativeClip(x: Double, y: Double, z: Double) {
        modMessage("clipping2")
        mc.thePlayer.setPosition(mc.thePlayer.posX + x,mc.thePlayer.posY + y,mc.thePlayer.posZ + z)
    }

    val String?.noControlCodes: String
        get() = this?.let { FORMATTING_CODE_PATTERN.replace(it, "") } ?: ""

    fun Any?.equalsOneOf(vararg options: Any?): Boolean {
        return options.any { this == it }
    }

    fun snapTo(yaw: Float, pitch: Float) {
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch
    }

    fun rightClick() {
        KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
    }

    fun leftClick() {
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
    }

    fun Event.postAndCatch(): Boolean {
        return runCatching {
            MinecraftForge.EVENT_BUS.post(this)
        }.onFailure {
            it.printStackTrace()
            //logger.error("An error occurred", it)
            val style = ChatStyle()
            style.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/od copy ```${it.stackTraceToString().lineSequence().take(10).joinToString("\n")}```")
            style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§6Click to copy the error to your clipboard."))
            modMessage(" Caught an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. §cPlease click this message to copy and send it in the Odin discord!")}.getOrDefault(isCanceled)
    }
    fun getItemSlot(item: String, ignoreCase: Boolean = true): Int? =
        mc.thePlayer?.inventory?.mainInventory?.indexOfFirst { it?.unformattedName?.contains(item, ignoreCase) == true }.takeIf { it != -1 }

    fun swapFromName(name: String): Boolean {
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.displayName
            if (itemName != null) {
                if (itemName.contains(name, ignoreCase = true)) {
                    mc.thePlayer.inventory.currentItem = i
                    return true
                }
            }
        }
        modMessage("$name not found.")
        return false
    }

    fun airClick() {
        mc.netHandler.networkManager.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }

    val ItemStack?.unformattedName: String
        get() = this?.displayName?.noControlCodes ?: ""

    fun Vec3.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 {
        return this.addVector(x.toDouble(), y.toDouble(), z.toDouble())
    }
    fun runOnMCThread(run: () -> Unit) {
        if (!mc.isCallingFromMinecraftThread) mc.addScheduledTask(run) else run()
    }

    private val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
    fun romanToInt(s: String): Int {
        var result = 0
        for (i in 0 until s.length - 1) {
            val current = romanMap[s[i]] ?: 0
            val next = romanMap[s[i + 1]] ?: 0
            result += if (current < next) -current else current
        }
        return result + (romanMap[s.last()] ?: 0)
    }

    inline val posX get() = mc.thePlayer.posX
    inline val posY get() = mc.thePlayer.posY
    inline val posZ get() = mc.thePlayer.posZ

    /**
     * Removes the given coordinates to the Vec3.
     */
    fun Vec3.subtractVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 {
        return this.addVector(-x.toDouble(), -y.toDouble(), -z.toDouble())
    }

    /**
     * Adds the given coordinates to the Vec3.
     */
    fun Vec3i.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3i {
        return Vec3i(this.x + x.toInt(), this.y + y.toInt(), this.z + z.toInt())
    }

    fun Vec3.rotateToNorth(rotation: Rotations): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
            Rotations.WEST -> Vec3(this.zCoord, this.yCoord, -this.xCoord)
            Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
            Rotations.EAST -> Vec3(-this.zCoord, this.yCoord, this.xCoord)
            else -> this
        }
    }

    fun Vec3.rotateAroundNorth(rotation: Rotations): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
            Rotations.WEST -> Vec3(-this.zCoord, this.yCoord, this.xCoord)
            Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
            Rotations.EAST -> Vec3(this.zCoord, this.yCoord, -this.xCoord)
            else -> this
        }
    }

    fun renderText(
        text: String,
        x: Int,
        y: Int,
        scale: Double = 1.0,
        color: Int = 0xFFFFFF,
    ) {
        GlStateManager.pushMatrix()
        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()
        GlStateManager.scale(scale, scale, scale)
        var yOffset = y - mc.fontRendererObj.FONT_HEIGHT
        text.split("\n").forEach {
            yOffset += (mc.fontRendererObj.FONT_HEIGHT * scale).toInt()
            mc.fontRendererObj.drawString(
                it,
                round(x / scale).toFloat(),
                round(yOffset / scale).toFloat(),
                color,
                true
            )
        }
        GlStateManager.popMatrix()
    }
    /**
     * Profiles the specified function with the specified string as profile section name.
     * Uses the minecraft profiler.
     *
     * @param name The name of the profile section.
     * @param func The code to profile.
     */
    inline fun profile(name: String, func: () -> Unit) {
        startProfile(name)
        func()
        endProfile()
    }

    /**
     * Starts a minecraft profiler section with the specified name + "Odin: ".
     * */
    fun startProfile(name: String) {
        mc.mcProfiler.startSection("Catgirl: $name")
    }

    /**
     * Ends the current minecraft profiler section.
     */
    fun endProfile() {
        mc.mcProfiler.endSection()
    }

    data class Vec2(val x: Int, val z: Int)
    data class Vec2f(var x: Float, var y: Float)
    data class Vec3f(val x: Float, val y: Float, val z: Float)
    data class Vec4f(val x: Float, val y: Float, val z: Float, val w: Float)
}