package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.CatgirlRoutes.Companion.onHypixel
import catgirlroutes.events.PacketSentEvent
import catgirlroutes.events.ReceivePacketEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.utils.ChatUtils.chatMessage
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.Island
import catgirlroutes.utils.LocationManager
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoomName
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import me.odinmain.events.impl.PacketReceivedEvent
import me.odinmain.utils.skyblock.EtherWarpHelper
import me.odinmain.utils.skyblock.skyblockID
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Zpew : Module(
    name = "Zpew",
    category = Category.MISC
) {
    private val dingdingding: BooleanSetting = BooleanSetting("dingdingding", false)

    enum class SoundOptions(override val displayName: String) : Options {
        NOTE_PLING("note.pling"),
        MOB_BLAZE_HIT("mob.blaze.hit"),
        FIRE_IGNITE("fire.ignite"),
        RANDOM_ORB("random.orb"),
        RANDOM_BREAK("random.break"),
        MOB_GUARDIAN_LAND_HIT("mob.guardian.land.hit"),
        CUSTOM("Custom");
    }
    private val soundSelector = SelectorSetting("Custom Sound", SoundOptions.NOTE_PLING, SoundOptions.entries.toTypedArray(), "Sound Selection").withDependency { dingdingding.enabled }
    private val customSound: StringSetting = StringSetting("Custom Sound", SoundOptions.NOTE_PLING.displayName, description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency { dingdingding.enabled }
    private val pitch: NumberSetting = NumberSetting("Pitch", 1.0, 0.1, 2.0, 0.1).withDependency { dingdingding.enabled }

    init {
        addSettings(dingdingding, soundSelector, customSound, pitch)
    }

    private const val FAILWATCHPERIOD: Int = 20
    private const val MAXFAILSPERFAILPERIOD: Int = 3
    private const val MAXQUEUEDPACKETS: Int = 3

    private var updatePosition = true
    val recentlySentC06s = mutableListOf<SentC06>()
    private val recentFails = mutableListOf<Long>()
    private val blackListedBlocks = arrayListOf(Blocks.chest, Blocks.trapped_chest, Blocks.enchanting_table, Blocks.hopper, Blocks.furnace, Blocks.crafting_table)

    private var lastPitch: Float = 0f
    private var lastYaw: Float = 0f
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var lastZ: Double = 0.0
    private var isSneaking: Boolean = false

    private fun checkAllowedFails(): Boolean {
        if(LocationManager.currentArea.isArea(Island.SinglePlayer)) return true;

        if (recentlySentC06s.size >= MAXQUEUEDPACKETS) return false

        while (recentFails.size != 0 && System.currentTimeMillis() - recentFails[0] > FAILWATCHPERIOD * 1000) recentFails.removeFirst()

        return recentFails.size < MAXFAILSPERFAILPERIOD
    }

    private fun doZeroPingEtherWarp() {
        val etherBlock = EtherWarpHelper.getEtherPos(
            Vec3(lastX, lastY, lastZ),
            lastYaw,
            lastPitch,
            57.0
        )
        if (!etherBlock.succeeded) return

        val pos: BlockPos = etherBlock.pos!!
        val x: Double = pos.x.toDouble() + 0.5
        val y: Double = pos.y.toDouble() + 1.05
        val z: Double = pos.z.toDouble() + 0.5

        var yaw = lastYaw
        val pitch = lastPitch

        yaw %= 360
        if (yaw < 0) yaw += 360
        if (yaw > 360) yaw -= 360

        lastX = x
        lastY = y
        lastZ = z
        updatePosition = false

        recentlySentC06s.add(SentC06(yaw, pitch, x, y, z, System.currentTimeMillis()))

        if (dingdingding.enabled) playLoudSound(getSound(), 100f, Zpew.pitch.value.toFloat())

        scheduleTask(0) {
            mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y, z, yaw, pitch, mc.thePlayer.onGround))
            mc.thePlayer.setPosition(x, y, z)
            mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
            updatePosition = true
        }
    }

    fun isWithinTolerance(n1: Float, n2: Float, tolerance: Double = 1e-4): Boolean { // todo: move to MathUtils I think
        return kotlin.math.abs(n1 - n2) < tolerance
    }

    fun getBlockPlayerIsLookingAt(distance: Double = 5.0): Block? { // todo: move to PlayerUtils maybe
        val rayTraceResult = mc.thePlayer.rayTrace(distance, 1f)
        return rayTraceResult?.blockPos?.let { mc.theWorld.getBlockState(it).block }
    }


    @SubscribeEvent
    fun onC08(event: PacketSentEvent) {
        if (mc.thePlayer == null) return
        if (event.packet !is C08PacketPlayerBlockPlacement) return

        val dir = event.packet.placedBlockDirection
        if (dir != 255) return

        if (!isSneaking || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID" || getBlockPlayerIsLookingAt() in blackListedBlocks) return

        if(!checkAllowedFails()) {
            chatMessage("§cZero ping etherwarp teleport aborted.")
            chatMessage("§c${recentFails.size} fails last ${FAILWATCHPERIOD}s")
            chatMessage("§c${recentlySentC06s.size} C06's queued currently")
            return
        }

        doZeroPingEtherWarp()
    }

    @SubscribeEvent
    fun onC03(event: PacketSentEvent) {
        if (event.packet !is C03PacketPlayer) return
        if (!updatePosition) return
        val x = event.packet.positionX
        val y = event.packet.positionY
        val z = event.packet.positionZ
        val yaw = event.packet.yaw
        val pitch = event.packet.pitch

        if (event.packet.isMoving) {
            lastX = x
            lastY = y
            lastZ = z
        }

        if (event.packet.rotating) {
            lastYaw = yaw
            lastPitch = pitch
        }
    }

    @SubscribeEvent
    fun onC0B(event: PacketSentEvent) {
        if (event.packet !is C0BPacketEntityAction) return
        if (event.packet.action == C0BPacketEntityAction.Action.START_SNEAKING) isSneaking = true
        if (event.packet.action == C0BPacketEntityAction.Action.STOP_SNEAKING) isSneaking = false
    }

    @SubscribeEvent
    fun onS08(event: ReceivePacketEvent) {
        if (event.packet !is S08PacketPlayerPosLook) return
        if (inBoss || !onHypixel) return
        if (recentlySentC06s.isEmpty()) return

        val sentC06 = recentlySentC06s[0]
        recentlySentC06s.removeFirst()

        val newYaw = event.packet.yaw
        val newPitch = event.packet.pitch
        val newX = event.packet.x
        val newY = event.packet.y
        val newZ = event.packet.z

        val isCorrect = (
                (isWithinTolerance(sentC06.yaw, newYaw) || newYaw == 0f) &&
                (isWithinTolerance(sentC06.pitch, newPitch) || newPitch == 0f) &&
                newX == sentC06.x &&
                newY == sentC06.y &&
                newZ == sentC06.z
                )

        if (isCorrect) {
            debugMessage("Correct")
            event.isCanceled = true
            return
        }

        debugMessage("newYaw: $newYaw")
        debugMessage("newPitch: $newPitch")
        debugMessage("newX: $newX")
        debugMessage("newY: $newY")
        debugMessage("newZ: $newZ")
        debugMessage(sentC06)
        debugMessage("Failed")

        recentFails.add(System.currentTimeMillis())
        while (recentlySentC06s.size > 0) recentlySentC06s.removeFirst()
    }

    @SubscribeEvent
    fun onS29(event: PacketReceivedEvent) {
        if (event.packet !is S29PacketSoundEffect) return
        val packet: S29PacketSoundEffect = event.packet as S29PacketSoundEffect // I don't think it should be like that in kt lol
        if (packet.soundName != "mob.enderdragon.hit" || packet.volume != 1f || packet.pitch != 0.53968257f || !checkAllowedFails()) return
        event.isCanceled = true
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(): String {
        return if (soundSelector.index < soundSelector.options.size - 1)
            soundSelector.selected
        else
            customSound.text
    }

    private var shouldBypassVolume: Boolean = false // todo: move to PlayerUtils I think

    private fun playLoudSound(sound: String?, volume: Float, pitch: Float) {
        shouldBypassVolume = true
        mc.thePlayer?.playSound(sound, volume, pitch)
        shouldBypassVolume = false
    }

    data class SentC06(
        val yaw: Float,
        val pitch: Float,
        val x: Double,
        val y: Double,
        val z: Double,
        val sentAt: Long
    )
}

