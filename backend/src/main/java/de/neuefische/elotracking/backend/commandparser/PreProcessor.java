package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.event.domain.message.ReactionAddEvent;
import org.springframework.stereotype.Component;

@Component
public class PreProcessor {

	private final EloTrackingService service;

	public PreProcessor(EloTrackingService service) {
		this.service = service;
	}

	public void preProcessReactionEvent(ReactionAddEvent event) {
		// TODO!
	}
}
