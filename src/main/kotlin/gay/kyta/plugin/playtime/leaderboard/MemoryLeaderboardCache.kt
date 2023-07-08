package gay.kyta.plugin.playtime.leaderboard

import gay.kyta.plugin.playtime.session.SessionLogger
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class MemoryLeaderboardCache(private val sessionLogger: SessionLogger) : LeaderboardCache {
    private val currentStandings = ConcurrentHashMap<Period, List<LeaderboardPosition>>()

    override fun get(period: Period?) = currentStandings[period] ?: emptyList()

    @OptIn(ExperimentalStdlibApi::class)
    override fun refresh() = runBlocking {
        for (period in Period.entries) {
            currentStandings[period] = sessionLogger.getTopSessions(period)
        }
    }
}