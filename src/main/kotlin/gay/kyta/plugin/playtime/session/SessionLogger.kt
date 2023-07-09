package gay.kyta.plugin.playtime.session

import gay.kyta.plugin.playtime.leaderboard.LeaderboardPosition
import gay.kyta.plugin.playtime.leaderboard.Period
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import kotlin.time.Duration

interface SessionLogger {
    suspend fun recordLogin(player: Player)
    suspend fun recordDisconnect(player: Player)
    suspend fun getSessions(player: OfflinePlayer, period: Period): List<Duration>
    suspend fun getCurrentSession(player: Player): Duration
    suspend fun getTopSessions(period: Period, player: OfflinePlayer? = null, quantity: Int = 10): List<LeaderboardPosition>
    fun shutdown()
}