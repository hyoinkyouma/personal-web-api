package dbutils

import App.Companion.dotenv
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object DBConnection {
    lateinit var db: HikariDataSource

    fun init() {
        val config = HikariConfig()
        config.jdbcUrl =
            "jdbc:pgsql://${dotenv.get("NODE_DB_HOST")}:${dotenv.get("NODE_DB_PORT")}/${dotenv.get("NODE_DB_NAME")}"
        config.connectionTimeout = 30_000
        config.maxLifetime = 1_800_000
        config.idleTimeout = 600_000
        config.username = dotenv.get("NODE_DB_USERNAME").toString()
        config.password = dotenv.get("NODE_DB_PASSWORD").toString()

        db = HikariDataSource(config)
    }
}