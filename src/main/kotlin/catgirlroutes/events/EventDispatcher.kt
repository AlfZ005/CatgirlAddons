package catgirlroutes.events

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.EntityRemovedEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.SecretPickupEvent
import catgirlroutes.utils.Utils.containsOneOf
import catgirlroutes.utils.Utils.equalsOneOf
import catgirlroutes.utils.Utils.postAndCatch
import catgirlroutes.utils.Utils.unformattedName
import catgirlroutes.utils.dungeon.DungeonUtils.dungeonItemDrops
import catgirlroutes.utils.dungeon.DungeonUtils.inBoss
import catgirlroutes.utils.dungeon.DungeonUtils.inDungeons
import catgirlroutes.utils.dungeon.DungeonUtils.isSecret
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EventDispatcher { // I didn't come up with anything better so I'm just skibidiing odon clint :(

    @SubscribeEvent
    fun onRemoveEntity(event: EntityRemovedEvent) = with(event.entity) { // secret item
        if (inDungeons && this is EntityItem && this.entityItem?.unformattedName?.containsOneOf(dungeonItemDrops, true) != false && mc.thePlayer.getDistanceToEntity(this) <= 6)
            SecretPickupEvent.Item(this).postAndCatch()
    }

    @SubscribeEvent
    fun onPacket(event: PacketSentEvent) = with(event.packet) { // secret interact
        if (inDungeons && this is C08PacketPlayerBlockPlacement && position != null)
            SecretPickupEvent.Interact(position, mc.theWorld?.getBlockState(position)?.takeIf { isSecret(it, position) } ?: return).postAndCatch()
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is S29PacketSoundEffect && inDungeons && !inBoss && (event.packet.soundName.equalsOneOf("mob.bat.hurt", "mob.bat.death") && event.packet.volume == 0.1f)) SecretPickupEvent.Bat(event.packet).postAndCatch()
    }
}