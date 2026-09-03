package org.robbie.yaha.registry

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.anvil.AnvilEntity
import org.robbie.yaha.features.paper_plane.PaperPlaneEntity
import org.robbie.yaha.features.time_bomb.TimeBombEntity
import org.robbie.yaha.features.trident.TridentEntity
import java.util.function.Supplier

object YahaEntities {
    private val ENTITIES: DeferredRegister<EntityType<*>> = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Yaha.MOD_ID)

    val PAPER_PLANE_ENTITY: DeferredHolder<EntityType<*>, EntityType<PaperPlaneEntity>> = ENTITIES.register("paper_plane", Supplier {
        EntityType.Builder.of(EntityType.EntityFactory<PaperPlaneEntity> { type, level -> PaperPlaneEntity(type, level) }, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .build(Yaha.MOD_ID + ":paper_plane")
    })

    val TIME_BOMB_ENTITY: DeferredHolder<EntityType<*>, EntityType<TimeBombEntity>> = ENTITIES.register("time_bomb", Supplier {
        EntityType.Builder.of(EntityType.EntityFactory<TimeBombEntity> { type, level -> TimeBombEntity(type, level) }, MobCategory.MISC)
            .sized(0.75f, 0.75f)
            .build(Yaha.MOD_ID + ":time_bomb")
    })

    val ANVIL_ENTITY: DeferredHolder<EntityType<*>, EntityType<AnvilEntity>> = ENTITIES.register("anvil", Supplier {
        EntityType.Builder.of(EntityType.EntityFactory<AnvilEntity> { type, level -> AnvilEntity(type, level) }, MobCategory.MISC)
            .sized(1f, 1f)
            .build(Yaha.MOD_ID + ":anvil")
    })

    val TRIDENT_ENTITY: DeferredHolder<EntityType<*>, EntityType<TridentEntity>> = ENTITIES.register("trident", Supplier {
        EntityType.Builder.of(EntityType.EntityFactory<TridentEntity> { type, level -> TridentEntity(type, level) }, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .build(Yaha.MOD_ID + ":trident")
    })

    fun register(modBus: IEventBus) {
        ENTITIES.register(modBus)
    }
}
