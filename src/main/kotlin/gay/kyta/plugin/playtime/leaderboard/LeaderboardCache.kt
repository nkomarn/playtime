package gay.kyta.plugin.playtime.leaderboard

import kotlin.time.Duration

data class LeaderboardPosition(
    val username: String,
    val playtime: Duration,
)

enum class Period {
    ALL,
    YEAR,
    MONTH,
    WEEK,
    DAY,
    HOUR,
}

interface LeaderboardCache {
    operator fun get(period: Period? = null): List<LeaderboardPosition>
    fun refresh()
}