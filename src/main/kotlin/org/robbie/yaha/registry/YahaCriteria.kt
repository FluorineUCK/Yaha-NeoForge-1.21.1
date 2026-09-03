package org.robbie.yaha.registry

import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import org.robbie.yaha.Yaha
import java.util.function.Supplier

object YahaCriteria {
    private val TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Yaha.MOD_ID)

    val COLLIDE_PLANES = RegisteredPlayerTrigger("collide_planes")
    val SUSCEPTION = RegisteredPlayerTrigger("susception")
    val BOMB_DEFUSAL = RegisteredPlayerTrigger("bomb_defusal")

    fun register(modBus: IEventBus) {
        TRIGGERS.register(modBus)
    }

    class RegisteredPlayerTrigger(name: String) {
        private val holder = TRIGGERS.register(name, Supplier { PlayerTrigger() })

        fun trigger(player: ServerPlayer) {
            holder.get().trigger(player)
        }
    }
}
