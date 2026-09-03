package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.utils.asCompound
import at.petrak.hexcasting.api.utils.getList
import at.petrak.hexcasting.api.utils.hasCompound
import at.petrak.hexcasting.api.utils.hasInt
import at.petrak.hexcasting.api.utils.hasList
import at.petrak.hexcasting.api.utils.hasLong
import at.petrak.hexcasting.api.utils.putCompound
import at.petrak.hexcasting.api.utils.putList
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.nbt.ListTag
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.robbie.yaha.YahaUtils
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaItems
import kotlin.math.absoluteValue

const val DRAG = 0.9

class TimeBombEntity(
    entityType: EntityType<out TimeBombEntity>,
    world: Level
) : ThrowableItemProjectile(entityType, world) {
    constructor(
        world: Level,
        owner: Entity?,
        hex: List<Iota>,
        media: Long,
        pigment: FrozenPigment,
        lifetime: Int,
        pos: Vec3
    ) : this(YahaEntities.TIME_BOMB_ENTITY.get(), world) {
        setOwner(owner)
        this.hex = hex
        this.media = media
        this.pigment = pigment
        this.lifetime = lifetime
        setPos(pos)
    }

    private var hex: List<Iota> = listOf()
    private var media: Long = 0
    var pigment = FrozenPigment.DEFAULT.get()
    private var lifetime = 0

    fun getMedia() = media
    fun setMedia(value: Long) {
        media = value.coerceAtLeast(0)
    }

    override fun tick() {
        super.tick()
        if (tickCount >= lifetime) explode()

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
        deltaMovement = velocity.scale(DRAG)

        checkInsideBlocks() // other minecraft projectiles seem to call both onHit AND block checks
    }

    private fun explode() {
        if (level() !is ServerLevel) return

        if (hex.isNotEmpty()) {
            val env = TimeBombCastEnv(level() as ServerLevel, this)

            var castingImage = CastingImage()
            val castingVM = CastingVM(castingImage, env)
            castingVM.queueExecuteAndWrapIotas(hex, level() as ServerLevel)
        }

        (level() as? ServerLevel)?.let {
            it.playSound(
                null,
                x, y, z,
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS,
                1.0f, 0.5f
            )
            val particleParam = ItemParticleOption(
                ParticleTypes.ITEM,
                ItemStack(Items.AMETHYST_BLOCK, 1)
            )
            it.sendParticles(
                particleParam,
                x, y, z,
                16,
                0.0, 0.0, 0.0,
                0.5
            )
        }
        discard()
    }

    override fun canHitEntity(entity: Entity) = false

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        if (isInvulnerableTo(source)) return false
        val entity = source.entity
        if (entity == null) return false

        if (!level().isClientSide) {
            deltaMovement = entity.lookAngle.scale(0.4)
        }

        return true
    }

    override fun onHitBlock(blockHitResult: BlockHitResult) {
        val normal = blockHitResult.direction.normal
        val force = deltaMovement.multiply(
            -normal.x.toDouble().absoluteValue,
            -normal.y.toDouble().absoluteValue,
            -normal.z.toDouble().absoluteValue
        )
        deltaMovement = deltaMovement.add(force.scale(1.8))
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)

        if (hex.isNotEmpty()) {
            val hexNbt = ListTag()
            hex.forEach { iota ->
                IotaType.TYPED_CODEC.encodeStart(NbtOps.INSTANCE, iota)
                    .result()
                    .ifPresent { hexNbt.add(it) }
            }
            nbt.putList("Hex", hexNbt)
        }
        nbt.putLong("Media", media)
        FrozenPigment.CODEC.encodeStart(NbtOps.INSTANCE, pigment)
            .result()
            .ifPresent { nbt.put("Pigment", it) }
        nbt.putInt("Lifetime", lifetime)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)

        hex = if (nbt.hasList("Hex") && level() is ServerLevel) {
            val hexNbt = nbt.getList("Hex", Tag.TAG_COMPOUND.toInt())
            hexNbt.mapNotNull { IotaType.TYPED_CODEC.parse(NbtOps.INSTANCE, it).result().orElse(null) }
        } else listOf()

        media = if (nbt.hasLong("Media"))
            nbt.getLong("Media")
        else 0

        pigment = if (nbt.hasCompound("Pigment"))
            FrozenPigment.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("Pigment")).result().orElse(FrozenPigment.DEFAULT.get())
        else FrozenPigment.DEFAULT.get()

        lifetime = if (nbt.hasInt("Lifetime"))
            nbt.getInt("Lifetime")
        else 0
    }

    override fun getDefaultItem() = YahaItems.TIME_BOMB.get()
    override fun isNoGravity() = true
}
