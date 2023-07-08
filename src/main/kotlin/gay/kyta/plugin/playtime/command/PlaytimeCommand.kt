package gay.kyta.plugin.playtime.command

import gay.kyta.plugin.playtime.Formatters
import gay.kyta.plugin.playtime.leaderboard.LeaderboardCache
import gay.kyta.plugin.playtime.leaderboard.Period
import gay.kyta.plugin.playtime.message.MessageContainer
import gay.kyta.plugin.playtime.message.placeholder
import gay.kyta.plugin.playtime.render
import gay.kyta.plugin.playtime.session.SessionLogger
import gay.kyta.plugin.playtime.sum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command

class PlaytimeCommand(
    private val coroutineScope: CoroutineScope,
    private val sessionLogger: SessionLogger,
    private val leaderboardCache: LeaderboardCache,
    private val messages: MessageContainer,
) {
    @Command("playtime", "pt")
    fun playtime(source: Player) {
        coroutineScope.launch {
            val sessions = sessionLogger.getSessions(source, Period.ALL)
            val currentSession = sessionLogger.getCurrentSession(source)

            val content = messages[
                "command_playtime_response",
                "playtime".placeholder(Formatters.PRETTY.render(sessions.sum())),
                "sessions".placeholder(sessions.size),
                "session".placeholder(Formatters.PRETTY.render(currentSession)),
            ]

            source.sendMessage(content)
        }
    }

    @Command("ptw")
    fun weeklyPlaytime(source: Player) {
        coroutineScope.launch {
            val weeklyPlaytime = sessionLogger.getSessions(source, Period.WEEK).sum()
            val content = messages[
                "command_playtime_weekly",
                "playtime".placeholder(Formatters.PRETTY.render(weeklyPlaytime))
            ]

            source.sendMessage(content)
        }
    }

    @Command("ptm")
    fun monthlyPlaytime(source: Player) {
        coroutineScope.launch {
            val monthlyPlaytime = sessionLogger.getSessions(source, Period.MONTH).sum()
            val content = messages[
                "command_playtime_monthly",
                "playtime".placeholder(Formatters.PRETTY.render(monthlyPlaytime))
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

    private fun createLeaderboardResponse(period: Period): Component {
        val builder = Component.text()
            .append(Component.newline())

        leaderboardCache[period].take(10).mapIndexed { index, it ->
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