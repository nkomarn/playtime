package gay.kyta.plugin.playtime.command

import gay.kyta.plugin.playtime.session.SessionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.DefaultFor
import kotlin.time.Duration

@Command("playtime")
class PlaytimeCommand(
    private val coroutineScope: CoroutineScope,
    private val sessionLogger: SessionLogger,
) {
    private val formatter = PeriodFormatterBuilder()
        .appendDays()
        .appendSuffix(" days ")
        .appendHours()
        .appendSuffix(" hours ")
        .appendMinutes()
        .appendSuffix(" minutes ")
        .appendSeconds()
        .appendSuffix(" seconds ")
        .toFormatter()

    @DefaultFor("playtime")
    fun playtime(source: Player) {
        coroutineScope.launch {
            val sessions = sessionLogger.getSessions(source)
            val totalPlaytime = sessions.fold(Duration.ZERO, Duration::plus)
            source.sendMessage(formatter.print(Period(totalPlaytime.inWholeMilliseconds)).trim())
        }
    }
}