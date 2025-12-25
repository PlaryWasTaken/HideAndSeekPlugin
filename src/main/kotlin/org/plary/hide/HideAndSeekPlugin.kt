package org.plary.hide

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import org.plary.hide.commands.HideCommand

class HideAndSeekPlugin : JavaPlugin() {
    var gameMaster = GameManager(this)
    override fun onEnable() {
        // Plugin startup logic
        config.addDefaults(
            mapOf(
                "mode" to "Normal",
                "hidingTime" to 60,
                "seekingTime" to 300,
                "hiderGlowInterval" to 20,
                "seekersItems" to listOf(
                    mapOf(
                        "material" to "netherite_sword",
                        "amount" to 1,
                        "name" to "&aSeeker Sword",
                        "enchantments" to mapOf(
                            "sharpness" to 5
                        ),
                        "unbreakable" to true
                    )
                ),
                "hidersItems" to listOf(
                    mapOf(
                        "material" to "stick",
                        "amount" to 1,
                        "name" to "&bKnockback Stick",
                        "enchantments" to mapOf(
                            "knockback" to 2
                        )
                    )
                )
            )
        )
        config.options().copyDefaults(true)
        saveConfig()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(HideCommand().createCommand(this ).build())
        }
        server.pluginManager.registerEvents(
            GameListener(gameMaster, this),
            this
        )
        gameMaster.init()

    }

    override fun onDisable() {
        // Plugin shutdown logic
        saveConfig()
    }
}
