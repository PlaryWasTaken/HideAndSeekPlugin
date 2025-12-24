package org.plary.hide.utils

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import java.util.concurrent.CompletableFuture


class EnumArgumentType<T : Enum<T>>(
    private val enumClass: Class<T>
) : CustomArgumentType<T, String> {

    override fun parse(reader: StringReader): T {
        val input = reader.readUnquotedString()
        return enumClass.enumConstants.firstOrNull {
            it.name.equals(input, true)
        } ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS
            .dispatcherUnknownArgument()
            .create()
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        enumClass.enumConstants.forEach {
            builder.suggest(it.name.lowercase())
        }
        return builder.buildFuture()
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }
}