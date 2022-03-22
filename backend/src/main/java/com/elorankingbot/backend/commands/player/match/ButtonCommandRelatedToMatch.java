package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

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
		System.out.println(event.getCustomId());
		this.match = dbservice.getMatch(UUID.fromString(event.getCustomId().split(":")[1]));
		this.queue = match.getQueue();
		this.game = queue.getGame();
		this.server = game.getServer();
		this.guildId = server.getGuildId();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
		this.activePlayerId = Player.generateId(guildId, activeUserId);
	}

	protected Mono<Message> getActiveMessage() {
		return bot.getMessageById(match.getMessageId(activePlayerId), match.getPrivateChannelId(activePlayerId));
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