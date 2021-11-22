package de.neuefische.elotracking.backend.parser;

import de.neuefische.elotracking.backend.command.Command;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class CommandParser {

    @Value("${default-command-prefix}")
    private String defaultCommandPrefix;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final Function<Message, Command> commandFactory;

    @Autowired
    public CommandParser(EloTrackingService service, DiscordBotService bot, Function<Message, Command> commandFactory) {
        this.service = service;
        this.bot = bot;
        this.commandFactory = commandFactory;
    }

    public boolean isCommand(Message msg) {
        log.trace("Incoming message: " + msg.getContent());
        if (msg.getContent().length() < 2) return false;

        String necessaryPrefix;
        Optional<Game> game = service.findGameByChannelId(msg.getChannelId().asString());
        if (game.isPresent()) {
            necessaryPrefix = game.get().getCommandPrefix();
        } else {
            necessaryPrefix = defaultCommandPrefix;
        }
        if (msg.getContent().startsWith(necessaryPrefix)) {
            log.debug(String.format("Channel %s : %s", msg.getChannelId().asString(), game.isPresent() ? game.get().getName() : "NULL"));
            return true;
        }

        return false;
    }

    public void processCommand(Message msg) {
        try {// TODO!
            Mono<MessageChannel> channelMono = msg.getChannel();
            Command command = commandFactory.apply(msg);
            log.debug(String.format("new %s(%s) : execute()",
                    command.getClass().getSimpleName(),
                    msg.getContent()));
            command.execute();
            MessageChannel channel = channelMono.block();
            //command.getBotReplies().forEach(channel::createMessage); TODO
            for (String reply : command.getBotReplies()) {
                channel.createMessage(reply).subscribe();
            }
        } catch(Exception e) {
            bot.sendToAdmin(String.format("%s: %s:\n%s", msg.getChannelId().asString(), msg.getContent(), e.getMessage()));
            throw e;
        }
    }
}
