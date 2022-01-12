package com.elorankingbot.backend.configuration;

import com.elorankingbot.backend.commands.Createresultchannel;
import com.elorankingbot.backend.commands.Forcedraw;
import com.elorankingbot.backend.commands.Forcewin;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.PermissionSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DevTools {

	private final EloRankingService service;
	private final DiscordBotService bot;
	private final GatewayDiscordClient client;
	private final ApplicationService applicationService;

	private long entenwieseId;
	private Guild entenwieseGuild;
	private long botId;
	private Game game;
	private Role modRole;
	private Role adminRole;

	public DevTools(EloRankingService service, DiscordBotService bot, GatewayDiscordClient client) {
		this.service = service;
		this.bot = bot;
		this.client = client;
		this.botId = client.getSelfId().asLong();
		entenwieseId = Long.parseLong(service.getPropertiesLoader().getEntenwieseId());
		//this.entenwieseGuild = client.getGuildById(Snowflake.of(entenwieseId)).block();
		this.applicationService = client.getRestClient().getApplicationService();

		ApplicationPropertiesLoader props = service.getPropertiesLoader();
		if (props.isDeleteDataOnStartup()) {
			service.deleteAllData();
		}
		if (props.isSetupDevGame()) setupDevGame();
		if (props.isDoUpdateGuildCommands()) updateGuildCommands();
	}

	private void updateGuildCommands() {
		log.warn("updating guild commands...");
		service.findAllGames().stream().forEach(
				game -> {
					try {
						Guild guild = client.getGuildById(Snowflake.of(game.getGuildId())).block();
						Role currentAdminRole = guild.getRoleById(Snowflake.of(game.getAdminRoleId())).block();
						Role currentModRole = guild.getRoleById(Snowflake.of(game.getModRoleId())).block();
						bot.deployCommandToGuild(Forcewin.getRequest(), guild, currentAdminRole, currentModRole);
						if (game.isAllowDraw())	bot.deployCommandToGuild(Forcedraw.getRequest(), guild, currentAdminRole, currentModRole);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
		);
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

	private void makeOwnerAdmin() {
		long ownerId = Long.valueOf(service.getPropertiesLoader().getOwnerId());
		entenwieseGuild.getMemberById(Snowflake.of(ownerId)).block()
				.asFullMember().block()
				.addRole(adminRole.getId()).subscribe();
	}
}
