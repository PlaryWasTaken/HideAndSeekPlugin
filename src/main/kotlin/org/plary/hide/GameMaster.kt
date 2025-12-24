package org.plary.hide

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcast
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class GameManager(private val plugin: Hide) {

    private val hiders = mutableSetOf<UUID>()
    private val seekers = mutableSetOf<UUID>()
    private val initialSeekers = mutableSetOf<UUID>()
    private var timeLeft = 0
    private var seekingPhase = false
    private var gameEnded = false
    private lateinit var scoreboardManager: ScoreboardManager
    val mode = GameModeType.valueOf(plugin.config.getString("mode", "Normal")!!)
    fun init() {
        scoreboardManager = ScoreboardManager()
    }
    fun startGame() {
        val players = Bukkit.getOnlinePlayers().iterator()

        players.forEachRemaining {
            hiders.add(it.uniqueId)
            if (it.uniqueId in initialSeekers) {
                this.setSeeker(it)
            }
        }

        setupPlayers()
        gameEnded = false
        seekingPhase = false
        timeLeft = plugin.config.getInt("hidingTime")
        startTimer()
        startHiderGlowTask()
    }
    fun setInitialSeekers(players: List<Player>) {
        initialSeekers.clear()
        players.forEach {
            initialSeekers.add(it.uniqueId)
        }
    }
    fun setSeeker(player: Player) {
        hiders.remove(player.uniqueId)
        seekers.add(player.uniqueId)
        scoreboardManager.seekersTeam.addEntry(player.name)
        player.scoreboard = scoreboardManager.scoreboard
        giveItems(player, "seekersItems")
        player.addPotionEffect(
            PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false)
        )

        //Bukkit.broadcastMessage("§c${player.name} is now a seeker!")
    }
    fun setHider(player: Player) {
        seekers.remove(player.uniqueId)
        hiders.add(player.uniqueId)
        scoreboardManager.hidersTeam.addEntry(player.name)
        player.scoreboard = scoreboardManager.scoreboard
        giveItems(player, "hidersItems")
        //Bukkit.broadcastMessage("§a${player.name} is now a hider!")
    }
    fun endGame() {
        gameEnded = true
        seekingPhase = false
        hiders.forEach {
            removePlayer(Bukkit.getPlayer(it)!!)
        }
        seekers.forEach {
            removePlayer(Bukkit.getPlayer(it)!!)
        }
        broadcast(Component.text("Partida Cancelada", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)

    }

    private fun setupPlayers() {
        seekers.forEach {
            Bukkit.getPlayer(it)?.let { p ->
                giveItems(p, "seekersItems")
                scoreboardManager.seekersTeam.addEntry(p.name)
                p.scoreboard = scoreboardManager.scoreboard
                p.addPotionEffect(
                    PotionEffect(PotionEffectType.GLOWING, Int.MAX_VALUE, 0, false, false)
                )
                p.addPotionEffect(
                    PotionEffect(PotionEffectType.BLINDNESS, plugin.config.getInt("hidingTime") * 20, 5, false, false)
                )
                p.addPotionEffect(
                    PotionEffect(PotionEffectType.SLOWNESS, plugin.config.getInt("hidingTime") * 20, 10, false, false)
                )
            }
        }

        hiders.forEach {
            Bukkit.getPlayer(it)?.let { p ->
                scoreboardManager.hidersTeam.addEntry(p.name)
                p.scoreboard = scoreboardManager.scoreboard
                giveItems(p, "hidersItems")
            }
        }
    }

    private fun giveItems(player: Player, path: String) {
        player.inventory.clear()

        val list = plugin.config.getMapList(path)
        for (entry in list) {
            val section = plugin.config.createSection("temp", entry)
            val item = ItemBuilder.fromSection(section)
            player.inventory.addItem(item)
            plugin.config.set("temp", null)
        }
    }

    fun isPlaying(player: Player) = player.uniqueId in hiders || player.uniqueId in seekers
    fun isSeeker(player: Player) = player.uniqueId in seekers
    fun isHider(player: Player) = player.uniqueId in hiders
    fun removePlayer(player: Player) {
        hiders.remove(player.uniqueId)
        seekers.remove(player.uniqueId)
        scoreboardManager.hidersTeam.removeEntry(player.name)
        scoreboardManager.seekersTeam.removeEntry(player.name)
        player.inventory.clear()
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
    }
    fun restoreHider(player: Player) {
        if (isHider(player)) {
            giveItems(player, "hidersItems")
        }
    }
    fun checkWinCondition() {
        if (hiders.isEmpty()) {
            broadcast(Component.text("Procuradores ganham!", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)
            gameEnded = true
        }
    }
    private fun startTimer() {
        object : BukkitRunnable() {
            override fun run() {
                if (gameEnded) {
                    cancel()
                    return
                }
                if (timeLeft <= 0) {
                    if (!seekingPhase) {
                        seekingPhase = true
                        timeLeft = plugin.config.getInt("seekingTime")
                        broadcast(Component.text("Procuradores liberados!", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)
                    } else {
                        broadcast(Component.text("Escondidos ganham!", NamedTextColor.GREEN), Server.BROADCAST_CHANNEL_USERS)
                        seekingPhase = false
                        cancel()
                        return
                    }
                }

                scoreboardManager.update(timeLeft, seekingPhase, hiders.size, seekers.size, plugin)
                timeLeft--
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }
    private fun startHiderGlowTask() {
        val interval = plugin.config.getInt("hiderGlowInterval") * 20L
        val glowTime = plugin.config.getInt("hiderGlowTime")
        object : BukkitRunnable() {
            override fun run() {
                if (gameEnded) {
                    cancel()
                    return
                }
                if (!seekingPhase) return

                hiders.forEach {
                    val player = Bukkit.getPlayer(it)
                    player?.addPotionEffect(
                        PotionEffect(PotionEffectType.GLOWING,  glowTime * 20, 200, false, false)
                    )
                    player?.sendActionBar(Component.text("Você está GLOWER por $glowTime segundo(s)!", NamedTextColor.BLUE))
                    plugin.logger.info(scoreboardManager.hidersTeam.entries.toString())
                }
            }
        }.runTaskTimer(plugin, interval, interval)
    }
}
