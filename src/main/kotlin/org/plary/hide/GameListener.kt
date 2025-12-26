package org.plary.hide

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.plary.hide.utils.GameModeType

class GameListener(
    private val gameManager: GameManager,
    private val plugin: HideAndSeekPlugin
) : Listener {

    /**
     * Handle player deaths during the game
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val victim = event.player

        // Ignore players not in the game
        if (!gameManager.isPlaying(victim)) return
        val killer = victim.killer
        // Should only remove when killed by a seeker
        plugin.logger.info("Player ${victim.name} died. Killer: ${killer?.name ?: "None"}")
        plugin.logger.info("Game mode: ${gameManager.mode}")
        when (gameManager.mode) {
            GameModeType.Normal -> {
                if (killer != null && gameManager.isSeeker(killer)) {
                    gameManager.removePlayer(victim)
                    gameManager.checkWinCondition()
                }
            }

            GameModeType.Zombie -> {
                if (killer != null && gameManager.isSeeker(killer)) {
                    gameManager.setSeeker(victim)
                    gameManager.checkWinCondition()
                }
            }
            GameModeType.Tag -> {
                if (killer != null && gameManager.isSeeker(killer)) {
                    gameManager.setHider(killer)
                    gameManager.setSeeker(victim)
                }
            }
        }
    }

    /**
     * Re-apply effects & items after respawn
     */
    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        if (!gameManager.isPlaying(player)) return

        // Run 2 ticks later so inventory/meta is safe to modify
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            when {
                gameManager.isSeeker(player) -> {
                    gameManager.setSeeker(player)
                }

                gameManager.isHider(player) -> {
                    gameManager.setHider(player)
                }
            }
        }, 2L)
    }
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // If the game is ongoing, set as spectator
        if (gameManager.ongoing) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {

                gameManager.setHider(player)
            }, 2L)
        }
    }
    @EventHandler
    fun onEntityHit(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        if (damager !is org.bukkit.entity.Player || victim !is org.bukkit.entity.Player) return

        // Only seekers can damage hiders
        if (gameManager.ongoing && gameManager.mode == GameModeType.Tag) {
            if (gameManager.isSeeker(damager) && gameManager.isHider(victim)) {
                gameManager.setSeeker(victim)
                gameManager.setHider(damager)
                event.damage = 0.0
            }
            return
        }

    }
}
