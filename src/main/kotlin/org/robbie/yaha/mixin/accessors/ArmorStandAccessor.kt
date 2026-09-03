package org.robbie.yaha.mixin.accessors

import net.minecraft.world.entity.decoration.ArmorStand
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Invoker

@Mixin(ArmorStand::class)
interface ArmorStandAccessor {
    @Invoker("setSmall")
    fun yaha_setSmall(small: Boolean)
}
