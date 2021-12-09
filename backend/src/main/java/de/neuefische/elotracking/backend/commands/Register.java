package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
public class Register extends Command {

    private String baseUrl;

    public Register(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        super(msg, service, bot, queue);
        this.baseUrl = service.getPropertiesLoader().getBaseUrl();
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
        addBotReply(String.format(String.format("New game created. You can now %schallenge another player", defaultCommandPrefix)));

        //update channel description to include the new leaderboard URL
        ((TextChannel) channelMono.block()).edit()
                .withTopic(String.format("Leaderboard: http://%s/%s", baseUrl, this.channelId)).subscribe();
        addBotReply("I updated the channel description.");
    }
}
