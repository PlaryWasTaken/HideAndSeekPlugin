package org.plary.hide

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

class ScoreboardManager {

    private val board: Scoreboard =
        Bukkit.getScoreboardManager().newScoreboard

    val scoreboard: Scoreboard
        get() = board

    private val objective = board.registerNewObjective(
        "hideandseek",
        Criteria.DUMMY,
        Component.text("Esconde Esconde")
    ).apply {
        displaySlot = DisplaySlot.SIDEBAR
    }

    val seekersTeam: Team = board.registerNewTeam("seekers").apply {
        color(NamedTextColor.RED)
        setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
    }

    val hidersTeam: Team = board.registerNewTeam("hiders").apply {
        color(NamedTextColor.GREEN)
        setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM)
    }

    val spectatorsTeam: Team = board.registerNewTeam("spectators").apply {
        color(NamedTextColor.GRAY)
        setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)
    }

    fun update(time: Int, seeking: Boolean, hiders: Int, seekers: Int, plugin: Hide) {
        board.entries.forEach { board.resetScores(it) }

        objective.getScore(
            "§bTempo: §e${if (seeking) "Procurando" else "Escondendo"}: $time"
        ).score = 4

        objective.getScore("§aEscondendo: $hiders").score = 3
        objective.getScore("§cProcurando: $seekers").score = 2
        objective.getScore(
            "§fModo: ${plugin.config.getString("mode", "Normal")}"
        ).score = 1
        //Bukkit.getOnlinePlayers().forEach { it.scoreboard = board }
    }

    fun end(winners: List<Player>) {
        board.entries.forEach { board.resetScores(it) }

        objective.getScore(
            "§fVencedores: §a${winners.joinToString(", ") { it.name }}"
        ).score = 1
    }
}