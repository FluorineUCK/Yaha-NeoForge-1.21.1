package org.robbie.yaha.registry

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.common.items.storage.ItemFocus
import at.petrak.hexcasting.common.items.storage.ItemThoughtKnot
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.bundles.IotaHolderBundle
import java.util.function.Supplier

object YahaItems {
    private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(Yaha.MOD_ID)

    val TIME_BOMB: DeferredItem<Item> = ITEMS.register("time_bomb", Supplier {
        Item(
            Item.Properties()
                .stacksTo(1)
                .food(FoodProperties.Builder().nutrition(2).saturationModifier(0.1f).alwaysEdible().build())
                .rarity(Rarity.UNCOMMON)
        )
    })
    val SPINDLE: DeferredItem<IotaHolderBundle> = ITEMS.register("spindle", Supplier {
        IotaHolderBundle(Item.Properties().stacksTo(1), { it is ItemThoughtKnot })
    })
    val POUCH: DeferredItem<IotaHolderBundle> = ITEMS.register("pouch", Supplier {
        IotaHolderBundle(Item.Properties().stacksTo(1), { it is ItemFocus })
    })

    // uncomment when uhhhh 10 items in addon
    // val YAHA_GROUP = FabricItemGroup.builder()
    //     .icon { TIME_BOMB.defaultStack }
    //     .displayName(Text.translatable("itemGroup.yaha.yaha"))
    //     .entries { _, entries ->
    //         entries.add(TIME_BOMB.defaultStack)
    //         entries.add(SPINDLE.defaultStack)
    //         entries.add(POUCH.defaultStack)
    //     }
    //     .build()

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
    }

    fun addCreativeTabEntries(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey == ResourceKey.create(Registries.CREATIVE_MODE_TAB, HexAPI.modLoc("hexcasting"))) {
            event.accept(TIME_BOMB)
            event.accept(SPINDLE)
            event.accept(POUCH)
        }
    }
}
