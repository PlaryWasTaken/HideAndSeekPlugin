package org.plary.hide.utils

import org.bukkit.entity.Player
import org.plary.hide.HideAndSeekPlugin

object ItemHelper {
    fun giveItems(plugin: HideAndSeekPlugin, player: Player, path: String) {
        player.inventory.clear()

        val list = plugin.config.getMapList(path)
        for (entry in list) {
            val section = plugin.config.createSection("temp", entry)
            val item = ItemBuilder.fromSection(section)
            player.inventory.addItem(item)
            plugin.config.set("temp", null)
        }
    }
}