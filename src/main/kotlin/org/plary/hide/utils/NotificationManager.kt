package org.plary.hide.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.scoreboard.Team
import org.plary.hide.HideAndSeekPlugin
import org.plary.hide.ScoreboardManager

class NotificationManager(private val plugin: HideAndSeekPlugin) {
    fun sendActionBarMessage(message: Component, team: Team) {
        team.entries.mapNotNull { plugin.server.getPlayerExact(it) }.forEach { p ->
            p.sendActionBar(message)
        }
    }
    fun sendTitleMessage(title: Title, team: Team) {
        team.entries.mapNotNull { plugin.server.getPlayerExact(it) }.forEach { p ->
            p.showTitle(title)
        }
    }

}