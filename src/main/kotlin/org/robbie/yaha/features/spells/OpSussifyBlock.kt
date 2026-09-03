package org.robbie.yaha.features.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.getItemEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.core.BlockPos
import org.robbie.yaha.registry.YahaCriteria

object OpSussifyBlock : SpellAction {
    override val argc = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRangeForEditing(pos)
        val item = args.getItemEntity(env.world, 1, argc)
        env.assertEntityInRange(item)

        val block = env.world.getBlockState(pos).block
        val brushBlock = when (block) {
            Blocks.SAND -> Blocks.SUSPICIOUS_SAND
            Blocks.GRAVEL -> Blocks.SUSPICIOUS_GRAVEL
            else -> throw MishapBadBlock.of(pos, "yaha:sussifiable")
        }

        if (item.item.item == block.asItem() && env.castingEntity is ServerPlayer)
            YahaCriteria.SUSCEPTION.trigger(env.castingEntity as ServerPlayer)

        return SpellAction.Result(
            Spell(pos, brushBlock, item),
            MediaConstants.DUST_UNIT / 8,
            listOf(ParticleSpray.cloud(pos.center, 1.0))
        )
    }

    private data class Spell(val pos: BlockPos, val brushBlock: Block, val item: ItemEntity) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            if (!env.canEditBlockAt(pos)) return

            if (!IXplatAbstractions.INSTANCE.isPlacingAllowed(
                    env.world,
                    pos,
                    ItemStack(brushBlock),
                    env.castingEntity as? ServerPlayer
            )) return

            val blockNbt = CompoundTag()
            blockNbt.put("item", item.item.save(env.world.registryAccess()))

            val blockWithNbt = BlockInput(
                brushBlock.defaultBlockState(),
                mutableSetOf<Property<*>>(),
                blockNbt
            )

            if (blockWithNbt.place(env.world, pos, Block.UPDATE_ALL))
                item.discard()
        }
    }
}
