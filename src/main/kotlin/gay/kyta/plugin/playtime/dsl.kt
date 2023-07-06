package gay.kyta.plugin.playtime

import kotlinx.datetime.Clock
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import java.util.UUID
import kotlin.time.Duration

fun JavaPlugin.registerListener(listener: Listener) =
    server.pluginManager.registerEvents(listener, this)

fun PeriodFormatter.render(duration: Duration) =
    print(Period(duration.inWholeMilliseconds)).trim()

inline fun <reified T : Enum<T>> valueOf(key: String) =
    runCatching { java.lang.Enum.valueOf(T::class.java, key.uppercase()) }.getOrNull()

fun now() = Clock.System.now()
fun List<Duration>.sum() = fold(Duration.ZERO, Duration::plus)
fun UUID.fetchPlayerName() = Bukkit.getOfflinePlayer(this).name ?: "???"