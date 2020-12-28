package de.neuefische.elotracking.backend.discord;

import de.neuefische.elotracking.backend.command.*;
import de.neuefische.elotracking.backend.common.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DiscordBot {
    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;
    private final ApplicationPropertiesLoader config;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient,
                      EloTrackingService eloTrackingService,
                      ApplicationPropertiesLoader applicationPropertiesLoader) {
        this.client = gatewayDiscordClient;
        this.service = eloTrackingService;
        this.config = applicationPropertiesLoader;

        String adminId = config.getProperty("ADMIN_DISCORD_ID");
        this.adminMentionAsString = String.format("<@%s>", adminId);
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        log.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(msgEvent -> {
                    log.trace(msgEvent.toString());
                    log.debug("Incoming message: " + msgEvent.getMessage().getContent());
                    return msgEvent.getMessage();
                })
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(this::isCommand)
                .subscribe(this::parseCommand);

        client.getEventDispatcher().on(Event.class)
                .subscribe(logs -> log.trace(logs.toString()));
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    private boolean isCommand(Message msg) {
        return service.isCommand(
                msg.getChannel().block().getId().asString(),
                msg.getContent().substring(0,1));
    }

    private void parseCommand(Message msg) {
        log.debug("Parsing command: " + msg.getContent());
        String commandString = msg.getContent().substring(1).split(" ")[0];
        MessageChannel channel = msg.getChannel().block();
        Command command;
        switch(commandString) {
            case "register":
                command = new Register(this, service, msg, channel);
                break;
            case "challenge":
                command = new Challenge(this, service, msg, channel);
                break;
            case "accept":
                command = new Accept(this, service, msg, channel);
                break;
            case "win":
                report(msg, channel, true);
                return;
            case "lose":
                report(msg, channel, false);
                return;
            case "help":
                command = new Help(this, service, msg, channel);
                break;
            case "setprefix":
                command = new SetPrefix(this, service, msg, channel);
                break;
            default:
                    channel.createMessage("Unknown command " + commandString).subscribe();
                    return;
        }
        command.execute();
        for (String reply : command.getBotReplies()) {
            channel.createMessage(reply).subscribe();
        }
    }

    private void report(Message msg, MessageChannel channel, boolean isWin) {
        if (msg.getUserMentionIds().size() != 1) {
            channel.createMessage(String.format("You need to tag one and only one Discord user with this command, " +
                    "e.g. %s%s @somebody", msg.getContent().charAt(0), isWin ? "win" : "lose")).subscribe();
            return;
        }

        String replyFromService = service.report(
                channel.getId().asString(),
                msg.getAuthor().get().getId().asString(),
                msg.getUserMentionIds().iterator().next().asString(),
                isWin);
        String winnerMention = isWin ? msg.getAuthor().get().getMention() : msg.getUserMentions().blockFirst().getMention();
        String loserMention = !isWin ? msg.getAuthor().get().getMention() : msg.getUserMentions().blockFirst().getMention();
        channel.createMessage(String.format(replyFromService, winnerMention, loserMention)).subscribe();
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
