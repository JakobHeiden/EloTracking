package com.elorankingbot.backend.command;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.commands.ChallengeAsUserInteraction;
import com.elorankingbot.backend.commands.Setup;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class EventParser {

    private final GatewayDiscordClient client;
    private final Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory;
    private final Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory;
    private final Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory;
    private final EloRankingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;

    public EventParser(GatewayDiscordClient client, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
                       Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory,
                       Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory,
                       Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory) {
        this.client = client;
        this.slashCommandFactory = slashCommandFactory;
        this.buttonCommandFactory = buttonCommandFactory;
        this.service = service;
        this.bot = bot;
        this.queue = queue;
        this.userInteractionChallengeFactory = userInteractionChallengeFactory;

        client.on(ChatInputInteractionEvent.class)
                .map(event -> new ChatInputInteractionEventWrapper(event, service, bot, queue, client))
                .map(slashCommandFactory::apply)
                .subscribe(SlashCommand::execute);

        client.on(ButtonInteractionEvent.class)
                .map(event -> new ButtonInteractionEventWrapper(event, service, bot, queue, client))
                .map(buttonCommandFactory::apply)
                .subscribe(ButtonCommand::execute);

        client.on(UserInteractionEvent.class)
                .map(event -> new UserInteractionEventWrapper(event, service, bot, queue, client))
                .map(userInteractionChallengeFactory::apply)
                .subscribe(ChallengeAsUserInteraction::execute);

        client.on(GuildCreateEvent.class)
                .filter(event -> service.findGameByGuildId(event.getGuild().getId().asLong()).isEmpty())
                .subscribe(event ->
                    client.getRestClient().getApplicationService()
                            .createGuildApplicationCommand(
                                    client.getSelfId().asLong(),
                                    event.getGuild().getId().asLong(),
                                    Setup.getRequest()).subscribe());
    }

    public static SlashCommand createSlashCommand(ChatInputInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCommandName();
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (SlashCommand) Class.forName("com.elorankingbot.backend.commands." + commandClassName)
                    .getConstructor(ChatInputInteractionEvent.class, EloRankingService.class,
                            DiscordBotService.class, TimedTaskQueue.class, GatewayDiscordClient.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
        } catch (Exception e) {
            wrapper.bot().sendToOwner(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static ButtonCommand createButtonCommand(ButtonInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCustomId().split(":")[0];
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (ButtonCommand) Class.forName("com.elorankingbot.backend.commands." + commandClassName)
                    .getConstructor(ButtonInteractionEvent.class, EloRankingService.class, DiscordBotService.class,
                            TimedTaskQueue.class, GatewayDiscordClient.class)
                    .newInstance(wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
        } catch (Exception e) {
            wrapper.bot().sendToOwner(e.toString());
            e.printStackTrace();
            return null;
        }
    }

    public static ChallengeAsUserInteraction createUserInteractionChallenge(UserInteractionEventWrapper wrapper) {
        return new ChallengeAsUserInteraction(
                wrapper.event(), wrapper.service(), wrapper.bot(), wrapper.queue(), wrapper.client());
    }
}
