package org.robbie.yaha.client.features.anvil

import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.BlockPos
import org.robbie.yaha.Yaha
import org.robbie.yaha.client.registry.YahaEntitiesClient
import org.robbie.yaha.features.anvil.AnvilEntity


class AnvilEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<AnvilEntity>(context) {
    val model: AnvilEntityModel<AnvilEntity> = AnvilEntityModel(context.bakeLayer(YahaEntitiesClient.ANVIL))

    override fun render(
        entity: AnvilEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        matrices.pushPose()
        matrices.scale(-1f, -1f, 1f)

        val vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)))
        model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -1)

        matrices.popPose()
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
    }

    override fun getTextureLocation(entity: AnvilEntity) = Yaha.id("textures/entity/anvil.png")
    override fun getBlockLightLevel(entity: AnvilEntity, pos: BlockPos) = 15
}
