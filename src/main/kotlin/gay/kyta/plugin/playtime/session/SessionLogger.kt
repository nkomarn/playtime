package gay.kyta.plugin.playtime.session

import org.bukkit.entity.Player
import kotlin.time.Duration

interface SessionLogger {
    suspend fun recordLogin(player: Player)
    suspend fun recordDisconnect(player: Player)
    suspend fun getSessions(player: Player, period: Duration? = null): List<Duration>
    suspend fun assembleLeaderboard(period: Duration? = null, quantity: Int = 10): List<LeaderboardPosition>
}