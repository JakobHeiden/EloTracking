package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.UUID;

public abstract class ButtonCommandRelatedToDispute extends ButtonCommand {

	protected String moderatorTag;
	protected final Match match;
	protected final Server server;

	protected ButtonCommandRelatedToDispute(ButtonInteractionEvent event, Services services) {
		super(event, services);
		UUID matchId = UUID.fromString(event.getCustomId().split(":")[1]);
		this.match = dbService.getMatch(matchId);
		this.server = match.getServer();
		this.moderatorTag = event.getInteraction().getUser().getTag();
	}

	protected boolean isByAdminOrModeratorDoReply() {// TODO das geht auch anders und schoener? vllt mit ButtonCommand::doExecute
		// wahrscheinlich anpassen zusammen mit error handling
		boolean isByAdminOrModerator = event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getAdminRoleId()))
				|| event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getModRoleId()));
		if (!isByAdminOrModerator) event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
		return isByAdminOrModerator;
	}

	protected Mono<Message> postToDisputeChannel(String text) {
		return event.getInteraction().getChannel().flatMap(messageChannel -> messageChannel.createMessage(text));
	}

	protected void updateButtons() {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
	}

}
