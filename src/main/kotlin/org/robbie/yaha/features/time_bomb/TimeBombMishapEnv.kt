package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.eval.MishapEnvironment
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

class TimeBombMishapEnv(bomb: TimeBombEntity, world: ServerLevel) : MishapEnvironment(world, bomb.owner as? ServerPlayer) {
    override fun yeetHeldItemsTowards(targetPos: Vec3) {}

    override fun dropHeldItems() {}

    override fun drown() {}

    override fun damage(healthProportion: Float) {}

    override fun removeXp(amount: Int) {}

    override fun blind(ticks: Int) {}

    override fun nauseate(ticks: Int) {}
}
