package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.ButtonCommand;
import de.neuefische.elotracking.backend.commands.ChallengeAsUserInteraction;
import de.neuefische.elotracking.backend.commands.Createresultchannel;
import de.neuefische.elotracking.backend.commands.SlashCommand;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class CommandParser {

    private final GatewayDiscordClient client;
    private final Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory;
    private final Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory;
    private final Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory;
    private final EloTrackingService service;
    private final DiscordBotService bot;
    private final TimedTaskQueue queue;
    private final long entenwieseId = 612347065762054165L;
    private final Snowflake botSnowflake;

    public CommandParser(GatewayDiscordClient client, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue,
                         Function<ChatInputInteractionEventWrapper, SlashCommand> slashCommandFactory,
                         Function<ButtonInteractionEventWrapper, ButtonCommand> buttonCommandFactory,
                         Function<UserInteractionEventWrapper, ChallengeAsUserInteraction> userInteractionChallengeFactory) {
        this.client = client;
        this.slashCommandFactory = slashCommandFactory;
        this.buttonCommandFactory = buttonCommandFactory;
        this.service = service;
        this.bot = bot;
        this.queue = queue;
        this.botSnowflake = client.getSelfId();
        this.userInteractionChallengeFactory = userInteractionChallengeFactory;

        ApplicationCommandRequest challengeCommandRequest = ApplicationCommandRequest.builder()
                .name("challenge")
                .description("Challenge a player to a match")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("player").description("The player to challenge")
                        .type(ApplicationCommandOption.Type.USER.getValue()).required(true)
                        .build())
                .build();

        ApplicationCommandRequest challengeUserCommandRequest = ApplicationCommandRequest.builder()
                .type(2)
                .name("challenge")
                .build();

        ApplicationCommandRequest setupCommandRequest = ApplicationCommandRequest.builder()
                .name("setup")
                .description("Get started with the bot")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("nameofgame").description("The name of the game you want to track elo rating for")
                        .type(3).required(true).build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("allowdraw").description("Allow draw results and not just win or lose?")
                        .type(5).required(true).build())
                .build();

        if (service.getPropertiesLoader().isDeployGuildCommands()) {
            log.info("Deploying guild commands...");
            ApplicationService applicationService = client.getRestClient().getApplicationService();

            List<ApplicationCommandData> guildApplicationCommands = applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
                    .collectList().block();
            for (ApplicationCommandData guildApplicationCommand : guildApplicationCommands) {
                applicationService.deleteGuildApplicationCommand(
                        botSnowflake.asLong(), entenwieseId,
                        Long.parseLong(guildApplicationCommand.id())).block();
            }

            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, setupCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, challengeCommandRequest).subscribe();
            applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId, challengeUserCommandRequest).subscribe();
        }

        if (service.getPropertiesLoader().isDeployGlobalCommands()) {
            ApplicationService applicationService = client.getRestClient().getApplicationService();

            log.info("Deleting guild commands...");
            List<ApplicationCommandData> guildApplicationCommands = applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
                .collectList().block();
            for (ApplicationCommandData guildApplicationCommand : guildApplicationCommands) {
                applicationService.deleteGuildApplicationCommand(
                        botSnowflake.asLong(), entenwieseId,
                        Long.parseLong(guildApplicationCommand.id())).block();
            }

            log.info("Deploying global commands...");
            List<ApplicationCommandData> globalApplicationCommands = applicationService
                    .getGlobalApplicationCommands(botSnowflake.asLong()).collectList().block();
            for (ApplicationCommandData globalApplicationCommand : globalApplicationCommands) {
                applicationService.deleteGlobalApplicationCommand(
                        botSnowflake.asLong(),
                        Long.parseLong(globalApplicationCommand.id())).block();
            }

            applicationService.createGlobalApplicationCommand(botSnowflake.asLong(), setupCommandRequest).subscribe();
            applicationService.createGlobalApplicationCommand(botSnowflake.asLong(), challengeCommandRequest).subscribe();
            applicationService.createGlobalApplicationCommand(botSnowflake.asLong(), challengeUserCommandRequest).subscribe();
        }

        if (service.getPropertiesLoader().isSetupDevGame()) {
            log.info("Setting up Dev Game...");
            Game game = new Game(entenwieseId, "Dev Game");
            game.setAllowDraw(true);
            game.setDisputeCategoryId(924066405836554251L);
            game.setMatchAutoResolveTime(1);
            game.setOpenChallengeDecayTime(1);
            game.setAcceptedChallengeDecayTime(1);
            game.setMessageCleanupTime(3);

            Guild entenwieseGuild = client.getGuildById(Snowflake.of(entenwieseId)).block();
            List<GuildChannel> channels = entenwieseGuild.getChannels()
                    .filter(channel -> channel.getName().equals("elotracking-results")
                            || channel.getName().equals("elotracking-disputes"))
                    .collectList().block();
            for (GuildChannel channel : channels) {
                channel.delete().block();
            }
            Createresultchannel.staticExecute(service, entenwieseGuild, game);

            entenwieseGuild.getRoles().filter(role -> role.getName().equals("Elo Admin")
                    || role.getName().equals("Elo Moderator"))
                    .subscribe(role -> role.delete().subscribe());
            Role adminRole = entenwieseGuild.createRole(RoleCreateSpec.builder().name("Elo Admin")
                    .permissions(PermissionSet.none()).build()).block();
            game.setAdminRoleId(adminRole.getId().asLong());
            Role modRole = entenwieseGuild.createRole(RoleCreateSpec.builder().name("Elo Moderator")
                    .permissions(PermissionSet.none()).build()).block();
            game.setModRoleId(modRole.getId().asLong());

            long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
            entenwieseGuild.getMemberById(Snowflake.of(ownerId)).block()
                    .asFullMember().block()
                    .addRole(adminRole.getId()).subscribe();

            service.saveGame(game);
        }

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
    }

    public static SlashCommand createSlashCommand(ChatInputInteractionEventWrapper wrapper) {
        String commandClassName = wrapper.event().getCommandName();
        commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
        log.trace("commandString = " + commandClassName);
        try {
            return (SlashCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ChatInputInteractionEvent.class, EloTrackingService.class,
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
            return (ButtonCommand) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
                    .getConstructor(ButtonInteractionEvent.class, EloTrackingService.class, DiscordBotService.class,
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
