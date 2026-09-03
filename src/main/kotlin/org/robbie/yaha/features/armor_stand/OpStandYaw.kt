package org.robbie.yaha.features.armor_stand

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getDouble
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.math.PI

object OpStandYaw : SpellAction {
    override val argc = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val armorStand = args.getEntity(env.world, 0, argc)
        val yaw = args.getDouble(1, argc)
        env.assertEntityInRange(armorStand)
        if (armorStand !is ArmorStand) throw MishapBadEntity.of(armorStand, "yaha:armor_stand")
        return SpellAction.Result(
            Spell(armorStand, yaw),
            MediaConstants.DUST_UNIT / 8,
            listOf(ParticleSpray.cloud(armorStand.position(), 1.0))
        )
    }

    private data class Spell(val armorStand: ArmorStand, val yaw: Double) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            armorStand.setYRot((yaw * 180.0 / PI).toFloat() % 360)
        }
    }
}
