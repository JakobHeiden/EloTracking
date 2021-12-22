package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class Createresultchannel extends ApplicationCommandInteractionCommand {

	public Createresultchannel(ApplicationCommandInteractionEvent event, EloTrackingService service,
							   DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsGame = true;
	}

	public void execute() {
		if (!super.canExecute()) return;

		staticExecute(service, event.getInteraction().getGuild().block(), game);
		service.saveGame(game);

		event.reply("The channel was created.").subscribe();
	}

	public static void staticExecute(EloTrackingService service, Guild guild, Game game) {
		TextChannel resultChannel = guild.createTextChannel("Elotracking results")
				.withTopic(String.format("All resolved matches will be logged here. Leaderboard: http://%s/%s",
						service.getPropertiesLoader().getBaseUrl(), guild.getId().asString())).block();
		game.setResultChannelId(resultChannel.getId().asLong());
	}
}
