package org.robbie.yaha.client

import net.minecraft.client.Minecraft
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.common.NeoForge
import org.robbie.yaha.Yaha
import org.robbie.yaha.client.features.anvil.AnvilEntityModel
import org.robbie.yaha.client.features.anvil.AnvilEntityRenderer
import org.robbie.yaha.client.features.bundles.IotaBundleTooltipComponent
import org.robbie.yaha.client.features.bundles.IotaBundleTooltipHandler
import org.robbie.yaha.client.features.paper_plane.PaperPlaneEntityRenderer
import org.robbie.yaha.client.features.trident.TridentEntityRenderer
import org.robbie.yaha.client.registry.YahaEntitiesClient
import org.robbie.yaha.client.registry.YahaItemsClient
import org.robbie.yaha.features.bundles.IotaBundleTooltipData
import org.robbie.yaha.registry.YahaEntities

object YahaClient {
    private const val VALIDATE_CLIENT_HOOKS_PROPERTY = "yaha.probe.validateClientHooks"
    private const val EXIT_AFTER_CLIENT_STARTUP_PROPERTY = "yaha.probe.exitAfterClientStartup"
    private var setupHookRegistered = false
    private var renderersRegistered = false
    private var layerDefinitionsRegistered = false
    private var tooltipComponentsRegistered = false
    private var probeExitTicks = 0

    fun register(modBus: IEventBus) {
        modBus.addListener(YahaClient::clientSetup)
        modBus.addListener(YahaClient::registerRenderers)
        modBus.addListener(YahaClient::registerLayerDefinitions)
        modBus.addListener(YahaClient::registerTooltipComponents)
        if (java.lang.Boolean.getBoolean(VALIDATE_CLIENT_HOOKS_PROPERTY)) {
            Yaha.LOGGER.info("[YAHA-PROBE] client_mod_bus_hooks=PASS events=FMLClientSetupEvent,EntityRenderersEvent.RegisterRenderers,EntityRenderersEvent.RegisterLayerDefinitions,RegisterClientTooltipComponentFactoriesEvent")
        }
    }

    private fun clientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            YahaItemsClient.registerItemProperties()
            NeoForge.EVENT_BUS.addListener(YahaClient::beforeMouseScroll)
            NeoForge.EVENT_BUS.addListener(YahaClient::beforeRender)
            NeoForge.EVENT_BUS.addListener(YahaClient::afterClientTick)
            setupHookRegistered = true
            if (java.lang.Boolean.getBoolean(VALIDATE_CLIENT_HOOKS_PROPERTY)) {
                Yaha.LOGGER.info("[YAHA-PROBE] client_setup=PASS events=ScreenEvent.MouseScrolled.Pre,ScreenEvent.Render.Pre,ClientTickEvent.Post")
            }
        }
    }

    private fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(YahaEntities.PAPER_PLANE_ENTITY.get(), ::PaperPlaneEntityRenderer)
        event.registerEntityRenderer(YahaEntities.TIME_BOMB_ENTITY.get()) { context -> net.minecraft.client.renderer.entity.ThrownItemRenderer(context, 2f, true) }
        event.registerEntityRenderer(YahaEntities.ANVIL_ENTITY.get(), ::AnvilEntityRenderer)
        event.registerEntityRenderer(YahaEntities.TRIDENT_ENTITY.get(), ::TridentEntityRenderer)
        renderersRegistered = true
    }

    private fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(YahaEntitiesClient.ANVIL, AnvilEntityModel.Companion::getTexturedMeshDefinition)
        layerDefinitionsRegistered = true
    }

    private fun registerTooltipComponents(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(IotaBundleTooltipData::class.java) { data -> IotaBundleTooltipComponent(data) }
        tooltipComponentsRegistered = true
    }

    private fun beforeMouseScroll(event: ScreenEvent.MouseScrolled.Pre) {
        IotaBundleTooltipHandler.beforeMouseScroll(
            event.screen,
            event.mouseX,
            event.mouseY,
            event.scrollDeltaX,
            event.scrollDeltaY
        )
    }

    private fun beforeRender(event: ScreenEvent.Render.Pre) {
        IotaBundleTooltipHandler.beforeRender(
            event.screen,
            event.guiGraphics,
            event.mouseX,
            event.mouseY,
            event.partialTick
        )
    }

    private fun afterClientTick(event: ClientTickEvent.Post) {
        if (!java.lang.Boolean.getBoolean(EXIT_AFTER_CLIENT_STARTUP_PROPERTY)) {
            return
        }
        probeExitTicks++
        if (probeExitTicks == 120) {
            val client = Minecraft.getInstance()
            val screenName = client.screen?.javaClass?.name ?: "null"
            val hooksOk = setupHookRegistered &&
                renderersRegistered &&
                layerDefinitionsRegistered &&
                tooltipComponentsRegistered
            if (hooksOk) {
                Yaha.LOGGER.info(
                    "[YAHA-PROBE] client_startup_exit=PASS ticks={} screen={} setup={} renderers={} layers={} tooltip={}",
                    probeExitTicks,
                    screenName,
                    setupHookRegistered,
                    renderersRegistered,
                    layerDefinitionsRegistered,
                    tooltipComponentsRegistered,
                )
            } else {
                Yaha.LOGGER.error(
                    "[YAHA-PROBE] client_startup_exit=FAIL ticks={} screen={} setup={} renderers={} layers={} tooltip={}",
                    probeExitTicks,
                    screenName,
                    setupHookRegistered,
                    renderersRegistered,
                    layerDefinitionsRegistered,
                    tooltipComponentsRegistered,
                )
            }
            client.stop()
        }
    }
}
