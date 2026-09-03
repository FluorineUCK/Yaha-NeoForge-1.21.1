package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.CastResult
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.eval.sideeffects.OperatorSideEffect
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.GameType
import java.util.function.Predicate

class TimeBombCastEnv(world: ServerLevel, private val bomb: TimeBombEntity) : CastingEnvironment(world) {
    private val AMBIT = 8.0
    private val player = bomb.owner as LivingEntity?

    override fun getCastingEntity() = player

    override fun getMishapEnvironment() = TimeBombMishapEnv(bomb, world)

    override fun postExecution(result: CastResult?) {
        super.postExecution(result)
        if (player !is ServerPlayer || result == null) return

        for (sideEffect in result.sideEffects) if (sideEffect is OperatorSideEffect.DoMishap) {
            val msg = sideEffect.mishap.errorMessageWithName(this, sideEffect.errorCtx)
            msg?.let(::printMessage)
        }
    }

    override fun mishapSprayPos(): Vec3 = bomb.position()

    override fun extractMediaEnvironment(cost: Long, simulate: Boolean): Long {
        val extracted = minOf(cost, bomb.getMedia())
        if (!simulate) bomb.setMedia(bomb.getMedia() - extracted)
        return cost - extracted
    }

    override fun isVecInRangeEnvironment(vec: Vec3) =
        vec.distanceToSqr(bomb.position()) <= AMBIT * AMBIT + 0.00000000001

    override fun hasEditPermissionsAtEnvironment(pos: BlockPos): Boolean {
        if (player !is ServerPlayer) return false
        return player.gameMode.gameModeForPlayer != GameType.ADVENTURE && player.mayInteract(world, pos)
    }

    override fun getCastingHand() = InteractionHand.MAIN_HAND

    override fun getUsableStacks(mode: StackDiscoveryMode): List<ItemStack> {
        if (player !is ServerPlayer) return mutableListOf()
        return getUsableStacksForPlayer(mode, null, player)
    }

    override fun getPrimaryStacks(): List<HeldItemInfo> {
        if (player !is ServerPlayer) return mutableListOf()
        return getPrimaryStacksForPlayer(castingHand, player)
    }

    override fun replaceItem(
        stackOk: Predicate<ItemStack>?,
        replaceWith: ItemStack?,
        hand: InteractionHand?
    ): Boolean {
        if (player !is ServerPlayer) return false
        return  replaceItemForPlayer(stackOk, replaceWith, hand, player)
    }

    override fun getPigment(): FrozenPigment = bomb.pigment

    override fun setPigment(pigment: FrozenPigment?): FrozenPigment? {
        if (pigment != null) bomb.pigment = pigment
        return pigment
    }

    override fun produceParticles(
        particles: ParticleSpray,
        colorizer: FrozenPigment
    ) {
        particles.sprayParticles(world, colorizer)
    }

    override fun printMessage(message: Component) {
        if (player is ServerPlayer) player.sendSystemMessage(message)
    }

    fun getBomb() = bomb
}
