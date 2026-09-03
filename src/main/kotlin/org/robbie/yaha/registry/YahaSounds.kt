package org.robbie.yaha.registry

import net.minecraft.sounds.SoundEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import org.robbie.yaha.Yaha
import java.util.function.Supplier

object YahaSounds {
    private val SOUNDS: DeferredRegister<SoundEvent> = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Yaha.MOD_ID)

    val PLANE_SHATTER = register("plane_shatter")
    val ANVIL_HIT = register("anvil_hit")
    val ANVIL_SHATTER = register("anvil_shatter")
    val TRIDENT_HIT = register("trident_hit")
    val TRIDENT_SHATTER = register("trident_shatter")

    fun register(modBus: IEventBus) {
        SOUNDS.register(modBus)
    }

    private fun register(name: String): DeferredHolder<SoundEvent, SoundEvent> {
        return SOUNDS.register(name, Supplier { SoundEvent.createVariableRangeEvent(Yaha.id(name)) })
    }
}
