package org.robbie.yaha

import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.common.NeoForge
import org.robbie.yaha.client.YahaClient
import org.robbie.yaha.network.YahaNetwork
import org.robbie.yaha.registry.YahaActions
import org.robbie.yaha.registry.YahaCriteria
import org.robbie.yaha.registry.YahaEntities
import org.robbie.yaha.registry.YahaItems
import org.robbie.yaha.registry.YahaSounds
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Mod(Yaha.MOD_ID)
class Yaha(modBus: IEventBus) {
    init {
        YahaEntities.register(modBus)
        YahaItems.register(modBus)
        YahaSounds.register(modBus)
        modBus.addListener(YahaItems::addCreativeTabEntries)
        if (FMLEnvironment.dist.isClient) {
            YahaClient.register(modBus)
        }
        modBus.addListener(YahaNetwork::registerPayloadHandlers)
        modBus.addListener(YahaActions::register)
        YahaCriteria.register(modBus)
        NeoForge.EVENT_BUS.addListener(YahaProbeValidation::onServerStarted)
    }

    companion object {
        const val MOD_ID: String = "yaha"
        val RANDOM: RandomSource = RandomSource.create() // if world.random cannot be used
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
        fun id(string: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, string)
    }
}
