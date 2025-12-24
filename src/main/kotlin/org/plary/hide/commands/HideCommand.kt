package org.plary.hide.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.plary.hide.GameModeType
import org.plary.hide.Hide
import org.plary.hide.utils.EnumArgumentType


class HideCommand {

    fun createCommand(plugin: Hide): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("hide")
            .then(
                Commands.literal("start")
                    .executes { ctx -> runStart(ctx, plugin) }
            )
            .then(
                Commands.literal("procuradorInicial").then(
                    Commands.argument("procuradores", ArgumentTypes.players())
                        .executes { ctx -> configureSeekers(ctx, plugin) }
                )
            )
            .then(
                Commands.literal("encerrar")
                    .executes { ctx -> runEndGame(ctx, plugin) }
            )
            .then(
                Commands.literal("modo").then(
                    Commands.argument("modo", EnumArgumentType(GameModeType::class.java))
                        .executes { ctx -> setMode(ctx, plugin) }
                )
            )
            .then(
                Commands.literal("tempo")
                    .then(
                        Commands.literal("esconder")
                            .then(
                                Commands.argument("segundos", IntegerArgumentType.integer(0))
                                    .executes { ctx -> setConfigInt("segundos", "hidingTime", ctx, plugin) }
                            )
                    )
                    .then(
                        Commands.literal("procurar")
                            .then(
                                Commands.argument("segundos", IntegerArgumentType.integer(0))
                                    .executes { ctx -> setConfigInt("segundos", "seekingTime", ctx, plugin) }
                            )
                    )
                    .then(
                        Commands.literal("intervalo")
                            .then(
                                Commands.literal("glow")
                                    .then(
                                        Commands.argument("segundos", IntegerArgumentType.integer(0))
                                            .executes { ctx -> setConfigInt("segundos", "hiderGlowInterval", ctx, plugin) }
                                    )
                            )
                    )
                    .then(
                        Commands.literal("glow")
                            .then(
                                Commands.argument("segundos", IntegerArgumentType.integer(0))
                                    .executes { ctx -> setConfigInt("segundos", "hiderGlowTime", ctx, plugin) }
                            )
                    )
            )
    }
    private fun runStart(ctx: CommandContext<CommandSourceStack>, plugin: Hide): Int {
        val sender = ctx.source.sender // Retrieve the command sender
        val executor = ctx.source.executor

        if (executor !is Player) {
            sender.sendPlainMessage("Only players can start the game!")
            return Command.SINGLE_SUCCESS
        }

        sender.sendPlainMessage("Começando jogo de esconde esconde..")
        plugin.gameMaster.startGame()

        return Command.SINGLE_SUCCESS
    }
    private fun runEndGame(ctx: CommandContext<CommandSourceStack>, plugin: Hide): Int {
        val sender = ctx.source.sender
        sender.sendMessage(Component.text("Encerrando o jogo de esconde esconde..", NamedTextColor.RED))
        plugin.gameMaster.endGame()
        return Command.SINGLE_SUCCESS
    }
    private fun configureSeekers(ctx: CommandContext<CommandSourceStack>, plugin: Hide): Int {
        val targetResolver = ctx.getArgument("procuradores", PlayerSelectorArgumentResolver::class.java)
        val targets = targetResolver.resolve(ctx.source)
        val sender = ctx.source.sender
        val executor = ctx.source.executor
        if (executor !is Player) {
            sender.sendMessage("Only players can start the game!")
            return Command.SINGLE_SUCCESS
        }
        plugin.gameMaster.setInitialSeekers(targets)
        sender.sendMessage(Component.text("Procuradores configurados com sucesso!"))
        return Command.SINGLE_SUCCESS
    }
    private fun setMode(ctx: CommandContext<CommandSourceStack>, plugin: Hide): Int {
        val sender = ctx.source.sender
        sender.sendMessage(Component.text("Modo de jogo alterado para ${ctx.getArgument("modo", GameModeType::class.java)}"))
        plugin.config.set("mode", ctx.getArgument("modo", GameModeType::class.java).toString())
        return Command.SINGLE_SUCCESS
    }
    private fun setConfigInt(argName: String, configName: String, ctx: CommandContext<CommandSourceStack>, plugin: Hide): Int {
        val sender = ctx.source.sender
        val value = ctx.getArgument(argName, Integer::class.java)
        if (value > 99999) {
            sender.sendMessage(Component.text("Valor inválido para $argName!", NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }
        plugin.config.set(configName, value)
        sender.sendMessage(Component.text("Configuração '$configName' alterada para $value segundos!", NamedTextColor.GREEN))
        return Command.SINGLE_SUCCESS
    }
}