package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.utils.ChatUtils.modMessage
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.cos
import kotlin.math.sin

object InstantSprint: Module(
    name = "Instant Sprint",
    category = Category.MISC
) {
    private val forwarKeybing = (mc.gameSettings.keyBindForward)
    var veloSet = false


    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!enabled) return
        val multiplier = if (mc.thePlayer?.isSneaking == true) {
            0.3
        } else {
            1.0
        }
        if (forwarKeybing.isPressed) {
            modMessage("NIGGER")
            if (veloSet) return
            veloSet = true
            if(!mc.thePlayer.onGround) return
            val yaw = mc.thePlayer.rotationYaw
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806 * multiplier
            val radians = yaw * Math.PI / 180 // todo: MathUtils?
            val x = -sin(radians) * speed
            val z = cos(radians) * speed
            mc.thePlayer.motionX = x
            mc.thePlayer.motionZ = z
        } else {
            veloSet = false
        }
    }
}