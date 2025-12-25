package org.plary.hide

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Bukkit.broadcast
import org.bukkit.GameMode
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import org.plary.hide.utils.GameModeType
import org.plary.hide.utils.ItemHelper.giveItems
import org.plary.hide.utils.NotificationManager
import java.util.*

class GameManager(private val plugin: HideAndSeekPlugin) {

    private val hiders = mutableSetOf<UUID>()
    private val seekers = mutableSetOf<UUID>()
    val initialSeekers = mutableSetOf<UUID>()
    private var timeLeft = 0
    private var seekingPhase = false
    private var activeTasks: MutableList<BukkitTask> = mutableListOf()
    private lateinit var scoreboardManager: ScoreboardManager
    private lateinit var notificationManager: NotificationManager
    var mode = GameModeType.Normal // Default to normal but should be set on game start
        private set
    var ongoing: Boolean = false
        private set
    fun isPlaying(player: Player) = player.uniqueId in hiders || player.uniqueId in seekers
    fun isSeeker(player: Player) = player.uniqueId in seekers
    fun isHider(player: Player) = player.uniqueId in hiders
    fun init() {
        scoreboardManager = ScoreboardManager()
        notificationManager = NotificationManager(plugin)
    }
    fun startGame() {
        val players = Bukkit.getOnlinePlayers().iterator()
        mode = GameModeType.valueOf(plugin.config.getString("mode", "Normal")!!)
        // ** Player setup **
        players.forEachRemaining {
            it.gameMode = GameMode.ADVENTURE
            it.scoreboard = scoreboardManager.scoreboard
            if (it.uniqueId in initialSeekers) this.setSeeker(it)
            else this.setHider(it)
        }
        ongoing = true
        seekingPhase = mode == GameModeType.Tag
        timeLeft = if (seekingPhase) plugin.config.getInt("seekingTime") else plugin.config.getInt("hidingTime")
        activeTasks.add(startTimer())
        activeTasks.add(startHiderGlowTask())
    }
    fun setSeeker(player: Player) {
        hiders.remove(player.uniqueId)
        seekers.add(player.uniqueId)
        scoreboardManager.seekersTeam.addEntry(player.name)
        giveItems(plugin, player, "seekersItems")
        setSeekerEffects(player)
    }
    private fun setSeekerEffects(player: Player) {
        player.clearActivePotionEffects()
        player.addPotionEffect(
            PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false)
        )
        player.addPotionEffect(
            PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0, false, false)
        )
        player.addPotionEffect(
            PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 30, false, false)
        )
        if (!seekingPhase) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.BLINDNESS, plugin.config.getInt("hidingTime") * 20, 5, false, false)
            )
            player.addPotionEffect(
                PotionEffect(PotionEffectType.SLOWNESS, plugin.config.getInt("hidingTime") * 20, 10, false, false)
            )
        }
    }
    fun setHider(player: Player) {

        seekers.remove(player.uniqueId)
        hiders.add(player.uniqueId)
        scoreboardManager.hidersTeam.addEntry(player.name)
        giveItems(plugin, player, "hidersItems")
        setHiderEffects(player)
    }
    private fun setHiderEffects(player: Player) {
        player.clearActivePotionEffects()
        player.addPotionEffect(
            PotionEffect(PotionEffectType.SATURATION, PotionEffect.INFINITE_DURATION, 0, false, false)
        )
        if (mode == GameModeType.Tag) {
            player.addPotionEffect(
                PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false)
            )
        }
    }
    fun endGame() {
        plugin.logger.info("Ending game")
        ongoing = false
        seekingPhase = false
        plugin.logger.info("Removing hiders")
        val endGameTitle = Title.title(Component.text("Fim de jogo!", NamedTextColor.RED),
            Component.text(""))
        hiders.toList().mapNotNull { Bukkit.getPlayer(it) }.forEach {
            it.showTitle(endGameTitle)
            removePlayer(it)
        }
        hiders.clear()
        plugin.logger.info("Removing seekers")
        seekers.toList().mapNotNull { Bukkit.getPlayer(it) }.forEach {
            it.showTitle(endGameTitle)
            removePlayer(it)
        }
        seekers.clear()
        plugin.logger.info("Cancelling active tasks")
        activeTasks.forEach { it.cancel() }
        activeTasks.clear()
        broadcast(Component.text("Partida Terminada", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)
    }

    fun removePlayer(player: Player) {
        plugin.logger.info("Removing player ${player.name}")
        hiders.remove(player.uniqueId)
        seekers.remove(player.uniqueId)
        scoreboardManager.spectatorsTeam.addEntry(player.name)
        player.inventory.clear()
        player.clearActivePotionEffects()

        plugin.logger.info("Finished removing player")
    }
    fun checkWinCondition() {
        if (hiders.isEmpty()) {
            broadcast(Component.text("Procuradores ganham!", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)
            this.endGame()
        }
    }
    private fun startTimer(): BukkitTask {
        return object : BukkitRunnable() {
            override fun run() {
                if (!ongoing) return cancel()

                if (timeLeft <= 0) {
                    if (!seekingPhase) {
                        seekingPhase = true
                        timeLeft = plugin.config.getInt("seekingTime")
                        broadcast(Component.text("Procuradores liberados!", NamedTextColor.RED), Server.BROADCAST_CHANNEL_USERS)
                    } else {
                        scoreboardManager.end(hiders.mapNotNull { Bukkit.getPlayer(it) })
                        broadcast(Component.text("Escondidos ganham!", NamedTextColor.GREEN), Server.BROADCAST_CHANNEL_USERS)
                        endGame()
                        return
                    }
                }

                scoreboardManager.update(timeLeft, seekingPhase, hiders.size, seekers.size, plugin)
                timeLeft--
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }
    private fun startHiderGlowTask(): BukkitTask {
        val interval = plugin.config.getInt("hiderGlowInterval") * 20L
        val glowTime = plugin.config.getInt("hiderGlowTime") * 20
        return object : BukkitRunnable() {
            override fun run() {
                if (!ongoing) return cancel()

                if (!seekingPhase) return
                hiders.mapNotNull { Bukkit.getPlayer(it) }.forEach {
                    it.addPotionEffect(
                        PotionEffect(PotionEffectType.GLOWING,  glowTime, 200, false, false)
                    )
                    it.sendActionBar(Component.text("Você está GLOWER por $glowTime segundo(s)!", NamedTextColor.BLUE))
                }
                notificationManager.sendActionBarMessage(Component.text("Os que estão escondendo brilharão por $glowTime segundo(s)!", NamedTextColor.BLUE), scoreboardManager.seekersTeam)
            }
        }.runTaskTimer(plugin, interval, interval)
    }

}
