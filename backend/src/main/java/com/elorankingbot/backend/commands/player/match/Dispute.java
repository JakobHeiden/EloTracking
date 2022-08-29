package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Dispute extends ButtonCommandRelatedToMatch {

	private TextChannel matchChannel;
	private TextChannel disputeChannel;

	public Dispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		if (!activeUserIsInvolvedInMatch() || match.isDispute()) {
			event.acknowledge().subscribe();
			return;
		}

		event.getInteraction().getMessage().get().edit().withComponents(none).subscribe();
		match.setDispute(true);
		dbService.saveMatch(match);
		matchChannel = (TextChannel) event.getInteraction().getChannel().block();
		channelManager.moveToDisputes(server, matchChannel);
		channelManager.createDisputeMessage(matchChannel, match, activeUser.getTag());
		event.acknowledge().subscribe();
	}
}
