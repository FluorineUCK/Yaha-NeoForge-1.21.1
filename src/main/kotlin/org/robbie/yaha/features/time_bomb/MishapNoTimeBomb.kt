package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.TreeList
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.Level

class MishapNoTimeBomb : Mishap() {
    override fun accentColor(
        ctx: CastingEnvironment,
        errorCtx: Context
    ): FrozenPigment = dyeColor(DyeColor.LIGHT_BLUE)

    override fun errorMessage(
        ctx: CastingEnvironment,
        errorCtx: Context
    ) = error("yaha:no_time_bomb")

    override fun execute(
        env: CastingEnvironment,
        errorCtx: Context,
        stack: TreeList<Iota>
    ): TreeList<Iota> {
        val pos = env.castingEntity?.position() ?: return stack
        env.world.explode(null, pos.x, pos.y, pos.z, 0.25f, Level.ExplosionInteraction.NONE)
        return stack
    }
}
