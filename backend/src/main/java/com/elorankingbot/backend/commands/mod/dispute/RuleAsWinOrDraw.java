package com.elorankingbot.backend.commands.mod.dispute;

import com.elorankingbot.backend.commands.ButtonCommand;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.MatchResult;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.elorankingbot.backend.model.ReportStatus.*;

public abstract class RuleAsWinOrDraw extends ButtonCommand {

	protected int winningTeamIndex;
	protected boolean isRuleAsWin;
	protected String moderatorTag;
	protected final Match match;

	public RuleAsWinOrDraw(ButtonInteractionEvent event, Services services) {
		super(event, services);
		this.moderatorTag = event.getInteraction().getUser().getTag();
		UUID matchId = UUID.fromString(event.getCustomId().split(":")[1]);
		this.match = dbService.getMatch(matchId);
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

		if (isRuleAsWin) {
			for (int i = 0; i < match.getNumTeams(); i++) {
				for (Player player : match.getTeams().get(i)) {
					match.reportAndSetConflictData(player.getId(), i == winningTeamIndex ? WIN : LOSE);
				}
			}
		} else {
			for (Player player : match.getPlayers()) {
				match.reportAndSetConflictData(player.getId(), DRAW);
			}
		}
		MatchResult matchResult = MatchService.generateMatchResult(match);

		String rulingMessage = isRuleAsWin ?
				String.format("**%s has ruled this match a %s %s for team #%s.**",
						moderatorTag, WIN.asNoun, WIN.asEmojiAsString(), winningTeamIndex + 1)
				: String.format("**%s has ruled this match a %s %s.**", moderatorTag, DRAW.asNoun, DRAW.asEmojiAsString());
		matchService.processMatchResult(matchResult, match, rulingMessage, manageRoleFailedCallback(event));
		removeButtons();
		acknowledgeEvent();
	}

	protected Mono<Message> postToDisputeChannel(String text) {
		return event.getInteraction().getChannel().flatMap(messageChannel -> messageChannel.createMessage(text));
	}

	protected void removeButtons() {
		event.getInteraction().getMessage().get().edit()
				.withComponents(none).subscribe();
	}
}
