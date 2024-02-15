package com.example.examplemod

import com.mojang.logging.LogUtils
import org.slf4j.Logger

object ExampleMod {
    /** The mod id for  examplemod.  */
    const val MOD_ID: String = "examplemod"

    /** The logger for examplemod.  */
    val LOGGER: Logger = LogUtils.getLogger()

    /**
     * Initializes the mod.
     */
    @JvmStatic
    fun init() {
    }
}
