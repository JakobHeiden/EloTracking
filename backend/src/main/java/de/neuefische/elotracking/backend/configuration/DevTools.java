package de.neuefische.elotracking.backend.configuration;

import de.neuefische.elotracking.backend.commands.Challenge;
import de.neuefische.elotracking.backend.commands.ChallengeAsUserInteraction;
import de.neuefische.elotracking.backend.commands.Createresultchannel;
import de.neuefische.elotracking.backend.commands.Setup;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
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
		if (props.isSetupDevGame()) setupDevGame();
		if (props.isDeployGlobalCommands()) deployGlobalCommands();
		if (props.isDeployGuildCommands()) deployGuildCommands();
		if (props.isDeleteDataOnStartup()) service.deleteAllData();
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

		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
		entenwieseGuild.getMemberById(Snowflake.of(ownerId)).block()
				.asFullMember().block()
				.addRole(adminRole.getId()).subscribe();

		service.saveGame(game);
	}

	private void deployGlobalCommands() {
		log.info("Deleting guild commands...");
		List<ApplicationCommandData> guildApplicationCommands = applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
				.collectList().block();
		for (ApplicationCommandData guildApplicationCommand : guildApplicationCommands) {
			applicationService.deleteGuildApplicationCommand(
					botSnowflake.asLong(), entenwieseId,
					Long.parseLong(guildApplicationCommand.id())).block();
		}

		log.info("Deploying global commands...");
		// delete all
		List<ApplicationCommandData> globalApplicationCommands = applicationService
				.getGlobalApplicationCommands(botSnowflake.asLong()).collectList().block();
		for (ApplicationCommandData globalApplicationCommand : globalApplicationCommands) {
			applicationService.deleteGlobalApplicationCommand(
					botSnowflake.asLong(),
					Long.parseLong(globalApplicationCommand.id())).block();
		}
		// create anew
		for (ApplicationCommandRequest request : applicationCommandRequests()) {
			applicationService.createGlobalApplicationCommand(botSnowflake.asLong(),  request).subscribe();
		}
	}

	private void deployGuildCommands() {
		log.info("Deploying guild commands...");
		// delete all
		List<ApplicationCommandData> guildApplicationCommands =
				applicationService.getGuildApplicationCommands(botSnowflake.asLong(), entenwieseId)
				.collectList().block();
		for (ApplicationCommandData guildApplicationCommand : guildApplicationCommands) {
			applicationService.deleteGuildApplicationCommand(
					botSnowflake.asLong(), entenwieseId,
					Long.parseLong(guildApplicationCommand.id())).block();
		}
		// create anew
		for (ApplicationCommandRequest request : applicationCommandRequests()) {
			applicationService.createGuildApplicationCommand(botSnowflake.asLong(), entenwieseId,  request).subscribe();
		}
	}

	private List<ApplicationCommandRequest> applicationCommandRequests() {
		return List.of(
				Challenge.getRequest(),
				ChallengeAsUserInteraction.getRequest(),
				Setup.getRequest());
	}
}
