package org.robbie.yaha.features.paper_plane

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.EntityType
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.robbie.yaha.YahaUtils
import org.robbie.yaha.registry.YahaCriteria
import org.robbie.yaha.registry.YahaDamageTypes
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaSounds
import java.util.UUID

const val ACCELERATION = 0.1
const val DRAG = 0.9
const val MAX_AGE = 200

class PaperPlaneEntity(
    entityType: EntityType<out PaperPlaneEntity>,
    world: Level
) : Projectile(entityType, world) {
    constructor(
        world: Level,
        owner: Entity?,
        target: Entity?,
        pos: Vec3
    ) : this(YahaEntities.PAPER_PLANE_ENTITY.get(), world) {
        setOwner(owner)
        setTarget(target)
        setPos(pos)

        target ?: return
        YahaUtils.pitchYawFromRotVec(target.position().subtract(pos))?.let {
            setXRot(it.first)
            setYRot(it.second)
        }
    }

    private var target: Entity? = null
    private var targetUUID: UUID? = null

    override fun tick() {
        super.tick()

        if (!level().isClientSide && tickCount > MAX_AGE) shatter()

        // check collision
        val hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity)
        if (hitResult.type != HitResult.Type.MISS) onHit(hitResult)

        // update position, rotation, and velocity
        val velocity = deltaMovement
        YahaUtils.pitchYawFromRotVec(velocity)?.let {
            setXRot(it.first)
            setYRot(it.second)
        }

        setPos(position().add(velocity))
        val accelDirection = getTarget()?.let{
            it.eyePosition.add(it.deltaMovement).subtract(position())
        } ?: lookAngle
        deltaMovement = deltaMovement
            .add(accelDirection.normalize().scale(ACCELERATION))
            .scale(DRAG)

        checkInsideBlocks() // other minecraft projectiles seem to call both onHit AND block checks
    }

    override fun canHitEntity(entity: Entity) = super.canHitEntity(entity) && (entity !is PaperPlaneEntity || target == entity)
    override fun isPickable() = true

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (isInvulnerableTo(source)) return false
        val entity = source.entity ?: return false

        if (!level().isClientSide) {
            deltaMovement = entity.lookAngle.scale(2.0)
            setOwner(entity)
            setTarget(null)
        }

        return true
    }

    fun getTarget(): Entity? {
        // also updates the target accordingly if it is null or removed

        target?.let {
            if (it.isRemoved) setTarget(null)
        } ?: if (targetUUID != null && level() is ServerLevel) {
            setTarget((level() as ServerLevel).getEntity(targetUUID))
        } else setTarget(null)

        return target
    }

    fun setTarget(entity: Entity?) {
        target = entity
        targetUUID = entity?.uuid
    }

    override fun onHitBlock(blockHitResult: BlockHitResult) {
        shatter()
    }

    override fun onHitEntity(entityHitResult: EntityHitResult) {
        val entity = entityHitResult.entity
        entity.hurt(YahaDamageTypes.source(level(), YahaDamageTypes.PAPER_PLANE, this, owner), 6f)
        if (entity is PaperPlaneEntity && owner is ServerPlayer)
            YahaCriteria.COLLIDE_PLANES.trigger(owner as ServerPlayer)

        shatter()
    }

    private fun shatter() {
        (level() as? ServerLevel)?.let {
            playSound(YahaSounds.PLANE_SHATTER.get(), 1.0f, 1.0f + 0.2f * random.nextFloat())
            val particleParam = ItemParticleOption(ParticleTypes.ITEM, ItemStack(Items.AMETHYST_BLOCK, 1))
            it.sendParticles(
                particleParam,
                x, y, z,
                8,
                0.0, 0.0, 0.0,
                0.1
            )
        }
        discard()
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        targetUUID?.let { nbt.putUUID("Target", it) }
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        if (nbt.hasUUID("Target")) {
            targetUUID = nbt.getUUID("Target")
            target = null
        }
    }

    override fun isNoGravity() = true
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}
}
