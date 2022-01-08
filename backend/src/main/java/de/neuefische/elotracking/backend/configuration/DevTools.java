package de.neuefische.elotracking.backend.configuration;

import de.neuefische.elotracking.backend.commands.*;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.neuefische.elotracking.backend.command.DiscordCommandManager.commandsThatNeedAdminRole;
import static de.neuefische.elotracking.backend.command.DiscordCommandManager.commandsThatNeedModRole;

@Component
@Slf4j
public class DevTools {

	private final EloTrackingService service;
	private final GatewayDiscordClient client;

	private long entenwieseId;
	private Guild entenwieseGuild;
	private long botId;
	private Game game;
	private Role modRole;
	private Role adminRole;

	public DevTools(EloTrackingService service, GatewayDiscordClient client) {
		this.service = service;
		this.client = client;
		this.botId = client.getSelfId().asLong();
		entenwieseId = Long.parseLong(service.getPropertiesLoader().getEntenwieseId());
		this.entenwieseGuild = client.getGuildById(Snowflake.of(entenwieseId)).block();

		ApplicationPropertiesLoader props = service.getPropertiesLoader();
		if (props.isDeleteDataOnStartup()) {
			service.deleteAllData();
			deleteAllGuildCommandsForEntenwiese();
			Setup.deployToGuild(client, entenwieseGuild);
		}
		if (props.isSetupDevGame()) setupDevGame();

		client.getGuilds().subscribe(guild -> {
			Forcedraw.deployToGuild(client, guild);
			Forcewin.deployToGuild(client, guild);
			Challenge.deployToGuild(client, guild);
			ChallengeAsUserInteraction.deployToGuild(client, guild);
		});
	}


	private void deleteAllGuildCommandsForEntenwiese() {
		ApplicationService applicationService = client.getRestClient().getApplicationService();
		applicationService.getGuildApplicationCommands(client.getSelfId().asLong(), entenwieseId)
				.subscribe(applicationCommandData -> applicationService.
						deleteGuildApplicationCommand(client.getSelfId().asLong(), entenwieseId,
								Long.parseLong(applicationCommandData.id())).subscribe());
	}

	private void setupDevGame() {
		log.info("Setting up Dev Game...");
		game = new Game(entenwieseId, "Dev Game");
		game.setAllowDraw(true);
		game.setDisputeCategoryId(924062376871989248L);
		game.setMatchAutoResolveTime(1);
		game.setOpenChallengeDecayTime(1);
		game.setAcceptedChallengeDecayTime(1);
		game.setMessageCleanupTime(3);

		deleteOldEloChannelsAndCategories();
		Createresultchannel.staticExecute(service, entenwieseGuild, game);
		deleteOldRoles();
		setDiscordCommandPermissions();
		makeOwnerAdmin();

		service.saveGame(game);
	}

	private void deleteOldEloChannelsAndCategories() {
		List<GuildChannel> channels = entenwieseGuild.getChannels()
				.filter(channel -> channel.getName().equals("elotracking-results")
						|| channel.getName().equals("elo disputes"))
				.collectList().block();
		for (GuildChannel channel : channels) {
			channel.delete().block();
		}
	}

	private void deleteOldRoles() {
		entenwieseGuild.getRoles().filter(role -> role.getName().equals("Elo Admin")
						|| role.getName().equals("Elo Moderator"))
				.subscribe(role -> role.delete().subscribe());
		adminRole = entenwieseGuild.createRole(RoleCreateSpec.builder().name("Elo Admin")
				.permissions(PermissionSet.none()).build()).block();
		game.setAdminRoleId(adminRole.getId().asLong());
		modRole = entenwieseGuild.createRole(RoleCreateSpec.builder().name("Elo Moderator")
				.permissions(PermissionSet.none()).build()).block();
		game.setModRoleId(modRole.getId().asLong());
	}

	private void setDiscordCommandPermissions() {
		ApplicationCommandPermissionsData modRolePermission = ApplicationCommandPermissionsData.builder()
				.id(modRole.getId().asLong()).type(1).permission(true).build();
		ApplicationCommandPermissionsData adminRolePermission = ApplicationCommandPermissionsData.builder()
				.id(adminRole.getId().asLong()).type(1).permission(true).build();
		// supply permission for admin commands to just admins
		ApplicationCommandPermissionsRequest onlyAdminPermissionRequest = ApplicationCommandPermissionsRequest.builder()
				.addPermission(adminRolePermission).build();
		client.getRestClient().getApplicationService().getGuildApplicationCommands(botId, entenwieseId)
				.filter(applicationCommandData ->
						Arrays.asList(commandsThatNeedAdminRole).contains(applicationCommandData.name()))
				.subscribe(applicationCommandData ->
						client.getRestClient().getApplicationService()
								.modifyApplicationCommandPermissions(
										botId, entenwieseId,
										Long.parseLong(applicationCommandData.id()),
										onlyAdminPermissionRequest)
								.subscribe());
		// supply permission for mod commands to both mods and admins
		ApplicationCommandPermissionsRequest adminAndModPermissionRequest = ApplicationCommandPermissionsRequest.builder()
				.addPermission(modRolePermission).addPermission(adminRolePermission).build();
		client.getRestClient().getApplicationService().getGuildApplicationCommands(botId, entenwieseId)
				.filter(applicationCommandData ->
						Arrays.asList(commandsThatNeedModRole).contains(applicationCommandData.name()))
				.subscribe(applicationCommandData ->
						client.getRestClient().getApplicationService()
								.modifyApplicationCommandPermissions(
										botId, entenwieseId,
										Long.parseLong(applicationCommandData.id()),
										adminAndModPermissionRequest)
								.subscribe());
	}

	private void makeOwnerAdmin() {
		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
		entenwieseGuild.getMemberById(Snowflake.of(ownerId)).block()
				.asFullMember().block()
				.addRole(adminRole.getId()).subscribe();
	}

	private void deploySetupGuildCommandToEntenwiese() {
		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(
						botId,
						entenwieseId,
						Setup.getRequest())
				.subscribe();
	}
}
