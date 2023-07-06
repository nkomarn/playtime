package gay.kyta.plugin.playtime.session

import gay.kyta.plugin.PlaytimePlugin
import gay.kyta.plugin.playtime.fetchPlayerName
import gay.kyta.plugin.playtime.now
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaDurationColumnType
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
        currentSessions[player] = now()
    }

    override suspend fun recordDisconnect(player: Player) = newSuspendedTransaction {
        val loginTime = currentSessions.remove(player) ?: return@newSuspendedTransaction

        /* record this session into our database */
        Sessions.insert {
            it[Sessions.player] = player.uniqueId
            it[timestamp] = loginTime.toJavaInstant()
            it[duration] = (now() - loginTime).toJavaDuration()
        }
    }

    override suspend fun getSessions(player: Player, period: Duration?): List<Duration> = newSuspendedTransaction {
        var query = Sessions.slice(Sessions.duration)
            .select { Sessions.player eq player.uniqueId }

        /* query within a specific period, if specified */
        if (period != null) {
            val minimumTimestamp = (now() - period).toJavaInstant()
            query = query.andWhere { Sessions.timestamp greaterEq minimumTimestamp }
        }

        val previousSessions = query.map { it[Sessions.duration].toKotlinDuration() }
        return@newSuspendedTransaction previousSessions + player.currentSession
    }

    /**
     * - get sum of all sessions for unique player id **that are after a timestamp**
     * - sort descending
     */
    override suspend fun assembleLeaderboard(period: Duration?, quantity: Int): List<LeaderboardPosition> = newSuspendedTransaction {
        /* query within a specific period, if specified. otherwise, use distant past */
        val cutOff = if (period == null) Instant.DISTANT_PAST else now() - period
        val durationSum = Sum(Sessions.duration, JavaDurationColumnType())

        Sessions.slice(Sessions.player, Sessions.timestamp, durationSum)
            .select { Sessions.timestamp greaterEq cutOff.toJavaInstant() }
            .orderBy(durationSum)
            .limit(quantity)
            .groupBy(Sessions.player)
            .map {
                val currentSession = Bukkit.getPlayer(it[Sessions.player])?.currentSession ?: Duration.ZERO
                val username = it[Sessions.player].fetchPlayerName()
                val playtime = it[durationSum]!!.toKotlinDuration() + currentSession
                LeaderboardPosition(username, playtime)
            }
    }

    private inline val Player.currentSession
        get() = now() - currentSessions[this]!!
}

private object Sessions : IntIdTable() {
    val player = uuid("player").index()
    val timestamp = timestamp("timestamp")
    val duration = duration("duration")
}