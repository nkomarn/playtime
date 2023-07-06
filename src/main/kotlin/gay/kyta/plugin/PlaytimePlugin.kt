package gay.kyta.plugin

import gay.kyta.plugin.playtime.command.PlaytimeCommand
import gay.kyta.plugin.playtime.listener.ConnectionListeners
import gay.kyta.plugin.playtime.message.MessageContainer
import gay.kyta.plugin.playtime.message.PluginMessageContainer
import gay.kyta.plugin.playtime.registerListener
import gay.kyta.plugin.playtime.session.SessionLogger
import gay.kyta.plugin.playtime.session.SqliteSessionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.bukkit.BukkitCommandHandler

class PlaytimePlugin : JavaPlugin(), CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
    lateinit var commandHandler: BukkitCommandHandler
    lateinit var messages: MessageContainer
    lateinit var sessionLogger: SessionLogger

    override fun onEnable() {
        commandHandler = BukkitCommandHandler.create(this)
        messages = PluginMessageContainer(this)
        sessionLogger = SqliteSessionLogger(this)
        registerListener(ConnectionListeners(this, sessionLogger))

        /* register commands */
        commandHandler.register(PlaytimeCommand(this, sessionLogger))
        commandHandler.registerBrigadier()
    }
}