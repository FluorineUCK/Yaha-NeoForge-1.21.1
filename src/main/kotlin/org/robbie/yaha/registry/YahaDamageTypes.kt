package org.robbie.yaha.registry

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import org.robbie.yaha.Yaha

object YahaDamageTypes {
    val PAPER_PLANE: ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, Yaha.id("paper_plane"))
    val ANVIL: ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, Yaha.id("anvil"))
    val TRIDENT: ResourceKey<DamageType> = ResourceKey.create(Registries.DAMAGE_TYPE, Yaha.id("trident"))

    fun register() {}

    fun source(level: Level, key: ResourceKey<DamageType>, direct: Entity, cause: Entity?): DamageSource {
        val holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key)
        return if (cause == null) DamageSource(holder, direct) else DamageSource(holder, direct, cause)
    }
}
