package de.neuefische.elotracking.backend.discord;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tinylog.Logger;

import java.util.function.Consumer;

@Component
public class DiscordBot {
    private final GatewayDiscordClient client;
    private final EloTrackingService service;
    private final PrivateChannel adminDm;
    @Getter
    private final String adminMentionAsString;
    @Getter
    private final String defaultPrefix;

    @Autowired
    public DiscordBot(GatewayDiscordClient gatewayDiscordClient, EloTrackingService eloTrackingService) {
        this.client = gatewayDiscordClient;
        this.service = eloTrackingService;
        this.defaultPrefix = service.getConfig().getProperty("DEFAULT_COMMAND_PREFIX");

        String adminId = service.getConfig().getProperty("ADMIN_DISCORD_ID");
        this.adminMentionAsString = String.format("<@%s>", adminId);
        User admin = client.getUserById(Snowflake.of(adminId)).block();
        this.adminDm = admin.getPrivateChannel().block();
        Logger.info("Private channel to admin established");
        sendToAdmin("I am logged in and ready");

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
                .filter(this::isCommand)
                .subscribe(this::parseCommand);

        client.getEventDispatcher().on(Event.class)
                .subscribe(Logger::trace);
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
        String[] parts = msg.getContent().substring(1).split(" ");
        MessageChannel channel = msg.getChannel().block();
        switch(parts[0]) {
            case "register":
                register(msg, parts, channel);
                break;
            case "challenge":
                challenge(msg, channel);
                break;
            case "accept":
                accept(msg, channel);
                break;
            case "win":
                report(msg, channel, true);
                break;
            case "lose":
                report(msg, channel, false);
                break;
            case "help":
                help(msg, channel);
                break;
            case "setprefix":
                setprefix(msg, channel, parts);
                break;
                default:
                    channel.createMessage("Unknown command " + parts[0]).subscribe();
        }
    }

    private void setprefix(Message msg, MessageChannel channel, String[] parts) {
        if (parts.length == 1 || parts[1].length() > 1) {
            channel.createMessage("Please specify a single special character (or any single character)")
                    .subscribe();
            return;
        }

        String replyFromService = service.setprefix(channel.getId().asString(), parts[1]);
        channel.createMessage(replyFromService).subscribe();
    }

    private void help(Message msg, MessageChannel channel) {
        channel.createMessage(String.format(
                "Commands are:\n" +
                        "%1$sregister\t\tRegister a new game, binding it to this channel\n" +
                        "%1$schallenge\tChallenge another player to a match\n" +
                        "%1$saccept\t\t Accept a challenge\n" +
                        "%1$swin\t\t\t Declare a win over another player\n" +
                        "%1$slose\t\t\tDeclare a loss to another player\n" +
                        "%1$shelp\t\t\t Show this message", msg.getContent().charAt(0)))
                .subscribe();//TODO formatting
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

    private void accept(Message msg, MessageChannel channel) {
        if (msg.getUserMentionIds().size() != 1) {
            channel.createMessage(String.format("You need to tag one and only one Discord user with this command, " +
                    "e.g. %saccept @somebody", msg.getContent().charAt(0))).subscribe();
            return;
        }

        String challengerId = msg.getUserMentionIds().iterator().next().asString();
        String replyFromService = service.accept(
                channel.getId().asString(),
                msg.getAuthor().get().getId().asString(),
                challengerId);
        channel.createMessage(replyFromService).subscribe();
    }

    private void challenge(Message msg, MessageChannel channel) {
        if (msg.getUserMentionIds().size() != 1) {
            channel.createMessage(String.format("You need to tag one and only one Discord user with this command, " +
                    "e.g. %schallenge @somebody", msg.getContent().charAt(0))).subscribe();
            return;
        }

        String otherPlayerId = msg.getUserMentionIds().iterator().next().asString();
        String replyFromService = service.challenge(
                channel.getId().asString(),
                msg.getAuthor().get().getId().asString(),
                otherPlayerId);
        channel.createMessage(replyFromService).subscribe();
    }

    private void register(Message msg, String[] parts, MessageChannel channel) {
        if (parts.length < 2) {
            channel.createMessage(String.format("Usage: %sregister <name of your game>",
                    msg.getContent().charAt(0)))
                    .subscribe();
            return;
        }

        String name = msg.getContent().substring("register".length() + 1);
        String replyFromService = service.register(channel.getId().asString(), name);
        channel.createMessage(replyFromService).subscribe();

        Consumer<TextChannelEditSpec> edit =
                textChannelEditSpec -> textChannelEditSpec.setTopic(
                        String.format("Leaderboard: http://%s/%s",
                                service.getConfig().getProperty("BASE_URL"),
                                channel.getId().asString()));
        ((TextChannel) channel).edit(edit).subscribe(System.out::println);
    }

    public String getPlayerName(String playerId) {
        return client.getUserById(Snowflake.of(playerId)).block().getTag();
    }
}
