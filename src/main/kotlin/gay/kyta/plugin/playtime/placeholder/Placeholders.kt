package gay.kyta.plugin.playtime.placeholder

import gay.kyta.plugin.playtime.Formatters
import gay.kyta.plugin.playtime.leaderboard.LeaderboardCache
import gay.kyta.plugin.playtime.leaderboard.LeaderboardPosition
import gay.kyta.plugin.playtime.leaderboard.Period
import gay.kyta.plugin.playtime.message.MessageContainer
import gay.kyta.plugin.playtime.message.placeholder
import gay.kyta.plugin.playtime.render
import gay.kyta.plugin.playtime.session.SessionLogger
import gay.kyta.plugin.playtime.sum
import gay.kyta.plugin.playtime.valueOf
import kotlinx.coroutines.runBlocking
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.plugin.PluginDescriptionFile

class Placeholders(
    private val description: PluginDescriptionFile,
    private val sessionLogger: SessionLogger,
    private val leaderboardCache: LeaderboardCache,
    private val messages: MessageContainer,
) : PlaceholderExpansion() {
    override fun getIdentifier() = description.name.lowercase()
    override fun getAuthor() = description.authors.joinToString(", ")
    override fun getVersion() = description.version
    override fun persist() = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        val arguments = params.split("_")
        val category = arguments.getOrNull(0) ?: return null

        return when (category) {
            "session" -> {
                if (player == null) return null
                runBlocking { Formatters.VERBOSE.render(sessionLogger.getCurrentSession(player)) }
            }

            "sessions" -> {
                if (player == null) return null
                runBlocking { sessionLogger.getSessions(player, Period.ALL).size }.toString()
            }

            "self" -> {
                if (player == null) return null
                val period = arguments.getOrNull(1)?.let { valueOf<Period>(it) } ?: return null
                runBlocking { sessionLogger.getSessions(player, period) }.sum()
                    .let { Formatters.VERBOSE.render(it) }
            }

            "leaderboard" -> {
                val period = arguments.getOrNull(1)?.let { valueOf<Period>(it) } ?: return null
                val place = arguments.getOrNull(2)?.toIntOrNull() ?: return null
                leaderboardCache[period].getOrNull(place - 1)?.render() ?: return "-"
            }

            else -> null
        }
    }

    private fun LeaderboardPosition.render() = messages[
        "placeholder_leaderboard_position",
        "username".placeholder(username),
        "playtime".placeholder(Formatters.VERBOSE.render(playtime))
    ].serialize

    private companion object {
        val serializer = LegacyComponentSerializer.builder()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()

        inline val Component.serialize
            get() = serializer.serialize(this)
    }
}