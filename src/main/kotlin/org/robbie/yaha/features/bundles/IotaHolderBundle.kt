package org.robbie.yaha.features.bundles

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.item.IotaHolderItem
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUtils
import net.minecraft.world.inventory.Slot
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.stats.Stats
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickAction
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.core.NonNullList
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.BundleContents
import org.robbie.yaha.Yaha
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

val ITEM_BAR_COLOR = Mth.color(0.4f, 0.4f, 1.0f)

/**
 * Holds 16 individual items that satisfy the given filter.
 * Reading this bundle picks and reads a random item inside, or null if no non-empty readable item can be found.
 */
class IotaHolderBundle(settings: Properties, val filter: Predicate<Item>) : Item(settings), IotaHolderItem {
    override fun overrideStackedOnOther(stack: ItemStack, slot: Slot, clickType: ClickAction, player: Player): Boolean {
        if (clickType != ClickAction.SECONDARY) return false
        val slotItem = slot.item
        if (slotItem.isEmpty) {
            removeFirst(stack)?.let {
                slot.safeInsert(it)
                playRemoveOneSound(player)
            }
        } else if (slotItem.item.canFitInsideContainerItems() && addOneToBundle(stack, slotItem)) {
            slotItem.shrink(1)
            playInsertSound(player)
        }
        return true
    }

    override fun overrideOtherStackedOnMe(
        stack: ItemStack,
        otherStack: ItemStack,
        slot: Slot,
        clickType: ClickAction,
        player: Player,
        cursorStackReference: SlotAccess
    ): Boolean {
        if (clickType != ClickAction.SECONDARY) return false
        if (otherStack.isEmpty) {
            removeSelected(stack, player)?.let {
                cursorStackReference.set(it)
                playRemoveOneSound(player)
            }
        } else if (addOneToBundle(stack, otherStack)) {
            otherStack.shrink(1)
            playInsertSound(player)
        }
        return true
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val bundle = user.getItemInHand(hand)
        if (dropAll(bundle, user)) {
            playDropContentsSound(user)
            user.awardStat(Stats.ITEM_USED.get(this))
            return InteractionResultHolder.sidedSuccess(bundle, world.isClientSide)
        }
        return InteractionResultHolder.fail(bundle)
    }

    override fun isBarVisible(stack: ItemStack) = getBundleOccupancy(stack) > 0

    override fun getBarWidth(stack: ItemStack) = (1 + 12 * getBundleOccupancy(stack) / MAX_COUNT)
        .coerceAtMost(13)

    override fun getBarColor(stack: ItemStack) = ITEM_BAR_COLOR

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        val defaultedList = NonNullList.create<ItemStack>()
        getBundledStacks(stack).forEach { defaultedList.add(it) }
        return Optional.of(IotaBundleTooltipData(defaultedList))
    }

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag) {
        tooltip.add(
            Component.translatable(
                "item.minecraft.bundle.fullness",
                getBundleOccupancy(stack),
                MAX_COUNT).withStyle(ChatFormatting.GRAY)
        )
    }

    override fun onDestroyed(entity: ItemEntity) {
        ItemUtils.onContainerDestroyed(entity, getBundledStacks(entity.item).toList())
    }

    /**
     * Choose a random item in the bundle and return the iota tag it holds.
     * Items with no iota tag can be chosen, and return a NullIota.
     * If the bundle is empty, this returns null.
     */
    override fun readIota(stack: ItemStack): Iota? {
        val itemList = getBundledStacks(stack).toList()
        if (itemList.isEmpty()) return null
        val chosenItem = itemList.elementAt(Yaha.RANDOM.nextInt(itemList.count()))
        return (chosenItem.item as? IotaHolderItem)?.readIota(chosenItem)
            ?: NullIota()
    }

    override fun writeable(stack: ItemStack) = false
    override fun canWrite(stack: ItemStack, iota: Iota?) = false
    override fun writeDatum(stack: ItemStack, iota: Iota?) {}

    /**
     * Attempts to add one item of the given stack to the bundle
     * and returns if it was successful. Items are added to the beginning of the list
     */
    fun addOneToBundle(bundle: ItemStack, stack: ItemStack): Boolean {
        if (getBundleOccupancy(bundle) == MAX_COUNT || !filter.test(stack.item)) return false
        val items = getBundledStacks(bundle).toList().toMutableList()
        items.add(0, stack.copyWithCount(1))
        setBundledStacks(bundle, items)
        return true
    }

    /**
     * Removes the first item in the bundle and returns it.
     * Returns null if the bundle was empty.
     */
    fun removeFirst(bundle: ItemStack): ItemStack? {
        val items = getBundledStacks(bundle).toList().toMutableList()
        if (items.isEmpty()) return null
        val removed = items.removeAt(0)
        setBundledStacks(bundle, items)
        return removed
    }

    /**
     * Removes the item selected by the player and returns it.
     * Returns null if either the bundle was empty
     * or the player has selected outside the list of items. somehow.
     */
    fun removeSelected(bundle: ItemStack, player: Player): ItemStack? {
        val selected = BundleSelection.get(player)
        val items = getBundledStacks(bundle).toList().toMutableList()
        if (selected !in items.indices) return null
        val removed = items.removeAt(selected)
        setBundledStacks(bundle, items)
        return removed
    }

    /**
     * Drops all items in the bundle. bleeeehh
     * Returns if dropping was successful (bundle wasn't empty)
     */
    fun dropAll(bundle: ItemStack, player: Player): Boolean {
        val items = getBundledStacks(bundle).toList()
        if (items.isEmpty()) return false
        if (player is ServerPlayer) {
            for (itemStack in items) {
                player.drop(itemStack, true)
            }
        }
        bundle.remove(DataComponents.BUNDLE_CONTENTS)
        return true
    }

    fun playRemoveOneSound(entity: Entity) {
        entity.playSound(
            SoundEvents.BUNDLE_REMOVE_ONE,
            0.8f,
            0.8f + entity.random.nextFloat() * 0.4f
        )
    }

    fun playInsertSound(entity: Entity) {
        entity.playSound(
            SoundEvents.BUNDLE_INSERT,
            0.8f,
            0.8f + entity.random.nextFloat() * 0.4f
        )
    }

    fun playDropContentsSound(entity: Entity) {
        entity.playSound(
            SoundEvents.BUNDLE_DROP_CONTENTS,
            0.8f,
            0.8f + entity.random.nextFloat() * 0.4f
        )
    }

    companion object {
        // these functions are here so they can be used by the item model
        const val MAX_COUNT = 16

        fun getBundledStacks(bundle: ItemStack): Stream<ItemStack> {
            return bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopyStream()
        }

        fun getBundleOccupancy(bundle: ItemStack) = getBundledStacks(bundle).count().toInt()

        private fun setBundledStacks(bundle: ItemStack, items: List<ItemStack>) {
            if (items.isEmpty()) {
                bundle.remove(DataComponents.BUNDLE_CONTENTS)
            } else {
                bundle.set(DataComponents.BUNDLE_CONTENTS, BundleContents(items))
            }
        }
    }
}
