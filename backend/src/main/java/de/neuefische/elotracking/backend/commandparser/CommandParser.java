package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Component
public class CommandParser {

    private String defaultCommandPrefix;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;
    private final Function<MessageWrapper, Command> commandFactory;

    @Autowired
    public CommandParser(EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, Function<MessageWrapper, Command> commandFactory) {
        this.service = service;
        this.bot = bot;
        this.queue = queue;
        this.commandFactory = commandFactory;
        this.defaultCommandPrefix = service.getPropertiesLoader().getDefaultCommandPrefix();
    }

    public boolean isCommand(Message msg) {
        try {
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
        } catch (Exception e) {
            bot.sendToAdmin(String.format("Error in CommandParser::isCommand/n%s : %s\n%s",
                    msg.getChannelId().asString(), msg.getContent(), e.getMessage()));
            throw e;
        }
    }

    public void processCommand(Message msg) {
        try {// TODO!
            Mono<MessageChannel> channelMono = msg.getChannel();
            MessageWrapper msgWrapper = new MessageWrapper(msg, service, bot, queue);
            Command command = commandFactory.apply(msgWrapper);
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
            bot.sendToAdmin(String.format("Error in CommandParser::parseCommand/n%s : %s\n%s",
                    msg.getChannelId().asString(), msg.getContent(), e.getMessage()));
            throw e;
        }
    }
}
