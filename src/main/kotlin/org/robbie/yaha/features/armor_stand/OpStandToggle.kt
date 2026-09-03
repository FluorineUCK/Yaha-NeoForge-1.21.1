package org.robbie.yaha.features.armor_stand

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.world.entity.decoration.ArmorStand
import org.robbie.yaha.mixin.accessors.ArmorStandAccessor

class OpStandToggle(val toggle: Toggle) : SpellAction {
    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val armorStand = args.getEntity(env.world, 0, argc)
        env.assertEntityInRange(armorStand)
        if (armorStand !is ArmorStand) throw MishapBadEntity.of(armorStand, "yaha:armor_stand")
        return SpellAction.Result(
            Spell(armorStand, toggle),
            MediaConstants.DUST_UNIT / 8,
            listOf(ParticleSpray.cloud(armorStand.position(), 1.0))
        )
    }

    private data class Spell(val armorStand: ArmorStand, val toggle: Toggle) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            armorStand.apply {
                when (toggle) {
                    Toggle.SHOW_ARMS -> setShowArms(!isShowArms)
                    Toggle.HIDE_BASEPLATE -> setNoBasePlate(!isNoBasePlate)
                    Toggle.MAKE_SMALL -> (this as ArmorStandAccessor).yaha_setSmall(!isSmall)
                }
            }
        }
    }

    enum class Toggle {
        SHOW_ARMS,
        HIDE_BASEPLATE,
        MAKE_SMALL
    }
}
