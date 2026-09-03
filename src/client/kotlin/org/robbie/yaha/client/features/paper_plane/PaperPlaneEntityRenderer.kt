package org.robbie.yaha.client.features.paper_plane

import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import com.mojang.math.Axis
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.paper_plane.PaperPlaneEntity

class PaperPlaneEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<PaperPlaneEntity>(context) {
    override fun render(
        entity: PaperPlaneEntity,
        yaw: Float,
        tickDelta: Float,
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int
    ) {
        matrices.pushPose()
        matrices.mulPose(Axis.YN.rotationDegrees(Mth.lerp(tickDelta, entity.yRotO, entity.yRot)))
        matrices.mulPose(Axis.XP.rotationDegrees(Mth.lerp(tickDelta, entity.xRotO, entity.xRot)))
        matrices.scale(0.9f/16f, 0.9f/16f, 0.9f/16f)
        matrices.translate(0f, 0f, -4f)

        val vertexConsumer = vertexConsumers.getBuffer(RenderType.entityCutout(getTextureLocation(entity)))

        val entry = matrices.last()
        val posMat = entry.pose()
        val normMat = entry.normal()

        vertex(posMat, normMat, vertexConsumer, -4, 0, -8, 0f, 0.5f, 0, 1, 0, light)
        vertex(posMat, normMat, vertexConsumer, -4, 0, 8, 1f, 0.5f, 0, 1, 0, light)
        vertex(posMat, normMat, vertexConsumer, 4, 0, 8, 1f, 0f, 0, 1, 0, light)
        vertex(posMat, normMat, vertexConsumer, 4, 0, -8, 0f, 0f, 0, 1, 0, light)

        vertex(posMat, normMat, vertexConsumer, -4, 0, -8, 0f, 0.5f, 0, -1, 0, light)
        vertex(posMat, normMat, vertexConsumer, 4, 0, -8, 0f, 0f, 0, -1, 0, light)
        vertex(posMat, normMat, vertexConsumer, 4, 0, 8, 1f, 0f, 0, -1, 0, light)
        vertex(posMat, normMat, vertexConsumer, -4, 0, 8, 1f, 0.5f, 0, -1, 0, light)

        vertex(posMat, normMat, vertexConsumer, 0, -3, -8, 0f, 11f/16f, 1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, 0, -8, 0f, 0.5f, 1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, 0, 8, 1f, 0.5f, 1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, -3, 8, 1f, 11f/16f, 1, 0, 0, light)

        vertex(posMat, normMat, vertexConsumer, 0, -3, -8, 0f, 11f/16f, -1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, -3, 8, 1f, 11f/16f, -1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, 0, 8, 1f, 0.5f, -1, 0, 0, light)
        vertex(posMat, normMat, vertexConsumer, 0, 0, -8, 0f, 0.5f, -1, 0, 0, light)

        matrices.popPose()
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
    }

    fun vertex(
        positionMatrix: Matrix4f,
        normalMatrix: Matrix3f,
        vertexConsumer: VertexConsumer,
        x: Int, y: Int, z: Int,
        u: Float, v: Float,
        normalX: Int, normalY: Int, normalZ: Int,
        light: Int
    ) {
        vertexConsumer.addVertex(positionMatrix, x.toFloat(), y.toFloat(), z.toFloat())
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(normalX.toFloat(), normalY.toFloat(), normalZ.toFloat())
    }

    override fun getTextureLocation(entity: PaperPlaneEntity) = Yaha.id("textures/entity/paper_plane.png")
    override fun getBlockLightLevel(entity: PaperPlaneEntity, pos: BlockPos) = 15
}
