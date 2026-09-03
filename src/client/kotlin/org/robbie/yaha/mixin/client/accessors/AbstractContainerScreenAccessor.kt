package org.robbie.yaha.mixin.client.accessors

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(AbstractContainerScreen::class)
interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    fun yaha_getX(): Int

    @Accessor("topPos")
    fun yaha_getY(): Int
}
