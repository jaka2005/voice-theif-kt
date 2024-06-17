@file:OptIn(KordVoice::class)

package funn.j2k

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import dev.kord.common.annotation.KordVoice
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.BaseVoiceChannelBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.exception.GatewayNotFoundException
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.voice.VoiceConnection
import dev.kord.voice.VoiceConnectionBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

enum class Command {
    JOIN, LEAVE, START, STOP
}

private val connections: MutableMap<Snowflake, VoiceConnection> = mutableMapOf()
private val lavaplayerManager = DefaultAudioPlayerManager()

suspend fun main() {
    val kord = Kord(System.getenv("TOKEN"))
    registerCommandsOnGuild(kord, System.getenv("GUILD_ID").toLong())

    kord.on<ReadyEvent> {
        println("Ready")
    }

    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        val response = interaction.deferPublicResponse()

        val command: Command
        try {
            command = Command.valueOf(
                interaction.command.rootName.uppercase()
            )
        } catch (e: IllegalStateException) {
            response.respond { content = "Unresolved command!" }
            return@on
        }

        val text = when(command) {
            Command.JOIN -> joinToChannel()
            Command.LEAVE -> leaveFromChannel()
            Command.START -> startRecording()
            Command.STOP -> stopRecording()
        }

        response.respond { content = text }
    }

    kord.login {
        intents += Intent.Guilds
    }
}

suspend fun registerCommandsOnGuild(kord: Kord, guildId: Long) {
    kord.createGuildChatInputCommand(
        Snowflake(guildId),
        "join",
        "join to channel"
    )
    kord.createGuildChatInputCommand(
        Snowflake(guildId),
        "leave",
        "leave the channel"
    )
    kord.createGuildChatInputCommand(
        Snowflake(guildId),
        "start",
        "start voice recording"
    )
    kord.createGuildChatInputCommand(
        Snowflake(guildId),
        "stop",
        "stop recording"
    )
}

suspend fun GuildChatInputCommandInteractionCreateEvent.joinToChannel(): String {
    val channel = interaction.user.getVoiceState().getChannelOrNull()
        ?: return "Please connect to voice channel first"

    val guildId = interaction.guildId

    if (connections.contains(guildId)) {
        connections.remove(guildId)!!.shutdown()
    }

    val connection = channel.connect {
        receiveVoice = true
    }

    connections[guildId] = connection

    return "Join!"
}

suspend fun GuildChatInputCommandInteractionCreateEvent.leaveFromChannel(): String {
    val guildId = interaction.guildId

    if (connections.contains(guildId)) {
        connections.remove(guildId)!!.shutdown()
    }

    return "Leave("
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun GuildChatInputCommandInteractionCreateEvent.startRecording(): String {
    val guildId = interaction.guildId

    GlobalScope.launch {
        connections[guildId]?.let {
            it.streams.incomingAudioFrames.collect { data ->
                val userId = data.first
                val audioFrame = data.second

                val file = File("$userId.wav")
                file.appendBytes(audioFrame.data)
            }
        }
    }

    return "recording!"
}

suspend fun GuildChatInputCommandInteractionCreateEvent.stopRecording(): String {
    TODO()
}

suspend fun BaseVoiceChannelBehavior.connect(builder: VoiceConnectionBuilder.() -> Unit): VoiceConnection {
    val voiceConnection = VoiceConnection(
        guild.gateway ?: GatewayNotFoundException.voiceConnectionGatewayNotFound(guildId),
        kord.selfId,
        id,
        guildId,
        builder
    )

    voiceConnection.connect()

    return voiceConnection
}
