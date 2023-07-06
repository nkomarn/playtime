package gay.kyta.plugin.playtime.session

import gay.kyta.plugin.PlaytimePlugin
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

class SqliteSessionLogger(plugin: PlaytimePlugin) : SessionLogger {
    private val currentSessions = ConcurrentHashMap<Player, Instant>()

    init {
        /* set up tables and a connection to our database */
        Database.connect("jdbc:sqlite:${File(plugin.dataFolder, "sessions.db")}", "org.sqlite.JDBC")
        transaction { SchemaUtils.create(Sessions) }
    }

    override suspend fun recordLogin(player: Player) {
        /* record the login time in order to calculate session time at logout */
        currentSessions[player] = Clock.System.now()
    }

    override suspend fun recordDisconnect(player: Player) = newSuspendedTransaction {
        val loginTime = currentSessions.remove(player) ?: return@newSuspendedTransaction

        /* record this session into our database */
        Sessions.insert {
            it[Sessions.player] = player.uniqueId
            it[timestamp] = loginTime.toJavaInstant()
            it[duration] = (Clock.System.now() - loginTime).toJavaDuration()
        }
    }

    override suspend fun getSessions(player: Player, period: Duration?): List<Duration> = newSuspendedTransaction {
        val currentSession = Clock.System.now() - currentSessions[player]!!
        var query = Sessions.slice(Sessions.duration)
            .select { Sessions.player eq player.uniqueId }

        /* query within a specific period, if specified */
        if (period != null) {
            val minimumTimestamp = (Clock.System.now() - period).toJavaInstant()
            query = query.andWhere { Sessions.timestamp greaterEq minimumTimestamp }
        }

        val previousSessions = query.map { it[Sessions.duration].toKotlinDuration() }
        return@newSuspendedTransaction previousSessions + currentSession
    }
}

private object Sessions : IntIdTable() {
    val player = uuid("player").index()
    val timestamp = timestamp("timestamp")
    val duration = duration("duration")
}