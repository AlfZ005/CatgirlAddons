package catgirlroutes.module.impl.misc

import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.mixins.accessors.AccessorGuiEditSign
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.ui.misc.searchoverlay.SearchOverlay
import catgirlroutes.ui.misc.searchoverlay.OverlayType
import catgirlroutes.utils.LocationManager.inSkyblock
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object BazaarSearchOverlay : Module(
    "Bazaar Search",
    Category.MISC,
    tag = TagType.WHIP
) {

    private val auctionOverlay = BooleanSetting("Auction Overlay")
    private val bazaarOverlay = BooleanSetting("Bazaar Overlay")

    private var overlay: OverlayType = OverlayType.NONE

    init {
        addSettings(this.auctionOverlay, this.bazaarOverlay)
    }

//    override fun onEnable() {
//        display = BazaarSearchOverlay()
//        toggle()
//        super.onEnable()
//    }

    @SubscribeEvent
    fun onS2DPacketOpenWindow(event: PacketReceiveEvent) {
        if (!inSkyblock || event.packet !is S2DPacketOpenWindow) return
        val title = event.packet.windowTitle.unformattedText
        if ((title.contains("Auctions") || title.contains("Auction House")) && this.auctionOverlay.enabled) {
            overlay = OverlayType.AUCTION
        } else if (title.contains("Bazaar") && this.bazaarOverlay.enabled) {
            overlay = OverlayType.BAZAAR
        }
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui !is GuiEditSign || overlay == OverlayType.NONE) return

        val sign = (event.gui as AccessorGuiEditSign).tileSign
        sign?.let {
            event.gui = SearchOverlay(overlay, sign)
        }
    }
}