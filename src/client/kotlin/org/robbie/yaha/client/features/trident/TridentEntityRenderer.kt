package org.robbie.yaha.client.features.trident

import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.model.TridentModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import com.mojang.math.Axis
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.trident.TridentEntity

class TridentEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<TridentEntity>(context) {
    val model = TridentModel(context.bakeLayer(ModelLayers.TRIDENT))

    override fun render(
        entity: TridentEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        matrices.pushPose()
        matrices.mulPose(Axis.YN.rotationDegrees(Mth.lerp(tickDelta, entity.yRotO, entity.yRot)))
        matrices.mulPose(Axis.XP.rotationDegrees(Mth.lerp(tickDelta, entity.xRotO, entity.xRot) - 90f))

        val vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)))
        model.renderToBuffer(matrices, vertexConsumer, light, OverlayTexture.NO_OVERLAY, -1)

        matrices.popPose()
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
    }

    override fun getTextureLocation(tridentEntity: TridentEntity) = Yaha.id("textures/entity/trident.png")
    override fun getBlockLightLevel(entity: TridentEntity, pos: BlockPos) = 15
}
