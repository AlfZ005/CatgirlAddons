package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.commands.impl.Node
import catgirlroutes.commands.impl.NodeManager
import catgirlroutes.commands.impl.NodeManager.nodes
import catgirlroutes.commands.impl.nodeEditMode
import catgirlroutes.events.impl.PacketSentEventReturn
import catgirlroutes.events.impl.SecretPickupEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.player.PearlClip
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.utils.ChatUtils.commandAny
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.PlayerUtils.airClick
import catgirlroutes.utils.PlayerUtils.leftClick2
import catgirlroutes.utils.PlayerUtils.recentlySwapped
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.Utils.renderText
import catgirlroutes.utils.dungeon.DungeonUtils.getRealCoords
import catgirlroutes.utils.dungeon.DungeonUtils.getRealYaw
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.ScanUtils.currentRoom
import catgirlroutes.utils.render.WorldRenderUtils.drawP3boxWithLayers
import catgirlroutes.utils.render.WorldRenderUtils.renderGayFlag
import catgirlroutes.utils.render.WorldRenderUtils.renderTransFlag
import catgirlroutes.utils.rotation.RotationUtils.snapTo
import kotlinx.coroutines.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color.black
import java.awt.Color.white
import kotlin.collections.set
import kotlin.math.floor

object AutoRoutes : Module(
    "Auto Routes",
    category = Category.DUNGEON,
    description = "A module that allows you to place down nodes that execute various actions."
){
    private val editTitle = BooleanSetting("EditMode title", false)
    private val annoyingShit = BooleanSetting("Annoying messages", true)
    private val boomType = StringSelectorSetting("Boom type","Regular", arrayListOf("Regular", "Infinity"), "Superboom TNT type to use for BOOM ring")
    private val preset = StringSelectorSetting("Node style","Trans", arrayListOf("Trans", "Normal", "LGBTQIA+"), description = "Ring render style to be used.")
    private val layers = NumberSetting("Ring layers amount", 3.0, 3.0, 5.0, 1.0, "Amount of ring layers to render").withDependency { preset.selected == "Normal" }
    private val colour1 = ColorSetting("Ring colour (inactive)", black, false, "Colour of Normal ring style while inactive").withDependency { preset.selected == "Normal" }
    private val colour2 = ColorSetting("Ring colour (active)", white, false, "Colour of Normal ring style while active").withDependency { preset.selected == "Normal" }

    init {
        this.addSettings(
            editTitle,
            annoyingShit,
            boomType,
            preset,
            layers,
            colour1,
            colour2
        )
    }

    @SubscribeEvent
    fun onTick2(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        NodeManager.loadNodes()
    }

    private var secretPickedUpDeferred: CompletableDeferred<Unit>? = null

    suspend fun awaitSecret() {
        val deferred = CompletableDeferred<Unit>()
        secretPickedUpDeferred = deferred
        deferred.await()
    }

    @SubscribeEvent
    fun onSecret(event: SecretPickupEvent) {
        if (annoyingShit.enabled) modMessage("secret!?!?!?!??!?!?!!??!?")
        scheduleTask(1) {
            secretPickedUpDeferred?.complete(Unit)
            secretPickedUpDeferred = null
        }
    }

    @SubscribeEvent
    fun onMouse(event: MouseEvent) {
        if (!event.buttonstate) return
        if (event.button != 0) return

        cooldownMap.forEach { (key, _) ->
            cooldownMap[key] = false
        }

        scheduleTask(1) {
            secretPickedUpDeferred?.complete(Unit)
            secretPickedUpDeferred = null
        }
    }

    private val cooldownMap = mutableMapOf<String, Boolean>()

    @OptIn(DelicateCoroutinesApi::class)
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (nodeEditMode || event.phase != TickEvent.Phase.START || mc.thePlayer == null) return
        nodes.forEach { node ->
            val key = "${node.location.xCoord},${node.location.yCoord},${node.location.zCoord},${node.type}"
            val cooldown: Boolean = cooldownMap[key] == true
            if(inNode(node)) {
                if (cooldown) return@forEach
                cooldownMap[key] = true
                GlobalScope.launch {
                    executeAction(node)
                }
            } else if (cooldown) {
                cooldownMap[key] = false
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        //modMessage(nodes.toString())
        nodes.forEach { node ->
            val room = currentRoom
            var realLocation = node.location

            if (room != null) {
                //modMessage("yo the room aint null $room")
                realLocation = room.getRealCoords(node.location)
            }

            val x: Double = realLocation.xCoord + 0.5
            val y: Double = realLocation.yCoord
            val z: Double = realLocation.zCoord + 0.5

            val cooldown: Boolean = cooldownMap["${node.location.xCoord},${node.location.yCoord},${node.location.zCoord},${node.type}"] == true
            val color = if (cooldown) colour2.value else colour1.value

            when(preset.selected) {
                "Trans"     -> renderTransFlag(x, y, z, node.width, node.height)
                "Normal"    -> drawP3boxWithLayers(x, y, z, node.width, node.height, color, layers.value.toInt())
                "LGBTQIA+"  -> renderGayFlag(x, y, z, node.width, node.height)
            }
        }
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Post) {
        if (editTitle.enabled && nodeEditMode && inDungeons) {
            val sr = ScaledResolution(mc)
            val t = "Edit Mode"
            renderText(t, sr.scaledWidth / 2 - mc.fontRendererObj.getStringWidth(t) / 2, sr.scaledHeight / 2 + mc.fontRendererObj.FONT_HEIGHT)
        }
    }

    private fun inNode(node: Node): Boolean {

        val room = currentRoom
        var realLocation = node.location
        if (room != null) {
            realLocation = room.getRealCoords(node.location)
        }

        val minX = realLocation.xCoord
        val maxX = realLocation.xCoord + node.width
        val minY = realLocation.yCoord
        val maxY = realLocation.yCoord + node.height
        val minZ = realLocation.zCoord
        val maxZ = realLocation.zCoord + node.width

        val posX = mc.thePlayer.posX
        val posY = mc.thePlayer.posY
        val posZ = mc.thePlayer.posZ

        return posX in minX..maxX && posY in minY..maxY && posZ in minZ..maxZ
    }

    private var shouldClick = false
    private var shouldLeftClick = false

    @SubscribeEvent
    fun onPacket(event: PacketSentEventReturn) {
        if (event.packet !is C03PacketPlayer) return
        if (recentlySwapped) {
            return
        }
        if (shouldClick) {
            shouldClick = false
            airClick()
        }
        if (shouldLeftClick) {
            shouldLeftClick = false
            leftClick2()
        }
    }

    private suspend fun executeAction(node: Node) {
        val actionDelay: Int = if (node.delay == null) 0 else node.delay!!
        val room2 = currentRoom
        var yaw = node.yaw
        if (room2 != null) {
            yaw = room2.getRealYaw(node.yaw)
        }
        if (node.type == "warp" || node.type == "aotv" || node.type == "hype" || node.type == "pearl") snapTo(yaw, node.pitch)
        if (node.arguments?.contains("await") == true) awaitSecret()
        delay(actionDelay.toLong())
        node.arguments?.let {
            if ("stop" in it) MovementUtils.stopVelo()
            if ("walk" in it) MovementUtils.setKey("w", true)
            if ("look" in it) snapTo(yaw, node.pitch)
        }
        when(node.type) {
            "warp" -> {
                val state = swapFromName("aspect of the void")
                MovementUtils.setKey("shift", true)
                if (state == "SWAPPED") {
                    scheduleTask(0) {
                        shouldClick = true
                    }
                } else if (state == "ALREADY_HELD") {
                    shouldClick = true
                }
            }
            "aotv" -> {
                swapFromName("aspect of the void")
                MovementUtils.setKey("shift", false)
                scheduleTask(0) {shouldClick = true}
            }
            "hype" -> {
                swapFromName("hyperion")
                scheduleTask(0) {shouldClick = true}
            }
            "walk" -> {
                modMessage("Walking!")
                MovementUtils.setKey("shift", false)
                MovementUtils.setKey("w", true)
            }
            "jump" -> {
                modMessage("Jumping!")
                MovementUtils.jump()
            }
            "stop" -> {
                modMessage("Stopping!")
                MovementUtils.stopMovement()
                MovementUtils.stopVelo()
            }
            "boom" -> {
                modMessage("Bomb denmark!")
                if (boomType.selected == "Regular") swapFromName("superboom tnt") else swapFromName("infinityboom tnt")
                scheduleTask(0) { shouldLeftClick = true }
            }
            "pearl" -> {
                swapFromName("ender pearl")
                MovementUtils.setKey("shift", false)
                scheduleTask(0) {shouldClick = true}
            }
            "pearlclip" -> {
                modMessage("Pearl clipping!")
                if (node.depth == 0F) {
                    PearlClip.pearlClip()
                } else {
                    PearlClip.pearlClip(node.depth!!.toDouble())
                }
            }
            "look" -> {
                modMessage("Looking!")
                snapTo(yaw, node.pitch)
            }
            "align" -> {
                modMessage("Aligning!")
                mc.thePlayer.setPosition(floor(mc.thePlayer.posX) + 0.5, mc.thePlayer.posY, floor(mc.thePlayer.posZ) + 0.5)
            }
            "command" -> {
                modMessage("Sexecuting!")
                commandAny(node.command!!)
            }
        }
    }
}