package net.soberanacraft.api.dao

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import net.soberanacraft.api.models.*


object DatabaseFactory {
//    private val logger = LoggerFactory.getLogger("DatabaseFactory")!!
    fun init() {
//        logger.warn("Starting...")
        val driverClassName = "org.sqlite.JDBC"
        val jdbcURL = "jdbc:sqlite:file:./database.db"
//        logger.warn("Connecting to Database...")
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Players, Servers, Connections, Nonces, Discord)
        }
//        logger.warn("Connected.")
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}