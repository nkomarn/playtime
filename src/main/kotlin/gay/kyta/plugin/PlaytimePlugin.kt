package gay.kyta.plugin

import gay.kyta.plugin.playtime.command.PlaytimeCommand
import gay.kyta.plugin.playtime.leaderboard.LeaderboardCache
import gay.kyta.plugin.playtime.leaderboard.MemoryLeaderboardCache
import gay.kyta.plugin.playtime.listener.ConnectionListeners
import gay.kyta.plugin.playtime.message.MessageContainer
import gay.kyta.plugin.playtime.message.PluginMessageContainer
import gay.kyta.plugin.playtime.placeholder.Placeholders
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
    private lateinit var commandHandler: BukkitCommandHandler
    private lateinit var messages: MessageContainer
    private lateinit var sessionLogger: SessionLogger
    private lateinit var leaderboardCache: LeaderboardCache
    private lateinit var placeholders: Placeholders

    override fun onEnable() {
        commandHandler = BukkitCommandHandler.create(this)
        messages = PluginMessageContainer(this)
        sessionLogger = SqliteSessionLogger(this)
        leaderboardCache = MemoryLeaderboardCache(sessionLogger)
        registerListener(ConnectionListeners(this, sessionLogger))

        /* register commands */
        commandHandler.register(PlaytimeCommand(this, sessionLogger, leaderboardCache, messages))
        commandHandler.registerBrigadier()

        /* schedule leaderboard updates */
        server.scheduler.runTaskTimerAsynchronously(this, Runnable { leaderboardCache.refresh() }, 20 * 15, 20 * 60)

        /* hook into placeholder api */
        if (!server.pluginManager.isPluginEnabled("PlaceholderAPI")) return
        placeholders = Placeholders(description, sessionLogger, leaderboardCache, messages).also { it.register() }
    }

    override fun onDisable() {
        /* make sure all session data is saved correctly */
        sessionLogger.shutdown()

        /* unregister placeholders to avoid classloader conflicts */
        if (this::placeholders.isInitialized) placeholders.unregister()
    }
}