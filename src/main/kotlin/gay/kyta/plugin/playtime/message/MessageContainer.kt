package gay.kyta.plugin.playtime.message

import gay.kyta.plugin.PlaytimePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.text.NumberFormat

fun String.placeholder(value: String) = TagResolver.resolver(this, Tag.preProcessParsed(value))
fun String.placeholder(value: Number) = placeholder(NumberFormat.getInstance().format(value))
fun String.placeholder(value: Component) = TagResolver.resolver(this, Tag.selfClosingInserting(value))

interface MessageContainer {
    operator fun get(id: String): Component
    operator fun get(id: String, vararg placeholders: TagResolver): Component
}

class PluginMessageContainer(private val plugin: PlaytimePlugin) : MessageContainer {
    private val messages: Map<String, String>

    init {
        val file = fetchAndSaveResource("messages.yml")!!
        val yaml = YamlConfiguration.loadConfiguration(file)
        messages = yaml.getKeys(false).associateWith { yaml.getString(it) ?: it }
    }

    override fun get(id: String): Component {
        return get(id, emptyResolver)
    }

    override fun get(id: String, vararg placeholders: TagResolver): Component {
        val message = messages[id] ?: return Component.text(id)
        return resolver.deserialize(message, *placeholders)
    }

    private fun fetchAndSaveResource(name: String): File? {
        val directory = plugin.dataFolder
        if (!directory.exists()) directory.mkdir()

        val file = File(directory, name)
        if (!file.exists()) {
            if (plugin.getResource(name) == null) {
                return null
            }

            plugin.saveResource(name, true)
        }

        return file
    }

    private companion object {
        val resolver = MiniMessage.miniMessage()
        val emptyResolver = TagResolver.builder().build()
    }
}