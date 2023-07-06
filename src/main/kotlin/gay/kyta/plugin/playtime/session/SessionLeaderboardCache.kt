package gay.kyta.plugin.playtime.session

import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class SessionLeaderboardCache(private val sessionLogger: SessionLogger) : LeaderboardCache {
    private val currentStandings = ConcurrentHashMap<Period, List<LeaderboardPosition>>()

    override fun get(period: Period?) = currentStandings[period] ?: emptyList()

    @OptIn(ExperimentalStdlibApi::class)
    override fun refresh() = runBlocking {
        for (period in Period.entries) {
            currentStandings[period] = sessionLogger.assembleLeaderboard(period.duration)
        }
    }
}