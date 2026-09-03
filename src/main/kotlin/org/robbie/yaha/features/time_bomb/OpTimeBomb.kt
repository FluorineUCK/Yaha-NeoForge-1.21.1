package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.getLong
import at.petrak.hexcasting.api.casting.getPositiveDouble
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.utils.TreeList
import net.minecraft.world.phys.Vec3

object OpTimeBomb : SpellAction {
    override val argc = 4

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getVec3(0, argc)
        env.assertVecInRange(pos)
        val hex = args.getList(1, argc)
        val media = (MediaConstants.DUST_UNIT * args.getPositiveDouble(2, argc)).toLong()
        val lifetime = args.getLong(3, argc).toInt()
        return SpellAction.Result(
            Spell(pos, hex, media, lifetime),
            media + MediaConstants.CRYSTAL_UNIT,
            listOf(ParticleSpray.cloud(pos, 1.0))
        )
    }

    private data class Spell(val pos: Vec3, val hex: TreeList<Iota>, val media: Long, val lifetime: Int) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {}
        override fun cast(env: CastingEnvironment, image: CastingImage): CastingImage? {
            val bomb = TimeBombEntity(
                env.world,
                env.castingEntity,
                hex.toList(),
                media,
                env.pigment,
                lifetime,
                pos
            )
            env.world.addFreshEntity(bomb)
            return image.copy(stack = image.stack.appended(EntityIota(bomb)))
        }
    }
}
