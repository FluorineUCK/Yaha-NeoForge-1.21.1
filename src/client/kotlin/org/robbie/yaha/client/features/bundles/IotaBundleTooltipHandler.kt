package org.robbie.yaha.client.features.bundles

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import org.robbie.yaha.features.bundles.BundleSelection
import org.robbie.yaha.features.bundles.IotaHolderBundle
import org.robbie.yaha.mixin.client.accessors.AbstractContainerScreenAccessor
import org.robbie.yaha.network.YahaNetwork

object IotaBundleTooltipHandler {
    var hoveredSlot: Slot? = null
    var selected: Int = 0

    // select slot in bundle with scroll wheel
    fun beforeMouseScroll(screen: Screen, mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double) {
        if (verticalAmount == 0.0) return
        if (screen !is AbstractContainerScreen<*>) return

        val slot = hoveredSlot
        if (slot == null || !slot.hasItem()) return
        val itemStack = slot.item
        if (itemStack.item !is IotaHolderBundle) return
        val count = IotaHolderBundle.getBundleOccupancy(itemStack)
        if (count == 0) return

        syncSelected((selected + if (verticalAmount < 0) 1 else -1).mod(count))
    }

    // reset the selected slot when hovering over a new bundle
    fun beforeRender(screen: Screen, drawContext: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        if (screen !is AbstractContainerScreen<*>) return
        val slot = getHoveredSlot(screen, mouseX, mouseY)
        if (slot != hoveredSlot) syncSelected(0)
        hoveredSlot = slot
    }

    // selected is stored on this client-side object to make sure the tooltip doesnt look laggy;
    // tooltip will use IotaBundleTooltipHandler.selected while item will use CCBundleSelect.selected
    private fun syncSelected(newSelect: Int) {
        selected = newSelect
        val player = Minecraft.getInstance().player
        if (player == null) return
        BundleSelection.set(player, selected)
        YahaNetwork.sendBundleSelectionToServer(selected)
    }

    private fun getHoveredSlot(screen: AbstractContainerScreen<*>, mouseX: Int, mouseY: Int): Slot? {
        for (slot in screen.menu.slots) {
            (screen as AbstractContainerScreenAccessor).run {
                if (
                    mouseX >= yaha_getX() + slot.x - 1 &&
                    mouseX < yaha_getX() + slot.x + 17 &&
                    mouseY >= yaha_getY() + slot.y - 1 &&
                    mouseY < yaha_getY() + slot.y + 17
                ) return slot
            }
        }
        return null
    }
}
