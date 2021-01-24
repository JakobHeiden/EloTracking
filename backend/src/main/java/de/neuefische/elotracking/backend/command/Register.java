package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class Register extends Command {
    private static Mono<MessageChannel> channelMono;
    public Register(DiscordBot bot, EloTrackingService service, Message msg, Mono<MessageChannel> channelMono) {
        super(bot, service, msg);
        this.channelMono = channelMono;
    }

    public static String getDescription() {
        return "!register NameOfGame - Register a new leaderboard, linking it to this channel";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String nameOfNewGame = msg.getContent().substring(1 + "register".length()).trim();
        if (nameOfNewGame.equals("")) {
            botReplies.add("needs name");
            canExecute = false;
        }
        Optional<Game> existingGame = service.findGameByChannelId(this.channelId);
        if (existingGame.isPresent()) {
            botReplies.add(String.format("There is already a game associated with this channel: %s", existingGame.get().getName()));
            canExecute = false;
        }
        if (!canExecute) return;

        service.saveGame(new Game(this.channelId, nameOfNewGame));
        botReplies.add(String.format(String.format("New game created. You can now %schallenge another player",
                service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX"))));

        //update channel description to include the new leaderboard URL
        Consumer<TextChannelEditSpec> editConsumer =
                textChannelEditSpec -> textChannelEditSpec.setTopic(
                        String.format("Leaderboard: http://%s/%s",
                                service.getConfig().getProperty("BASE_URL"),
                                this.channelId));
        Channel channel = channelMono.block();
        ((TextChannel) channel).edit(editConsumer).subscribe();
        botReplies.add("I updated the channel description.");
    }
}
