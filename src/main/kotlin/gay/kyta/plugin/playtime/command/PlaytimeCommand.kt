package gay.kyta.plugin.playtime.command

import gay.kyta.plugin.playtime.Formatters
import gay.kyta.plugin.playtime.leaderboard.LeaderboardCache
import gay.kyta.plugin.playtime.leaderboard.Period
import gay.kyta.plugin.playtime.message.MessageContainer
import gay.kyta.plugin.playtime.message.placeholder
import gay.kyta.plugin.playtime.render
import gay.kyta.plugin.playtime.session.SessionLogger
import gay.kyta.plugin.playtime.sum
import gay.kyta.plugin.playtime.username
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.AutoComplete
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import kotlin.time.Duration

class PlaytimeCommand(
    private val coroutineScope: CoroutineScope,
    private val sessionLogger: SessionLogger,
    private val leaderboardCache: LeaderboardCache,
    private val messages: MessageContainer,
) {
    @Command("playtime", "pt")
    @AutoComplete("@players-permissible")
    fun playtime(source: Player, @Optional target: OfflinePlayer?) {
        coroutineScope.launch {
            val player = getTarget(source, target)
            val sessions = sessionLogger.getSessions(player, Period.ALL)
            val currentSession = player.player
                ?.let { sessionLogger.getCurrentSession(it) }
                ?: Duration.ZERO

            val content = messages[
                "command_playtime_response",
                "target".placeholder(player.username),
                "playtime".placeholder(Formatters.PRETTY.render(sessions.sum())),
                "sessions".placeholder(sessions.size),
                "session".placeholder(Formatters.PRETTY.render(currentSession)),
            ]

            source.sendMessage(content)
        }
    }

    @Command("ptw")
    @AutoComplete("@players-permissible")
    fun weeklyPlaytime(source: Player, @Optional target: OfflinePlayer?) {
        coroutineScope.launch {
            val player = getTarget(source, target)
            val weeklyPlaytime = sessionLogger.getSessions(player, Period.WEEK).sum()
            val content = messages[
                "command_playtime_weekly",
                "target".placeholder(if (player == source) "You've" else player.username),
                "playtime".placeholder(Formatters.PRETTY.render(weeklyPlaytime)),
            ]

            source.sendMessage(content)
        }
    }

    @Command("ptm")
    @AutoComplete("@players-permissible")
    fun monthlyPlaytime(source: Player, @Optional target: OfflinePlayer?) {
        coroutineScope.launch {
            val player = getTarget(source, target)
            val monthlyPlaytime = sessionLogger.getSessions(player, Period.MONTH).sum()
            val content = messages[
                "command_playtime_monthly",
                "target".placeholder(if (player == source) "You've" else player.username),
                "playtime".placeholder(Formatters.PRETTY.render(monthlyPlaytime)),
            ]

            source.sendMessage(content)
        }
    }

    @Command("pttop")
    fun top(source: CommandSender) {
        source.sendMessage(createLeaderboardResponse(Period.ALL))
    }

    @Command("ptwtop")
    fun weeklyTop(source: CommandSender) {
        source.sendMessage(createLeaderboardResponse(Period.WEEK))
    }

    @Command("ptmtop")
    fun monthlyTop(source: CommandSender) {
        source.sendMessage(createLeaderboardResponse(Period.MONTH))
    }

    private fun getTarget(source: Player, target: OfflinePlayer?) = target ?: source

    private fun createLeaderboardResponse(period: Period): Component {
        val builder = Component.text().append(Component.newline())
        val entries = leaderboardCache[period].take(10)

        /* if there's no data to speak of, let the player know */
        if (entries.isEmpty()) {
            return builder
                .append(messages["command_playtime_leaderboard_empty"])
                .append(Component.newline())
                .build()
        }

        entries.mapIndexed { index, it ->
            val entry = messages[
                "command_playtime_leaderboard_entry",
                "position".placeholder(index + 1),
                "username".placeholder(it.username),
                "playtime".placeholder(Formatters.VERBOSE.render(it.playtime)),
            ]

            builder
                .append(entry)
                .append(Component.newline())
        }

        return builder.build()
    }
}