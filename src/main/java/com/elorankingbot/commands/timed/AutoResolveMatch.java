package com.elorankingbot.commands.timed;

import com.elorankingbot.components.EmbedBuilder;
import com.elorankingbot.model.Match;
import com.elorankingbot.model.MatchResult;
import com.elorankingbot.model.Player;
import com.elorankingbot.service.*;
import com.elorankingbot.timedtask.DurationParser;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.elorankingbot.model.ReportStatus.*;

public class AutoResolveMatch {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final ChannelManager channelManager;
	private final MatchService matchService;
	private final int duration;
	private final UUID matchId;
	private Match match;
	private TextChannel matchChannel, disputeChannel;

	public AutoResolveMatch(Services services, UUID matchId, int duration) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.channelManager = services.channelManager;
		this.matchService = services.matchService;
		this.duration = duration;
		this.matchId = matchId;
	}

	public void execute() {
		Optional<Match> maybeMatch = dbService.findMatch(matchId);
		if (maybeMatch.isEmpty()) return;
		match = maybeMatch.get();
		if (match.isDispute()) return;

		if (match.getReportIntegrity().equals(Match.ReportIntegrity.CONFLICT)) {
			processDispute();
			return;
		}

		String autoresolveMessage = "As 60 minutes have passed since the first report, I have auto-resolved the match.";// TODO
		// if ReportIntegrity != CONFLICT, the possible states of the reporting are greatly reduced
		if (match.getPlayerIdToReportStatus().containsValue(CANCEL)) {
			MatchResult canceledMatchResult = MatchService.generateCanceledMatchResult(match);
			matchService.processMatchResult(canceledMatchResult, match, autoresolveMessage, role -> throwable -> {});
			return;
		} else if (match.getPlayerIdToReportStatus().containsValue(DRAW)) {
			match.getPlayers().forEach(player -> match.reportAndSetConflictData(player.getId(), DRAW));
		} else if (match.getPlayerIdToReportStatus().containsValue(WIN)) {
			match.getTeams().forEach(team -> {
				boolean thisTeamReportedWin = team.stream()
						.anyMatch(player -> match.getReportStatus(player.getId()).equals(WIN));
				if (thisTeamReportedWin) {
					team.forEach(player -> match.reportAndSetConflictData(player.getId(), WIN));
				} else {
					team.forEach(player -> match.reportAndSetConflictData(player.getId(), LOSE));
				}
			});
		} else {
			List<List<Player>> teamsReportedLose = match.getTeams().stream()
					.filter(team -> team.stream().anyMatch(player -> match.getReportStatus(player.getId()).equals(LOSE)))
					.toList();
			if (teamsReportedLose.size() == match.getTeams().size() - 1) {
				teamsReportedLose.forEach(team ->
						team.forEach(player -> match.reportAndSetConflictData(player.getId(), LOSE)));
				match.getPlayers().stream()
						.filter(player -> match.getReportStatus(player.getId()).equals(NOT_YET_REPORTED))
						.forEach(player -> match.reportAndSetConflictData(player.getId(), WIN));
			} else {
				processDispute();
				return;
			}
		}
		MatchResult matchResult = MatchService.generateMatchResult(match);
		matchService.processMatchResult(matchResult, match, autoresolveMessage, role -> throwable -> {});
	}

	private void processDispute() {
		match.setDispute(true);
		dbService.saveMatch(match);
		matchChannel = (TextChannel) bot.getChannelById(match.getChannelId()).block();
		disputeChannel = channelManager.createDisputeChannel(match).block();
		sendDisputeLinkMessage();
		createDisputeMessage();
	}

	private void sendDisputeLinkMessage() {
		EmbedCreateSpec embed = EmbedCreateSpec.builder()
				.title(String.format("%s have passed since the first report, and this match is due for auto-resolution. " +
								"However, as there are conflicts in the reporting, I opened a dispute. " +
								"For resolution please follow the link:",
						DurationParser.minutesToString(duration)))
				.description(disputeChannel.getMention()).build();
		matchChannel.createMessage(embed).subscribe(message -> message.pin().subscribe());
	}

	private void createDisputeMessage() {
		String embedTitle = "The reporting at the moment the dispute was filed:";
		EmbedCreateSpec embed = EmbedBuilder.createMatchEmbed(embedTitle, match);
		disputeChannel.createMessage(String.format("""
								Welcome %s. Since this match could not be auto-resolved, I created this dispute.
								Only <@&%s> and affected players can view this channel.
								Please state your side of the conflict so a moderator can resolve it.
								The original match channel can be found here: <#%s>
								Note that the Buttons on this message can only be used by moderators.
								""",
						match.getAllMentions(),
						match.getServer().getModRoleId(),
						match.getChannelId()))
				.withEmbeds(embed)
				.withComponents(ChannelManager.createDisputeActionRow(match))
				.subscribe();
	}
}
