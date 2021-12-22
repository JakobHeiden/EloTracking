package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Createdisputechannel extends ApplicationCommandInteractionCommand {

	public Createdisputechannel(ApplicationCommandInteractionEvent event, EloTrackingService service,
								DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsGame = true;
	}

	public void execute() {
		if (!super.canExecute()) return;

		staticExecute(event.getInteraction().getGuild().block(), game);
		service.saveGame(game);

		event.reply("The channel was created. It is only visible to Elotracking Admin and Elotracking Moderator roles.").subscribe();
	}

	public static void staticExecute(Guild guild, Game game) {
		TextChannel resultChannel = guild.createTextChannel("Elotracking disputes")
				.withPermissionOverwrites(
						PermissionOverwrite.forRole(
								guild.getEveryoneRole().block().getId(),
								PermissionSet.none(),
								PermissionSet.of(Permission.VIEW_CHANNEL)),
						PermissionOverwrite.forRole(
								Snowflake.of(game.getAdminRoleId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()),
						PermissionOverwrite.forRole(
								Snowflake.of(game.getModRoleId()),
								PermissionSet.of(Permission.VIEW_CHANNEL),
								PermissionSet.none()))
				.withTopic("Disputes from conflicting match reports will be logged here")
				.block();
		game.setResultChannelId(resultChannel.getId().asLong());
	}
}
