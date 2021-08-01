package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.model.Game;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class Register extends Command {

    public Register(Message msg) {
        super(msg);
    }

    public static String getDescription() {
        return "!register NameOfGame - Register a new leaderboard, linking it to this channel";
    }

    public void execute() {
        boolean canExecute = super.canExecute();
        String nameOfNewGame = msg.getContent().substring(1 + "register".length()).trim();
        if (nameOfNewGame.equals("")) {
            addBotReply("needs name");
            canExecute = false;
        }
        if (nameOfNewGame.equals("NULL")) {
            addBotReply("Invalid name");
            canExecute = false;
        }
        Optional<Game> existingGame = service.findGameByChannelId(this.channelId);
        if (existingGame.isPresent()) {
            addBotReply(String.format("There is already a game associated with this channel: %s", existingGame.get().getName()));
            canExecute = false;
        }
        if (!canExecute) return;

        Mono<MessageChannel> channelMono = msg.getChannel();

        service.saveGame(new Game(this.channelId, nameOfNewGame));
        addBotReply(String.format(String.format("New game created. You can now %schallenge another player",
                service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX"))));

        //update channel description to include the new leaderboard URL
        Consumer<TextChannelEditSpec> editConsumer =
                textChannelEditSpec -> textChannelEditSpec.setTopic(
                        String.format("Leaderboard: http://%s/%s",
                                service.getConfig().getProperty("BASE_URL"),
                                this.channelId));
        ((TextChannel) channelMono.block()).edit(editConsumer).subscribe();
        addBotReply("I updated the channel description.");
    }
}
