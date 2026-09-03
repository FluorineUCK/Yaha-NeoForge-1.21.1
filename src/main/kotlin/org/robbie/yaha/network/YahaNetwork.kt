package org.robbie.yaha.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.bundles.BundleSelection
import org.robbie.yaha.features.bundles.IotaHolderBundle

data class BundleSelectionPayload(val selected: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<BundleSelectionPayload> =
            CustomPacketPayload.Type(Yaha.id("bundle_selection"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BundleSelectionPayload> =
            ByteBufCodecs.VAR_INT
                .cast<RegistryFriendlyByteBuf>()
                .map(::BundleSelectionPayload, BundleSelectionPayload::selected)
    }
}

object YahaNetwork {
    fun registerPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        event.registrar("1")
            .playToServer(
                BundleSelectionPayload.TYPE,
                BundleSelectionPayload.STREAM_CODEC,
                YahaNetwork::handleBundleSelection
            )
    }

    fun sendBundleSelectionToServer(selected: Int) {
        val clamped = selected.coerceIn(0, IotaHolderBundle.MAX_COUNT - 1)
        try {
            PacketDistributor.sendToServer(BundleSelectionPayload(clamped))
        } catch (_: IllegalStateException) {
            // No active client connection yet.
        } catch (_: NullPointerException) {
            // PacketDistributor requires Minecraft#getConnection to exist.
        }
    }

    fun applyBundleSelection(player: Player, selected: Int) {
        BundleSelection.set(player, selected)
    }

    private fun handleBundleSelection(payload: BundleSelectionPayload, context: IPayloadContext) {
        val player = context.player()
        if (player is ServerPlayer) {
            applyBundleSelection(player, payload.selected)
        }
    }
}
