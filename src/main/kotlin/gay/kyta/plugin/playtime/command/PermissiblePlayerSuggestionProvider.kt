package gay.kyta.plugin.playtime.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.command.CommandActor
import revxrsal.commands.command.ExecutableCommand

object PermissiblePlayerSuggestionProvider : SuggestionProvider {
    override fun getSuggestions(
        args: MutableList<String>,
        sender: CommandActor,
        command: ExecutableCommand
    ): Collection<String> {
        var players = Bukkit.getServer().onlinePlayers

        /* hide vanished players */
        if (sender is Player) {
            players = players.filter { sender.canSee(it) }
        }

        return players.map { it.name }
    }
}