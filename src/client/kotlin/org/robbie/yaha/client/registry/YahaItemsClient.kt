package org.robbie.yaha.client.registry

import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.resources.ResourceLocation
import org.robbie.yaha.features.bundles.IotaHolderBundle
import org.robbie.yaha.registry.YahaItems

object YahaItemsClient {
    fun registerItemProperties() {
        ItemProperties.register(
            YahaItems.SPINDLE.get(),
            ResourceLocation.withDefaultNamespace("filled")
        ) { itemStack, clientLevel, livingEntity, seed ->
            IotaHolderBundle.getBundleOccupancy(itemStack).toFloat() / IotaHolderBundle.MAX_COUNT.toFloat()
        }

        ItemProperties.register(
            YahaItems.POUCH.get(),
            ResourceLocation.withDefaultNamespace("filled")
        ) { itemStack, clientLevel, livingEntity, seed ->
            IotaHolderBundle.getBundleOccupancy(itemStack).toFloat() / IotaHolderBundle.MAX_COUNT.toFloat()
        }
    }
}
