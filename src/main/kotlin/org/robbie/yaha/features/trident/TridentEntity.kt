package org.robbie.yaha.features.trident

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.robbie.yaha.YahaUtils
import org.robbie.yaha.features.paper_plane.PaperPlaneEntity
import org.robbie.yaha.registry.YahaDamageTypes
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaSounds
import kotlin.math.pow

const val MAX_AGE = 600
const val GRAVITY = -0.05
const val DRAG = 0.99

class TridentEntity(
    entityType: EntityType<out TridentEntity>,
    world: Level
) : Projectile(entityType, world) {
    constructor(
        world: Level,
        owner: Entity?,
        pos: Vec3
    ) : this(YahaEntities.TRIDENT_ENTITY.get(), world) {
        setOwner(owner)
        setPos(pos)
    }

    val piercedEntities = hashSetOf<Int>()

    override fun tick() {
        super.tick()

        if (!level().isClientSide && tickCount > MAX_AGE) shatter()

        var velocity = deltaMovement
        if (velocity.lengthSqr() != 0.0) YahaUtils.pitchYawFromRotVec(velocity)?.let {
            setXRot(it.first)
            setYRot(it.second)
        }

        velocity = velocity.scale(DRAG)
        if (!isNoGravity) velocity = velocity.add(0.0, GRAVITY, 0.0)
        setPos(position().add(velocity))
        deltaMovement = velocity

        while (!isRemoved) {
            val hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity)
            if (hitResult.type == HitResult.Type.MISS) break
            onHit(hitResult)
        }

        checkInsideBlocks()
    }

    override fun onHitBlock(blockHitResult: BlockHitResult) {
        shatter()
    }

    override fun onHitEntity(entityHitResult: EntityHitResult) {
        val entity = entityHitResult.entity

        playSound(YahaSounds.TRIDENT_HIT.get(), 1.0f, 1.0f + 0.2f * random.nextFloat())
        spawnParticles()

        val velocity = deltaMovement
        val damage = 20 - 20 * (velocity.lengthSqr() / 15 + 1).pow(-2)
        if (entity !is Projectile) {
            entity.hurt(YahaDamageTypes.source(
                level(),
                YahaDamageTypes.TRIDENT,
                this,
                owner
            ), damage.toFloat())
        }
        entity.deltaMovement = velocity.reverse()

        piercedEntities.add(entity.id)
        if (piercedEntities.size == 3) shatter()
    }

    private fun shatter() {
        playSound(YahaSounds.TRIDENT_SHATTER.get(), 1.0f, 1.0f + 0.2f * random.nextFloat())
        spawnParticles()
        discard()
    }

    private fun spawnParticles() {
        (level() as? ServerLevel)?.let {
            val particleParam = ItemParticleOption(ParticleTypes.ITEM, ItemStack(Items.AMETHYST_BLOCK, 1))
            it.sendParticles(
                particleParam,
                x, y, z,
                8,
                0.0, 0.0, 0.0,
                0.1
            )
        }
    }

    override fun canHitEntity(entity: Entity) = super.canHitEntity(entity)
            && !piercedEntities.contains(entity.id)
            && (entity !is PaperPlaneEntity || entity.owner != owner)
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}
}
