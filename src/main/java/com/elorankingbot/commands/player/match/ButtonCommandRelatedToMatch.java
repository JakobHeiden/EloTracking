package com.elorankingbot.commands.player.match;

import com.elorankingbot.commands.ButtonCommand;
import com.elorankingbot.model.*;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.util.UUID;

public abstract class ButtonCommandRelatedToMatch extends ButtonCommand {

	protected final Server server;
	protected final long guildId;
	protected final Game game;
	protected final MatchFinderQueue queue;
	protected final Match match;
	protected final User activeUser;
	protected final long activeUserId;
	protected final UUID activePlayerId;

	protected ButtonCommandRelatedToMatch(ButtonInteractionEvent event, Services services) {
		super(event, services);
		this.match = dbService.getMatch(UUID.fromString(event.getCustomId().split(":")[1]));
		this.queue = match.getQueue();
		this.game = queue.getGame();
		this.server = game.getServer();
		this.guildId = server.getGuildId();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
		this.activePlayerId = Player.generateId(guildId, activeUserId);
	}

	protected boolean activeUserIsInvolvedInMatch() {
		return match.getPlayers().stream()
				.anyMatch(player -> player.getUserId() == activeUserId);
	}

	protected void updateAndSaveChallenge(Message message) {// TODO vllt in interface, default method refaktorn
		//if (isChallengerCommand) updateAcceptorMessageIdAndSaveChallenge(message);

		//else updateChallengerMessageIdAndSaveChallenge(message);
	}

	protected void updateChallengerMessageIdAndSaveChallenge(Message message) {
		//challenge.setChallengerMessageId(message.getId().asLong());
		//service.saveChallenge(challenge);
	}

	protected void updateAcceptorMessageIdAndSaveChallenge(Message message) {
		//challenge.setAcceptorMessageId(message.getId().asLong());
		//service.saveChallenge(challenge);
	}
}
