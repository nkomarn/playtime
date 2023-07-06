@file:Suppress("unused")

package gay.kyta.plugin.playtime.listener

import gay.kyta.plugin.playtime.session.SessionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ConnectionListeners(
    private val coroutineScope: CoroutineScope,
    private val sessionLogger: SessionLogger,
) : Listener {

    @EventHandler(priority = MONITOR)
    private fun connect(event: PlayerJoinEvent) {
        runBlocking { sessionLogger.recordLogin(event.player) }
    }

    @EventHandler(priority = MONITOR)
    private fun disconnect(event: PlayerQuitEvent) {
        coroutineScope.launch { sessionLogger.recordDisconnect(event.player) }
    }
}