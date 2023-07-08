package gay.kyta.plugin.playtime.session

import gay.kyta.plugin.playtime.leaderboard.LeaderboardPosition
import gay.kyta.plugin.playtime.leaderboard.Period
import org.bukkit.entity.Player
import kotlin.time.Duration

interface SessionLogger {
    suspend fun recordLogin(player: Player)
    suspend fun recordDisconnect(player: Player)
    suspend fun getSessions(player: Player, period: Period): List<Duration>
    suspend fun getCurrentSession(player: Player): Duration
    suspend fun getTopSessions(period: Period, player: Player? = null, quantity: Int = 10): List<LeaderboardPosition>
    fun shutdown()
}