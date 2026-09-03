package org.robbie.yaha.features.trident

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.world.phys.Vec3

object OpTrident : SpellAction {
    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getVec3(0, argc)
        env.assertVecInRange(pos)
        return SpellAction.Result(
            Spell(pos),
            MediaConstants.CRYSTAL_UNIT,
            listOf(ParticleSpray.cloud(pos, 1.0))
        )
    }

    private data class Spell(val pos: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {}
        override fun cast(env: CastingEnvironment, image: CastingImage): CastingImage {
            val trident = TridentEntity(
                env.world,
                env.castingEntity,
                pos
            )
            env.world.addFreshEntity(trident)
            return image.copy(stack = image.stack.appended(EntityIota(trident)))
        }
    }
}
