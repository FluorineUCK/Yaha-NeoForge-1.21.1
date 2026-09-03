package org.robbie.yaha.features.bundles

import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.core.NonNullList

data class IotaBundleTooltipData(val inventory: NonNullList<ItemStack>) : TooltipComponent
