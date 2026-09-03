package org.robbie.yaha.features.armor_stand

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.Rotations
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.Vec3
import kotlin.math.PI

class OpStandPose(val part: Part) : SpellAction {
    override val argc = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val armorStand = args.getEntity(env.world, 0, argc)
        val angles = args.getVec3(1, argc)
        env.assertEntityInRange(armorStand)
        if (armorStand !is ArmorStand) throw MishapBadEntity.of(armorStand, "yaha:armor_stand")
        return SpellAction.Result(
            Spell(armorStand, part, angles),
            MediaConstants.DUST_UNIT / 8,
            listOf(ParticleSpray.cloud(armorStand.position(), 1.0))
        )
    }

    private data class Spell(val armorStand: ArmorStand, val part: Part, val angles: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val degreeAngles = angles.scale(180.0 / PI)
            val rotations = Rotations(
                degreeAngles.x.toFloat(),
                degreeAngles.y.toFloat(),
                degreeAngles.z.toFloat()
            )
            armorStand.apply {
                when (part) {
                    Part.HEAD -> setHeadPose(rotations)
                    Part.BODY -> setBodyPose(rotations)
                    Part.LEFT_ARM -> setLeftArmPose(rotations)
                    Part.RIGHT_ARM -> setRightArmPose(rotations)
                    Part.LEFT_LEG -> setLeftLegPose(rotations)
                    Part.RIGHT_LEG -> setRightLegPose(rotations)
                }
            }
        }
    }

    enum class Part {
        HEAD,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG
    }
}
