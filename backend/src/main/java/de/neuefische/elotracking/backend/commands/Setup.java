package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.DiscordCommandManager;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.Arrays;

public class Setup extends SlashCommand {

	private Guild guild;
	private long botId;
	private Role modRole;
	private Role adminRole;
	private ApplicationService applicationService;

	public Setup(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.applicationService = client.getRestClient().getApplicationService();
		this.guild = event.getInteraction().getGuild().block();
		this.botId = client.getSelfId().asLong();
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("setup")
				.description("Get started with the bot")
				.addOption(ApplicationCommandOptionData.builder()
						.name("nameofgame").description("The name of the game you want to track elo rating for")
						.type(3).required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("allowdraw").description("Allow draw results and not just win or lose?")
						.type(5).required(true).build())
				.build();
	}

	public static void deployToGuild(GatewayDiscordClient client, Guild guild) {
		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(client.getSelfId().asLong(), guild.getId().asLong(), getRequest())
				.subscribe();
	}

	public void execute() {
		game = new Game(guild.getId().asLong(),
				event.getOption("nameofgame").get().getValue().get().asString());
		createModAndAdminRoles();
		setPermissionsForCommands();
		createResultChannel();
		createDisputeCategory();
		game.setAllowDraw(event.getOption("allowdraw").get().getValue().get().asBoolean());
		service.saveGame(game);

		event.reply("Setup performed. Here is what I did:\n" +
						"- I created the role Elo Moderator. Elo Moderator has permissions related to " +
						"dispute resolution and will get notified when disputes happen\n" +
						"- I created the role Elo Admin. Elo Admin has all the permissions of Elo Moderator, " +
						"and can also modify my settings\n" +
						"- I made you an Elo Admin\n" +
						"- I created a channel where I will post all match results\n" +
						"- I created a channel category ELO DISPUTES only visible to Elo Moderator\n" +
						"- I created a web page with rankings: " +
						String.format("http://%s/%s\n", service.getPropertiesLoader().getBaseUrl(), guildId) +
						"- I created two challenge commands, one for chat and " +
						"one user command (right click on a user -> apps -> challenge)\n" +
						"- I created the forcewin command, only available to Elo Moderator" +
						(game.isAllowDraw() ? "\n- I created the the forcedraw command, only available to Elo Moderator" : ""))
				.subscribe();

		deleteSetupGuildCommand();
		if (game.isAllowDraw()) Forcedraw.deployToGuild(client, guild);
		Forcewin.deployToGuild(client, guild);
		Challenge.deployToGuild(client, guild);
		ChallengeAsUserInteraction.deployToGuild(client, guild);

		bot.sendToOwner(String.format("Setup performed on guild %s:%s with %s members",
				guild.getId(), guild.getName(), guild.getMemberCount()));
	}

	private void createModAndAdminRoles() {
		adminRole = guild.createRole(RoleCreateSpec.builder().name("Elo Admin")
				.permissions(PermissionSet.none())
				.mentionable(true)
				.build()).block();
		game.setAdminRoleId(adminRole.getId().asLong());
		modRole = guild.createRole(RoleCreateSpec.builder().name("Elo Moderator")
				.permissions(PermissionSet.none())
				.build()).block();
		game.setModRoleId(modRole.getId().asLong());
		event.getInteraction().getMember().get().addRole(adminRole.getId()).subscribe();
	}

	private void setPermissionsForCommands() {// TODO vllt nach DiscordCommandManager umziehen?
		ApplicationCommandPermissionsData modRolePermission = ApplicationCommandPermissionsData.builder()
				.id(modRole.getId().asLong()).type(1).permission(true).build();
		ApplicationCommandPermissionsData adminRolePermission = ApplicationCommandPermissionsData.builder()
				.id(adminRole.getId().asLong()).type(1).permission(true).build();
		// supply permission for admin commands to just admins
		ApplicationCommandPermissionsRequest onlyAdminPermissionRequest = ApplicationCommandPermissionsRequest.builder()
				.addPermission(adminRolePermission).build();
		client.getRestClient().getApplicationService().getGuildApplicationCommands(client.getSelfId().asLong(), guildId)
				.filter(applicationCommandData ->
						Arrays.asList(DiscordCommandManager.commandsThatNeedAdminRole).contains(applicationCommandData.name()))
				.subscribe(applicationCommandData ->
						client.getRestClient().getApplicationService()
								.modifyApplicationCommandPermissions(
										client.getSelfId().asLong(), guildId,
										Long.parseLong(applicationCommandData.id()),
										onlyAdminPermissionRequest)
								.subscribe());
		// supply permission for mod commands to both mods and admins
		ApplicationCommandPermissionsRequest adminAndModPermissionRequest = ApplicationCommandPermissionsRequest.builder()
				.addPermission(modRolePermission).addPermission(adminRolePermission).build();
		client.getRestClient().getApplicationService().getGuildApplicationCommands(client.getSelfId().asLong(), guildId)
				.filter(applicationCommandData ->
						Arrays.asList(DiscordCommandManager.commandsThatNeedModRole).contains(applicationCommandData.name()))
				.subscribe(applicationCommandData ->
						client.getRestClient().getApplicationService()
								.modifyApplicationCommandPermissions(
										client.getSelfId().asLong(), guildId,
										Long.parseLong(applicationCommandData.id()),
										adminAndModPermissionRequest)
								.subscribe());
	}

	private void createResultChannel() {
		TextChannel resultChannel = guild.createTextChannel("Elo Ranking results")
				.withTopic(String.format("All resolved matches will be logged here. Leaderboard: http://%s/%s",
						service.getPropertiesLoader().getBaseUrl(), guild.getId().asString())).block();
		game.setResultChannelId(resultChannel.getId().asLong());
	}

	private void createDisputeCategory() {
		Category disputeCategory = guild.createCategory("elo disputes").withPermissionOverwrites(
				PermissionOverwrite.forRole(guild.getId(), PermissionSet.none(),
						PermissionSet.of(Permission.VIEW_CHANNEL)),
				PermissionOverwrite.forRole(adminRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none()),
				PermissionOverwrite.forRole(modRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none())).block();
		game.setDisputeCategoryId(disputeCategory.getId().asLong());
	}

	private void deleteSetupGuildCommand() {
		applicationService.getGuildApplicationCommands(botId, guildId)
				.filter(applicationCommandData -> applicationCommandData.name().equals("setup"))
				.map(applicationCommandData ->
						applicationService.deleteGuildApplicationCommand(
										botId, guildId, Long.parseLong(applicationCommandData.id()))
								.subscribe()).subscribe();
	}

	private void deployForcedrawGuildCommand() {
		applicationService.createGuildApplicationCommand(botId, guildId, Forcedraw.getRequest()).subscribe();
	}
}
