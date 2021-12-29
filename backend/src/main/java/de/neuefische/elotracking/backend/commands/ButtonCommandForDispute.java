package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;

public abstract class ButtonCommandForDispute extends ButtonCommand {

	protected ChallengeModel challenge;
	protected Game game;
	protected TextChannel disputeChannel;

	protected ButtonCommandForDispute(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);

		this.challenge = service.getChallengeByChallengerMessageId(Long.parseLong(
				event.getCustomId().split(":")[1])).get();
		this.game = service.findGameByGuildId(event.getInteraction().getGuildId().get().asLong()).get();
		this.disputeChannel = (TextChannel) event.getInteraction().getChannel().block();
	}
}
