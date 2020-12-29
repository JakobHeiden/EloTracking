package de.neuefische.elotracking.backend.discord;

import de.neuefische.elotracking.backend.command.*;
import de.neuefische.elotracking.backend.common.ApplicationPropertiesLoader;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
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
                command = new Report(this, service, msg, channel, ChallengeModel.ReportStatus.WIN);
                break;
            case "lose":
                command = new Report(this, service, msg, channel, ChallengeModel.ReportStatus.LOSS);
                break;
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

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
