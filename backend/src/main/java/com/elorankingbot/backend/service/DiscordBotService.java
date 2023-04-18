package com.elorankingbot.backend.service;

import com.elorankingbot.backend.logging.ExceptionHandler;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.model.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.service.ApplicationService;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@CommonsLog
@Service
public class DiscordBotService {

    private final GatewayDiscordClient client;
    private final DBService dbService;
    private final ApplicationService applicationService;
    private final ApplicationPropertiesLoader props;
    private PrivateChannel ownerPrivateChannel;
    @Getter
    private final long botId;

    private SimpleDateFormat sendToOwnerTimeStamp = new SimpleDateFormat("HH:mm:ss");

    public DiscordBotService(Services services) {
        this.client = services.client;
        this.dbService = services.dbService;
        this.botId = client.getSelfId().asLong();
        this.props = services.props;
        this.applicationService = client.getRestClient().getApplicationService();
    }

    @PostConstruct
    public void initAdminDm() {
        long ownerId = Long.valueOf(props.getOwnerId());
        User owner = client.getUserById(Snowflake.of(ownerId)).block();
        this.ownerPrivateChannel = owner.getPrivateChannel().block();
        sendToOwner("I am logged in and ready");
    }

    public Snowflake getSnowflake() {
        return Snowflake.of(botId);
    }

    // Logging
    public void sendToOwner(String text) {
        if (text == null) text = "null";
        if (text.equals("")) text = "empty String";
        final String finalText = text;
        ownerPrivateChannel.createMessage(sendToOwnerTimeStamp.format(new Date()) + ": " + text)
                .subscribe(ExceptionHandler.NO_OP, throwable -> {
                    log.error("Error in sendToOwner: " + finalText);
                    log.error(throwable.getMessage());
                });
    }

    // Guild
    public Mono<Guild> getGuild(long guildId) {
        return client.getGuildById(Snowflake.of(guildId));
    }

    public Mono<Guild> getGuild(Server server) {
        return getGuild(server.getGuildId());
    }

    public List<Long> getAllGuildIds() {
        return client.getGuilds()
                .map(guild -> guild.getId().asLong())
                .collectList().block();
    }

    public String getServerName(Server server) {
        try {
            return String.format("%s:%s",
                    server.getGuildId(),
                    client.getGuildById(Snowflake.of(server.getGuildId())).block().getName());
        } catch (ClientException e) {
            return server.getGuildId() + ":unknown, can't get Guild";
        }
    }

    public String getServerName(Player player) {
        return getServerName(dbService.getOrCreateServer(player.getGuildId()));
    }

    // User
    public User getUser(long userId) {
        return client.getUserById(Snowflake.of(userId)).block();
    }

    public Member getMember(Player player) {
        return client.getMemberById(Snowflake.of(player.getGuildId()), Snowflake.of(player.getUserId())).block();
    }

    // Roles
    public Role getRole(Server server, long roleId) {
        return client.getRoleById(Snowflake.of(server.getGuildId()), Snowflake.of(roleId)).block();
    }

    public boolean isBotRoleHigherThanGivenRole(Role givenRole) {
        for (Role botRole : client.getSelfMember(givenRole.getGuildId()).block().getRoles().collectList().block()) {
            if (botRole.getRawPosition() > givenRole.getRawPosition()) return true;
        }
        return false;
    }

    public Role getBotIntegrationRole(Server server) {
        return getBotIntegrationRole(server.getGuildId());
    }

    public Role getBotIntegrationRole(long guildId) {
        return client.getSelfMember(Snowflake.of(guildId)).block().getRoles()
                .filter(Role::isManaged).blockFirst();
    }

    public void removeAllRanks(Game game) {
        Collection<Long> allRankIds = game.getRequiredRatingToRankId().values();
        for (Player player : dbService.findAllPlayersForServer(game.getServer())) {
            client.getMemberById(Snowflake.of(player.getGuildId()), Snowflake.of(player.getUserId()))
                    .onErrorContinue((throwable, o) -> {
                    })// TODO wahrscheinlich den player loeschen
                    .subscribe(member -> member.getRoleIds().stream()
                            .filter(snowflake -> allRankIds.contains(snowflake.asLong()))
                            .forEach(snowflake -> member.removeRole(snowflake).subscribe()));
        }
    }

    // Messages
    public Mono<Message> getMessage(long messageId, long channelId) {
        return client.getMessageById(Snowflake.of(channelId), Snowflake.of(messageId));
    }

    public void sendDM(User user, ChatInputInteractionEvent event, String content) {
        user.getPrivateChannel()
                .flatMap(privateChannel -> privateChannel.createMessage(content))
                .subscribe(messageIgnored -> {
                        },
                        throwable -> event.createFollowup(dmFailedFollowupMessage(user.getTag())).subscribe());
    }

    public void sendDM(User user, ChatInputInteractionEvent event, EmbedCreateSpec content) {
        user.getPrivateChannel()
                .flatMap(privateChannel -> privateChannel.createMessage(content))
                .subscribe(messageIgnored -> {
                        },
                        throwable -> event.createFollowup(dmFailedFollowupMessage(user.getTag())).subscribe());
    }

    private String dmFailedFollowupMessage(String tag) {
        return String.format("I was not able to inform %s. Most likely their DMs are closed.", tag);
    }

    public void sendDM(User user, String content) {
        user.getPrivateChannel()
                .flatMap(privateChannel -> privateChannel.createMessage(content))
                .subscribe(messageIgnored -> {
                }, throwableIgnored -> {
                });
    }

    // Channels
    public Mono<Channel> getChannelById(long channelId) {
        // TODO this is a bandaid since the store will sometimes return pretend channels that have been deleted. remove when no longer needed
        return client.withRetrievalStrategy(EntityRetrievalStrategy.REST).getChannelById(Snowflake.of(channelId));
        //return client.getChannelById(Snowflake.of(channelId));
    }

    public void deleteChannel(long channelId) {
        if (channelId == 0L) {
            log.warn("deleteChannel called with id 0");
            return;
        }
        client.getChannelById(Snowflake.of(channelId))
                .onErrorContinue(((throwable, o) -> log.info(String.format("Channel %s is already deleted.", channelId))))
                .subscribe(channel -> channel.delete().subscribe());
    }

    public String guildAsString(Guild guild) {
        return guild.getId().asString() + ":" + guild.getName();
    }

    public String serverAsString(Server server) {
        try {
            return guildAsString(getGuild(server).block());
        } catch (Exception e) {
            return "error in bot::serverAsString";
        }
    }
}

