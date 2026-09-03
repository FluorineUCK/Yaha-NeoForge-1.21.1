package org.robbie.yaha.features.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.OperationResult
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.server.level.ServerPlayer
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.time_bomb.TimeBombCastEnv
import org.robbie.yaha.features.time_bomb.TimeBombEntity
import org.robbie.yaha.registry.YahaCriteria

object OpPotionToItem : SpellAction {
    private val PLUCKABLE_PROJECTILES: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, Yaha.id("potion_to_item"))

    override val argc = 1

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val entity = args.getEntity(env.world, 0, argc)
        val projectile = entity as? ThrowableItemProjectile

        if (
            projectile == null ||
            !entity.type.`is`(PLUCKABLE_PROJECTILES) ||
            !isOwnedByCaster(projectile, env)
        ) throw MishapBadEntity.of(entity, "yaha:potion")

        env.assertEntityInRange(entity)

        return SpellAction.Result(
            Spell(projectile),
            MediaConstants.CRYSTAL_UNIT,
            listOf(ParticleSpray.cloud(entity.position(), 1.0))
        )
    }

    /**
     * Easter Egg Specification:
     * Normally, this spell takes a PotionEntity and replaces it with an ItemEntity of the same potion.
     * However, if one were to cast it on a Time Bomb (that they own),
     * it should drop itself as an item AND end it's running hex (like Janus from Overevaluate).
     */
    override fun operate(
        env: CastingEnvironment,
        image: CastingImage,
        continuation: SpellContinuation
    ): OperationResult {
        // jank levels spiking!!
        val stackTop = image.stack.lastOrNull()
        val stackEntity = (stackTop as? EntityIota)?.getEntity(env.world)
        val isBomb = (
                stackTop is EntityIota &&
                stackEntity is TimeBombEntity
                )
        val isSelfCast = (
                isBomb &&
                env is TimeBombCastEnv &&
                env.getBomb() == stackEntity
                )

        val opResult = super.operate(env, image, continuation)

        if (env.castingEntity is ServerPlayer && isBomb)
            YahaCriteria.BOMB_DEFUSAL.trigger(env.castingEntity as ServerPlayer)

        return if (!isSelfCast) opResult else opResult.copy(newContinuation = SpellContinuation.Done)
    }

    private data class Spell(val potion: ThrowableItemProjectile) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val pos = potion.position()
            val vel = potion.deltaMovement
            val item = potion.item
            potion.discard()
            val itemEntity = ItemEntity(
                env.world,
                pos.x, pos.y, pos.z,
                item,
                vel.x, vel.y, vel.z
            )
            itemEntity.setDefaultPickUpDelay()
            env.world.addFreshEntity(itemEntity)
        }
    }

    private fun isOwnedByCaster(projectile: ThrowableItemProjectile, env: CastingEnvironment): Boolean {
        val caster = env.castingEntity ?: return false
        if (projectile.owner == caster) {
            return true
        }

        val tag = CompoundTag()
        projectile.saveWithoutId(tag)
        return tag.hasUUID("Owner") && tag.getUUID("Owner") == caster.uuid
    }
}
