package org.plary.hide

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class GameListener(
    private val gameManager: GameManager,
    private val plugin: Hide
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
                    gameManager.setSeeker(victim)
                    gameManager.setHider(killer)
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

        // Run 1 tick later so inventory/meta is safe to modify
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            when {
                gameManager.isSeeker(player) -> {
                    gameManager.setSeeker(player)
                }

                gameManager.isHider(player) -> {
                    gameManager.restoreHider(player)
                }
            }
        }, 2L)
    }
}
