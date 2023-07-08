package gay.kyta.plugin.playtime

import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder

object Formatters {
    val PRETTY: PeriodFormatter = PeriodFormatterBuilder()
        .appendDays()
        .appendSuffix(" days ")
        .appendHours()
        .appendSuffix(" hours ")
        .appendMinutes()
        .appendSuffix(" minutes ")
        .appendSeconds()
        .appendSuffix(" seconds ")
        .toFormatter()

    val VERBOSE: PeriodFormatter = PeriodFormatterBuilder()
        .appendDays()
        .appendSuffix("d ")
        .appendHours()
        .appendSuffix("h ")
        .appendMinutes()
        .appendSuffix("m ")
        .appendSeconds()
        .appendSuffix("s ")
        .toFormatter()
}