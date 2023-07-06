package gay.kyta.plugin.playtime.placeholder

import gay.kyta.plugin.playtime.render
import gay.kyta.plugin.playtime.session.LeaderboardCache
import gay.kyta.plugin.playtime.session.Period
import gay.kyta.plugin.playtime.session.SessionLogger
import gay.kyta.plugin.playtime.sum
import gay.kyta.plugin.playtime.valueOf
import kotlinx.coroutines.runBlocking
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginDescriptionFile
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder

class Placeholders(
    private val description: PluginDescriptionFile,
    private val sessionLogger: SessionLogger,
    private val leaderboardCache: LeaderboardCache,
) : PlaceholderExpansion() {
    override fun getIdentifier() = description.name.lowercase()
    override fun getAuthor() = description.authors.joinToString(", ")
    override fun getVersion() = description.version
    override fun persist() = true

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        if (params == "self") {
            return runBlocking { formatter.render(sessionLogger.getSessions(player).sum()) }
        }

        if (params.startsWith("leaderboard_")) {
            val arguments = params.removePrefix("leaderboard_").split("_")
            val period = arguments.getOrNull(0)?.let { valueOf<Period>(it.uppercase()) } ?: return null
            val place = arguments.getOrNull(1)?.toIntOrNull() ?: return null
            val position = leaderboardCache[period].getOrNull(place - 1) ?: return null
            return "${position.username}, ${formatter.render(position.playtime)}"
        }

        return null
    }

    private companion object {
        val formatter: PeriodFormatter = PeriodFormatterBuilder()
            .appendDays()
            .appendSuffix("d")
            .appendHours()
            .appendSuffix("h")
            .appendMinutes()
            .appendSuffix("m")
            .toFormatter()
    }
}