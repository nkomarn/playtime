package gay.kyta.plugin.playtime.session

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

data class LeaderboardPosition(
    val username: String,
    val playtime: Duration,
)

enum class Period(val duration: Duration) {
    YEAR(365.days),
    MONTH(30.days),
    WEEK(7.days),
    DAY(1.days),
    HOUR(1.hours),
}

interface LeaderboardCache {
    operator fun get(period: Period? = null): List<LeaderboardPosition>
    fun refresh()
}