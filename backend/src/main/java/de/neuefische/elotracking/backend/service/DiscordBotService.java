package de.neuefische.elotracking.backend.service;

import de.neuefische.elotracking.backend.commandparser.CommandParser;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class DiscordBotService {

    private final GatewayDiscordClient client;
    private String adminId;
    private PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;

    @Autowired
    public DiscordBotService(GatewayDiscordClient gatewayDiscordClient, EloTrackingService service, @Lazy CommandParser commandParser) {
        this.client = gatewayDiscordClient;
        this.adminId = service.getPropertiesLoader().getAdminId();

        this.adminMentionAsString = String.format("<@%s>", adminId);
        log.info(System.getenv("DATABASE"));//TODO

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(msgEvent -> msgEvent.getMessage())
                .filter(msg -> msg.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(commandParser::isCommand)
                .subscribe(commandParser::processCommand);
    }

    @PostConstruct
    public void initAdminDm() {
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        log.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");
    }

    public void sendToAdmin(String text) {
        adminDm.createMessage(text).subscribe();
    }

    public void sendToChannel(String channelId, String text) {
        TextChannel channel = (TextChannel) client.getChannelById(Snowflake.of(channelId)).block();
        channel.createMessage(text).subscribe();
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
