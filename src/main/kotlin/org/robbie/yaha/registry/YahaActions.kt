package org.robbie.yaha.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.HexRegistries
import net.neoforged.neoforge.registries.RegisterEvent
import org.robbie.yaha.Yaha
import org.robbie.yaha.features.anvil.OpAnvil
import org.robbie.yaha.features.armor_stand.OpStandPose
import org.robbie.yaha.features.armor_stand.OpStandToggle
import org.robbie.yaha.features.armor_stand.OpStandYaw
import org.robbie.yaha.features.paper_plane.OpPaperPlane
import org.robbie.yaha.features.paper_plane.OpPaperPlaneTarget
import org.robbie.yaha.features.spells.OpPotionToItem
import org.robbie.yaha.features.spells.OpSussifyBlock
import org.robbie.yaha.features.time_bomb.OpTimeBomb
import org.robbie.yaha.features.time_bomb.OpTimeBombPos
import org.robbie.yaha.features.trident.OpTrident

object YahaActions {
    fun register(event: RegisterEvent) {
        register(event, "paper_plane", HexDir.NORTH_WEST, "wwqaqwwdw", OpPaperPlane)
        register(event, "paper_plane_target", HexDir.NORTH_WEST, "wwqaqwwdedde", OpPaperPlaneTarget)
        register(event, "time_bomb", HexDir.NORTH_WEST, "eewaqawee", OpTimeBomb)
        register(event, "time_bomb_pos", HexDir.NORTH_WEST, "eewaqaweedd", OpTimeBombPos)
        register(event, "anvil", HexDir.WEST, "dqdwdqdqaa", OpAnvil)
        register(event, "trident", HexDir.SOUTH_EAST, "ddwwdaaeaa", OpTrident)

        register(event, "sussify_block", HexDir.EAST, "eqqqeawqwqwqwqwqw", OpSussifyBlock)
        register(event, "potion_to_item", HexDir.EAST, "dqqqqqedwda", OpPotionToItem)

        register(event, "stand_toggle_arms", HexDir.WEST, "eddweaqadaq", OpStandToggle(OpStandToggle.Toggle.SHOW_ARMS))
        register(event, "stand_toggle_base", HexDir.NORTH_WEST, "dawddwe", OpStandToggle(OpStandToggle.Toggle.HIDE_BASEPLATE))
        register(event, "stand_toggle_tiny", HexDir.EAST, "adaaea", OpStandToggle(OpStandToggle.Toggle.MAKE_SMALL))
        register(event, "stand_rotate_head", HexDir.NORTH_EAST, "edweaqadaw", OpStandPose(OpStandPose.Part.HEAD))
        register(event, "stand_rotate_body", HexDir.NORTH_WEST, "aweaqadawd", OpStandPose(OpStandPose.Part.BODY))
        register(event, "stand_rotate_yaw", HexDir.NORTH_EAST, "eadawddwea", OpStandYaw)
        register(event, "stand_rotate_left_arm", HexDir.EAST, "addweaqada", OpStandPose(OpStandPose.Part.LEFT_ARM))
        register(event, "stand_rotate_right_arm", HexDir.WEST, "eddweaqada", OpStandPose(OpStandPose.Part.RIGHT_ARM))
        register(event, "stand_rotate_left_leg", HexDir.SOUTH_EAST, "daqadawddw", OpStandPose(OpStandPose.Part.LEFT_LEG))
        register(event, "stand_rotate_right_leg", HexDir.SOUTH_WEST, "dwddweaqad", OpStandPose(OpStandPose.Part.RIGHT_LEG))
    }

    private fun register(event: RegisterEvent, name: String, startDir: HexDir, sig: String, action: Action) =
        event.register(HexRegistries.ACTION, Yaha.id(name)) {
            ActionRegistryEntry(HexPattern.fromAngles(sig, startDir), action)
        }
}
