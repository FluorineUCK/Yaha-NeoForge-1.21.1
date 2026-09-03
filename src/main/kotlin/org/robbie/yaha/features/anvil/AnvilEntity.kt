package org.robbie.yaha.features.anvil

import net.neoforged.fml.ModList
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
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
import org.robbie.yaha.compat.SpectrumCompat
import org.robbie.yaha.features.paper_plane.PaperPlaneEntity
import org.robbie.yaha.registry.YahaDamageTypes
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaSounds
import kotlin.math.pow

const val MAX_AGE = 600
const val GRAVITY = -0.04
const val DRAG = 0.98

class AnvilEntity(
    entityType: EntityType<out AnvilEntity>,
    world: Level
) : Projectile(entityType, world) {
    constructor(
        world: Level,
        owner: Entity?,
        pos: Vec3
    ) : this(YahaEntities.ANVIL_ENTITY.get(), world) {
        setOwner(owner)
        setPos(pos)
    }

    private var cooldown = 2
    private var count = 3

    override fun tick() {
        super.tick()

        if (!level().isClientSide && tickCount > MAX_AGE) shatter()
        if (cooldown != 0) cooldown--

        var velocity = deltaMovement
        YahaUtils.pitchYawFromRotVec(velocity)?.let {
            setXRot(it.first)
            setYRot(it.second)
        }
        if (!isNoGravity) velocity = velocity.add(0.0, GRAVITY, 0.0)
        setPos(position().add(velocity))
        deltaMovement = velocity.scale(DRAG)

        checkInsideBlocks()
        val hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity)
        if (hitResult.type != HitResult.Type.MISS) onHit(hitResult)
    }

    override fun canHitEntity(entity: Entity) = super.canHitEntity(entity)
            && entity !is AnvilEntity
            && (
            entity !is PaperPlaneEntity
                    || entity.owner != owner
            )

    override fun onHitBlock(blockHitResult: BlockHitResult) {
        setPos(blockHitResult.location)
        if (ModList.get().isLoaded("spectrum")) SpectrumCompat.crush(this)
        if (!level().isClientSide) shatter()
    }

    override fun onHitEntity(entityHitResult: EntityHitResult) {
        val entity = entityHitResult.entity
        if (cooldown != 0) return

        playSound(YahaSounds.ANVIL_HIT.get(), 1.0f, 1.0f + 0.2f * random.nextFloat())
        spawnParticles()

        val velocity = deltaMovement
        val damage = 20 - 20 * (velocity.lengthSqr() / 15 + 1).pow(-2)
        if (entity !is Projectile) {
            entity.hurt(YahaDamageTypes.source(
                level(),
                YahaDamageTypes.ANVIL,
                this,
                owner
            ), damage.toFloat())
        }
        val entityVelocity = entity.deltaMovement
        entity.deltaMovement = velocity
        deltaMovement = entityVelocity
        cooldown = 2

        count--
        if (count == 0 && !level().isClientSide) shatter()
    }

    private fun shatter() {
        playSound(YahaSounds.ANVIL_SHATTER.get(), 0.8f, 1.0f + 0.2f * random.nextFloat())
        spawnParticles()
        discard()
    }

    private fun spawnParticles() {
        (level() as? ServerLevel)?.let {
            val particleParam = ItemParticleOption(ParticleTypes.ITEM, ItemStack(Items.AMETHYST_BLOCK, 1))
            it.sendParticles(
                particleParam,
                x, y + 0.5, z,
                16,
                0.2, 0.2, 0.2,
                0.1
            )
        }
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        nbt.putInt("Count", count)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        count = if (nbt.contains("Count")) {
            nbt.getInt("Count")
        } else 3
    }

    override fun canCollideWith(other: Entity) = (other.isPickable || other.isPushable) && !isPassengerOfSameVehicle(other)
    override fun isPickable() = cooldown == 0
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {}
}
