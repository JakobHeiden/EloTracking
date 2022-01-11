package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;

public class Createresultchannel extends SlashCommand {// TODO wird so nicht mehr genutzt

	public Createresultchannel(ChatInputInteractionEvent event, EloRankingService service,
							   DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!super.canExecute()) return;

		staticExecute(service, event.getInteraction().getGuild().block(), game);
		service.saveGame(game);

		event.reply("The channel was created.").subscribe();
	}

	public static void staticExecute(EloRankingService service, Guild guild, Game game) {
		TextChannel resultChannel = guild.createTextChannel("Elotracking results")
				.withTopic(String.format("All resolved matches will be logged here. Leaderboard: http://%s/%s",
						service.getPropertiesLoader().getBaseUrl(), guild.getId().asString())).block();
		game.setResultChannelId(resultChannel.getId().asLong());
	}
}
