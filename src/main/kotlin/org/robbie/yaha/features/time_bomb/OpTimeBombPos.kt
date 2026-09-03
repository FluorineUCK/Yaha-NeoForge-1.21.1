package org.robbie.yaha.features.time_bomb

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota

object OpTimeBombPos : ConstMediaAction {
    override val argc = 0

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        if (env !is TimeBombCastEnv) throw MishapNoTimeBomb()
        return env.getBomb().position().asActionResult
    }
}
