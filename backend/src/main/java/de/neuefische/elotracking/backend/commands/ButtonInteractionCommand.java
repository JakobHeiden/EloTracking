package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.Optional;

public abstract class ButtonInteractionCommand {

	protected final EloTrackingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final ButtonInteractionEvent event;
	protected final Message reporterMessage;
	protected final long guildId;
	protected final Game game;
	protected final ChallengeModel challenge;
	protected final boolean isChallengerCommand;

	public ButtonInteractionCommand(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.reporterMessage = event.getMessage().get();
		Optional<ChallengeModel> maybeChallengeByChallengerMessageId = service.getChallengeByChallengerMessageId(event.getMessageId().asLong());
		if (maybeChallengeByChallengerMessageId.isPresent()) {
			this.challenge = maybeChallengeByChallengerMessageId.get();
			this.guildId = challenge.getGuildId();
			this.isChallengerCommand = true;
		} else {
			this.challenge = service.getChallengeByAcceptorMessageId(event.getMessageId().asLong()).get();
			this.guildId = challenge.getGuildId();
			this.isChallengerCommand = false;
		}
		this.game = service.findGameByGuildId(guildId).get();
	}

	public abstract void execute();

	protected void removeSelfReactions(Message message, ReactionEmoji... emojis) {
		for (ReactionEmoji emoji : emojis) {
			message.removeSelfReaction(emoji).subscribe();
		}
	}

}
