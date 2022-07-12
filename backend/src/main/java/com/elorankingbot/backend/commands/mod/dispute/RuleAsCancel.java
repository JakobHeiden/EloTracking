package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RuleAsCancel extends RuleAsWinOrDraw {

	public RuleAsCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	@Override
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

		String reason = String.format("%s has ruled the match to be canceled.", moderatorTag);
		MatchResult canceledMatchResult = MatchService.generateCanceledMatchResult(match);
		matchService.processMatchResult(canceledMatchResult, match, reason);
		removeButtons();
		postToDisputeChannel("**" + reason + "**").block();
		event.getInteraction().getChannel().subscribe(channel -> bot.moveToArchive(server, channel));
		event.acknowledge().subscribe();
	}
}
