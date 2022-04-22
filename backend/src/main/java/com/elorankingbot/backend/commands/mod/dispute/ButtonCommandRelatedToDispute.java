package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.Buttons;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

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

	protected boolean isByModeratorOrAdminDoReply() {// TODO das geht auch anders und schoener? vllt mit ButtonCommand::doExecute
		// wahrscheinlich anpassen zusammen mit error handling
		boolean result = event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getAdminRoleId()))
				|| event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(server.getModRoleId()));
		if (!result) event.reply("Only a Moderator can use this.").withEphemeral(true).subscribe();
		return result;
	}

	protected void postToDisputeChannelAndUpdateButtons(String text) {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
		event.getInteraction().getChannel().subscribe(messageChannel ->
				messageChannel.createMessage(text)
						.withComponents(channelDisposal()).subscribe());
	}

	private static ActionRow channelDisposal() {
		return ActionRow.of(
				Buttons.deleteChannelNow(),
				Buttons.archiveAndDeleteChannelLater()
		);
	}
}
