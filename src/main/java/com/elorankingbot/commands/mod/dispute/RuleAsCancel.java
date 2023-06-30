package com.elorankingbot.commands.mod.dispute;

import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.Player;
import com.elorankingbot.service.MatchService;
import com.elorankingbot.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends RuleAsWinOrDraw {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		boolean userIsAdmin = event.getInteraction().getMember().get()
				.getRoleIds().stream().map(Snowflake::asLong).toList()
				.contains(server.getAdminRoleId());
		boolean userIsMod = event.getInteraction().getMember().get()
				.getRoleIds().stream().map(Snowflake::asLong).toList()
				.contains(server.getModRoleId());
		boolean isAdjudicatingOwnMatch = match.containsPlayer(Player.generateId(server.getGuildId(),
				event.getInteraction().getUser().getId().asLong()));
		if (!userIsAdmin && !userIsMod) {
			event.reply(String.format("Only <@&%s> and <@&%s> can adjudicate a match.",
					server.getAdminRoleId(), server.getModRoleId())).withEphemeral(true).subscribe();
			return;
		}
		if (isAdjudicatingOwnMatch && !userIsAdmin) {
			event.reply("Moderators cannot adjudicate their own match.").withEphemeral(true).subscribe();
			return;
		}

		acknowledgeEvent();
		String reason = String.format("%s has ruled the match to be canceled.", moderatorTag);
		MatchResult canceledMatchResult = MatchService.generateCanceledMatchResult(match);
		matchService.processMatchResult(canceledMatchResult, match, reason, manageRoleFailedCallbackFactory());
		removeButtons();
		postToDisputeChannel("**" + reason + "**").block();
		event.getInteraction().getChannel().subscribe(channel -> channelManager.moveToArchive(server, channel));
	}
}
