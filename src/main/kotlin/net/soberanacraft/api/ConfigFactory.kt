package net.soberanacraft.api

import mu.KotlinLogging
import net.soberanacraft.api.models.Config
import net.soberanacraft.api.models.from
import net.soberanacraft.api.models.new
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
	val _path = Path(path)
	if (_path.exists()) return

	if (_path.toFile().parentFile.mkdirs()) {
	    logger.warn { "Subdirectories created." } 
	}

	logger.error { "No config was found. Creating the default one." }
        new(path)
    }
}
