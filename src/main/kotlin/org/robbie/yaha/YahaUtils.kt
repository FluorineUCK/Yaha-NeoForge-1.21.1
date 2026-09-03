package org.robbie.yaha

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

object YahaUtils {
    /**
     * Returns the pitch and yaw for a given direction vector, or null if it is the zero vector
     */
    fun pitchYawFromRotVec(rotVec: Vec3): Pair<Float, Float>? {
        if (rotVec.lengthSqr() == 0.0) return null
        val yaw = (Mth.atan2(-rotVec.x, rotVec.z) * 180f / Math.PI).toFloat()
        val pitch = (Mth.atan2(-rotVec.y, rotVec.horizontalDistance()) * 180f / Math.PI).toFloat()
        return pitch to yaw
    }
}
