package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class PreProcessor {

	private final EloTrackingService service;
	private final ReactionEmoji checkMark;
	private final ReactionEmoji crossMark;

	public PreProcessor(EloTrackingService service, @Lazy DiscordBotService bot) {
		this.service = service;
		this.checkMark = bot.checkMark;
		this.crossMark = bot.crossMark;
	}

	public boolean isCommandByReaction(ReactionAddEvent event) {
		if (!event.getEmoji().equals(checkMark) && !event.getEmoji().equals(crossMark)) return false;

		return service.challengeExistsByAcceptorMessageId(event.getMessageId().asLong());
	}
}