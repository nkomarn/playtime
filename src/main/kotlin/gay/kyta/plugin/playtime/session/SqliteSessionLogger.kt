package gay.kyta.plugin.playtime.session

import gay.kyta.plugin.PlaytimePlugin
import gay.kyta.plugin.playtime.fetchPlayerName
import gay.kyta.plugin.playtime.leaderboard.LeaderboardPosition
import gay.kyta.plugin.playtime.leaderboard.Period
import gay.kyta.plugin.playtime.now
import gay.kyta.plugin.playtime.toFirstInstant
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.JavaDurationColumnType
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

class SqliteSessionLogger(plugin: PlaytimePlugin) : SessionLogger {
    private val currentSessions = ConcurrentHashMap<Player, Instant>()

    init {
        /* set up tables and a connection to our database */
        Database.connect("jdbc:sqlite:${File(plugin.dataFolder, "sessions.db")}", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Sessions)

            /* migrate sessions from statistics on first startup */
            if (Sessions.selectAll().empty()) {
                plugin.logger.info("migrating session data from player statistics..")
                migrateFromStatistics()
                plugin.logger.info("migration complete!")
            }

            /* start sessions for any players already online */
            plugin.server.onlinePlayers.forEach { runBlocking { recordLogin(it) } }
        }
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

    override suspend fun getSessions(player: OfflinePlayer, period: Period): List<Duration> = newSuspendedTransaction {
        var query = Sessions.slice(Sessions.duration)
            .select { Sessions.player eq player.uniqueId }

        /* query within a specific period, if specified */
        if (period != Period.ALL) {
            val cutOff = calculateCutOff(period).toJavaInstant()
            query = query.andWhere { Sessions.timestamp greaterEq cutOff }
        }

        val previousSessions = query.map { it[Sessions.duration].toKotlinDuration() }
        val currentSession = player.player?.currentSession ?: Duration.ZERO
        return@newSuspendedTransaction previousSessions + currentSession
    }

    override suspend fun getCurrentSession(player: Player) = player.currentSession

    /**
     * - get sum of all sessions for unique player id **that are after a timestamp**
     * - sort descending
     */
    override suspend fun getTopSessions(period: Period, player: OfflinePlayer?, quantity: Int): List<LeaderboardPosition> =
        newSuspendedTransaction {
            /* query within a specific period, if specified. otherwise, use distant past */
            val cutOff = calculateCutOff(period)
            val durationSum = Sum(Sessions.duration, JavaDurationColumnType())
            var query = Sessions.slice(Sessions.player, Sessions.timestamp, durationSum)
                .select { Sessions.timestamp greaterEq cutOff.toJavaInstant() }

            /* additionally, filter by player, if specified */
            if (player != null) {
                query = query.andWhere { Sessions.player eq player.uniqueId }
            }

            query.orderBy(durationSum, SortOrder.DESC)
                .limit(quantity)
                .groupBy(Sessions.player)
                .map {
                    val currentSession = Bukkit.getPlayer(it[Sessions.player])?.currentSession ?: Duration.ZERO
                    val username = it[Sessions.player].fetchPlayerName()
                    val playtime = it[durationSum]!!.toKotlinDuration() + currentSession
                    LeaderboardPosition(username, playtime)
                }.sortedByDescending { it.playtime }
        }

    override fun shutdown() {
        Bukkit.getServer().onlinePlayers.forEach {
            runBlocking { recordDisconnect(it) }
        }
    }

    private fun calculateCutOff(period: Period) = when (period) {
        Period.YEAR -> Year.now().atDay(1).toFirstInstant()
        Period.MONTH -> LocalDate.now().withDayOfMonth(1).toFirstInstant()
        Period.WEEK -> LocalDateTime.now(ZoneId.systemDefault())
            .with(LocalTime.MIN)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toKotlinInstant()

        Period.DAY -> LocalDate.now().toFirstInstant()
        Period.HOUR -> LocalDateTime.now(ZoneId.systemDefault())
            .withMinute(0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toKotlinInstant()

        else -> Instant.DISTANT_PAST
    }

    private fun migrateFromStatistics() = transaction {
        val timestamp = calculateCutOff(Period.MONTH) - 1.minutes

        for (player in Bukkit.getOfflinePlayers()) {
            val ticksPlayed = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
            val durationPlayed = (ticksPlayed / 20).seconds

            Sessions.insert {
                it[Sessions.player] = player.uniqueId
                it[Sessions.timestamp] = timestamp.toJavaInstant()
                it[duration] = durationPlayed.toJavaDuration()
            }
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