package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.UserInteractionEvent;

import java.util.Optional;

public class ChallengeAsUserInteraction {

	private final UserInteractionEvent event;
	private final EloTrackingService service;
	private final DiscordBotService bot;
	private final TimedTaskQueue queue;
	private final GatewayDiscordClient client;

	public ChallengeAsUserInteraction(UserInteractionEvent event, EloTrackingService service, DiscordBotService bot,
									  TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
	}

	public void execute() {
		Optional<Game> maybeGame = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong());
		if (maybeGame.isEmpty()) {
			event.reply("Please run /setup first.").withEphemeral(true).subscribe();
			return;
		}
		if (event.getTargetUser().block().isBot()) {
			event.reply("You cannot challenge a bot.").withEphemeral(true).subscribe();
			return;
		}

		long acceptorId = event.getTargetId().asLong();
		long guildId = event.getInteraction().getGuildId().get().asLong();
		Game game = maybeGame.get();
		Challenge.staticExecute(
				acceptorId, guildId, game,
				event, service, bot, queue);
	}
}
