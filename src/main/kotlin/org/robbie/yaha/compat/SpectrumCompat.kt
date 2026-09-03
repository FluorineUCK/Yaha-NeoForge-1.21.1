package org.robbie.yaha.compat

import org.robbie.yaha.features.anvil.AnvilEntity

object SpectrumCompat {
    @Suppress("UNUSED_PARAMETER")
    fun crush(anvil: AnvilEntity) {
        // Spectrum is Fabric-only for the original target. No NeoForge 1.21.1 API is available here.
    }
}
