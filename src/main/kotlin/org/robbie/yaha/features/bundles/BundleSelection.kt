package org.robbie.yaha.features.bundles

import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BundleSelection {
    private val selectedByPlayer: MutableMap<UUID, Int> = ConcurrentHashMap()

    fun get(player: Player): Int = selectedByPlayer[player.uuid] ?: 0

    fun set(player: Player, selected: Int) {
        selectedByPlayer[player.uuid] = selected.coerceIn(0, IotaHolderBundle.MAX_COUNT - 1)
    }

    fun clear(player: Player) {
        selectedByPlayer.remove(player.uuid)
    }
}
