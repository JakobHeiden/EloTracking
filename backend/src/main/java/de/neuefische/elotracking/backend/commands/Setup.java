package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.rest.util.PermissionSet;

public class Setup extends ApplicationCommandInteractionCommand {

	public Setup(ApplicationCommandInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (game != null) {
			event.reply("Setup was already performed before.").subscribe();
			return;
		}

		ChatInputInteractionEvent event = (ChatInputInteractionEvent) super.event;
		Guild guild = event.getInteraction().getGuild().block();

		Game game = new Game(guild.getId().asLong(),
				event.getOption("name").get().getValue().get().asString());

		Role adminRole = guild.createRole(RoleCreateSpec.builder().name("Elotracking Admin")
				.permissions(PermissionSet.none()).build()).block();
		game.setAdminRoleId(adminRole.getId().asLong());
		Role modRole = guild.createRole(RoleCreateSpec.builder().name("Elotracking Moderator")
				.permissions(PermissionSet.none()).build()).block();
		game.setModRoleId(modRole.getId().asLong());

		Createresultchannel.staticExecute(service, guild, game);

		game.setAllowDraw(event.getOption("allowdraw").get().getValue().get().asBoolean());

		service.saveGame(game);

		event.reply("Setup performed. Here is a link to the leaderboard: "// TODO! mehr infoes, rolles etc
				+ String.format("http://%s/%s", service.getPropertiesLoader().getBaseUrl(), guildId)).subscribe();
	}
}
