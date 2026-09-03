package org.robbie.yaha.client.features.anvil

import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.world.entity.Entity

class AnvilEntityModel<T : Entity>(private val root: ModelPart) : HierarchicalModel<T>() {
    override fun root() = root
    override fun setupAnim(
        entity: T,
        limbAngle: Float,
        limbDistance: Float,
        animationProgress: Float,
        headYaw: Float,
        headPitch: Float
    ) {}

    companion object {
        fun getTexturedMeshDefinition(): LayerDefinition {
            val modelData = MeshDefinition()
            val modelPartData = modelData.root

            modelPartData.addOrReplaceChild(
                "main",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-5f, -16f, -8f, 10f, 6f, 16f)
                    .texOffs(0, 22)
                    .addBox(-2f, -10f, -4f, 4f, 5f, 8f)
                    .texOffs(0, 35)
                    .addBox(-4f, -5f, -5f, 8f, 1f, 10f)
                    .texOffs(0, 46)
                    .addBox(-6f, -4f, -6f, 12f, 4f, 12f),
                PartPose.ZERO
            )

            return LayerDefinition.create(modelData, 64, 64)
        }
    }
}
