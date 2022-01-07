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
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DevTools {

	private EloTrackingService service;
	private GatewayDiscordClient client;
	private final long entenwieseId;
	private final Snowflake botSnowflake;
	private final ApplicationService applicationService;

	public DevTools(EloTrackingService service, GatewayDiscordClient client) {
		this.service = service;
		this.client = client;
		this.entenwieseId = Long.parseLong(service.getPropertiesLoader().getEntenwieseId());
		this.botSnowflake = client.getSelfId();
		this.applicationService = client.getRestClient().getApplicationService();

		ApplicationPropertiesLoader props = service.getPropertiesLoader();
		if (props.isDeployGlobalCommands()) deployGlobalCommands();
		if (props.isDeployGuildCommands()) deployGuildCommands();
		if (props.isDeleteDataOnStartup()) service.deleteAllData();
		if (!props.isUseDevBotToken()) deleteEntenwieseData();
		if (props.isSetupDevGame()) setupDevGame();
		else deploySetupGuildCommandToEntenwiese();
	}

	private void setupDevGame() {
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

		ApplicationCommandPermissionsData modRolePermission = ApplicationCommandPermissionsData.builder()
				.id(modRole.getId().asLong()).type(1).permission(true).build();
		ApplicationCommandPermissionsData adminRolePermission = ApplicationCommandPermissionsData.builder()
				.id(adminRole.getId().asLong()).type(1).permission(true).build();
		ApplicationCommandPermissionsRequest request = ApplicationCommandPermissionsRequest.builder()
				.addPermission(modRolePermission).addPermission(adminRolePermission).build();
		client.getRestClient().getApplicationService().getGuildApplicationCommands(client.getSelfId().asLong(), entenwieseId)
				.filter(applicationCommandData -> applicationCommandData.name().equals("forcewin"))
				.subscribe(applicationCommandData ->
						client.getRestClient().getApplicationService()
								.modifyApplicationCommandPermissions(
										client.getSelfId().asLong(), entenwieseId,
										Long.parseLong(applicationCommandData.id()),
										request)
								.subscribe());

		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
		entenwieseGuild.getMemberById(Snowflake.of(ownerId)).block()
				.asFullMember().block()
				.addRole(adminRole.getId()).subscribe();

		service.saveGame(game);
	}

	private void deploySetupGuildCommandToEntenwiese() {
		client.getRestClient().getApplicationService()
				.createGuildApplicationCommand(
						client.getSelfId().asLong(),
						entenwieseId,
						Setup.getRequest())
				.subscribe();
	}

	private void deleteEntenwieseData() {
		// TODO!
	}

	private void deployGlobalCommands() {
		log.info("Deploying global commands...");
		applicationService.bulkOverwriteGlobalApplicationCommand(
				botSnowflake.asLong(), applicationCommandRequests())
				.subscribe();
	}

	private void deployGuildCommands() {
		log.info("Deploying guild commands...");
		applicationService.bulkOverwriteGuildApplicationCommand(
				botSnowflake.asLong(), entenwieseId, applicationCommandRequests())
				.subscribe();
	}

	private List<ApplicationCommandRequest> applicationCommandRequests() {
		return List.of(
				Challenge.getRequest(),
				ChallengeAsUserInteraction.getRequest(),
				Forcewin.getRequest());
	}
}
