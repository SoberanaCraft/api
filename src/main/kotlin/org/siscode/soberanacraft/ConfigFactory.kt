package org.siscode.soberanacraft

import mu.KotlinLogging
import org.siscode.soberanacraft.models.Config
import org.siscode.soberanacraft.models.from
import org.siscode.soberanacraft.models.new
import kotlin.io.path.Path
import kotlin.io.path.exists

object ConfigFactory {
    lateinit var config: Config
    private val logger = KotlinLogging.logger {  }
    fun init(cfgPath: String){
        createIfNotFound(cfgPath)
        logger.info { "Reading config..." }
        config = from(cfgPath)
        if (config == Config.DEFAULT) return
    }

    private fun createIfNotFound(path: String) {
        if (Path(path).exists()) return
        logger.error { "No config was found. Creating the default one." }
        new(path)
    }
}