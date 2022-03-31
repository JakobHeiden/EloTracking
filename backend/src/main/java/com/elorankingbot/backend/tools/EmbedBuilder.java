package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.model.*;
import com.google.common.base.Strings;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.elorankingbot.backend.tools.FormatTools.formatRating;

public class EmbedBuilder {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

	public static EmbedCreateSpec createCompletedMatchEmbed(String title, Match match, MatchResult matchResult, String tagToHighlight) {
		return createMatchEmbedOrCompletedMatchEmbed(title, match, matchResult, tagToHighlight);
	}

	public static EmbedCreateSpec createMatchEmbed(String title, Match match, String tagToHighlight) {
		return createMatchEmbedOrCompletedMatchEmbed(title, match, null, tagToHighlight);
	}

	// TODO this really needs to be two separate methods that make use of common private methods
	private static EmbedCreateSpec createMatchEmbedOrCompletedMatchEmbed(String title, Match match, MatchResult matchResult, String tagToHighlight) {
		MatchFinderQueue queue = match.getQueue();
		boolean isCompletedMatch = matchResult != null;

		List<String> embedTexts = new ArrayList<>();
		for (List<Player> players : match.getTeams()) {
			String embedText = "";
			for (Player player : players) {
				ReportStatus reportStatus = match.getReportStatus(player.getId());
				String reportStatusIcon = " " + reportStatus.emoji.asUnicodeEmoji().get().getRaw();
				if (match.getConflictingReports().contains(player)) {
					reportStatusIcon += Emojis.exclamation.asUnicodeEmoji().get().getRaw();
				}
				embedText += String.format("%s (%s%s)%s\n",
						player.getTag(),
						isCompletedMatch ?
								formatRating(matchResult.getPlayerMatchResult(player.getId()).getNewRating())
								: formatRating(player.getGameStats(queue.getGame()).getRating()),
						isCompletedMatch ?
								", " + matchResult.getPlayerMatchResult(player.getId()).getRatingChangeAsString()
								: "",
						reportStatusIcon);
			}
			embedTexts.add(embedText);
		}

		var embedBuilder = EmbedCreateSpec.builder()
				.title(title);
		for (int i = 0; i < queue.getNumTeams(); i++) {
			String embedText = embedTexts.get(i);// TODO rating auch bold
			if (tagToHighlight != null) embedText = embedText.replace(tagToHighlight, "**" + tagToHighlight + "**");
			embedBuilder.addField(EmbedCreateFields.Field.of("Team #" + (i + 1), embedText, true));
		}
		if (isCompletedMatch && match.isOrWasConflict()) embedBuilder.footer(EmbedCreateFields.Footer.of(
				"There was conflicting reporting but it has been resolved by players redoing their reports.", null));

		return embedBuilder.build();
	}

	public static EmbedCreateSpec createMatchResultEmbed(MatchResult matchResult) {
		var embedBuilder = EmbedCreateSpec.builder()
				.title(matchResult.getGame().getName());
		for (TeamMatchResult teamMatchResult : matchResult.getTeamMatchResults()) {
			String fieldTitle = String.format("%s %s\n",
					teamMatchResult.getResultStatus().asCapitalizedNoun(),
					teamMatchResult.getResultStatus().asEmojiAsString());
			String fieldText = "";
			for (PlayerMatchResult playerMatchResult : teamMatchResult.getPlayerMatchResults()) {
				fieldText += String.format("%s (%s, %s)\n",
						playerMatchResult.getPlayerTag(),
						formatRating(playerMatchResult.getNewRating()),
						playerMatchResult.getRatingChangeAsString());
			}
			embedBuilder.addField(EmbedCreateFields.Field.of(fieldTitle, fieldText, true));
		}
		return embedBuilder.build();
	}

	public static EmbedCreateSpec createMatchHistoryEmbed(Player player, List<Optional<MatchResult>> matchResults) {
		String embedText = String.join("\n",
				matchResults.stream().map(maybeMatchResult -> {
					if (maybeMatchResult.isEmpty()) return "Match not found";
					MatchResult matchResult = maybeMatchResult.get();

					List<Player> ownTeam = matchResult.getTeamMatchResults().stream()
							.filter(teamMatchResult -> teamMatchResult.getPlayers().contains(player))
							.findAny().get().getPlayers();
					List<List<Player>> otherTeams = matchResult.getTeamMatchResults().stream()
							.map(TeamMatchResult::getPlayers)
							.filter(players -> !players.contains(player)).toList();
					return String.format("%s %s %s %s on %s",
							matchResult.getPlayerMatchResult(player.getId()).getResultStatus().asEmojiAsString(),
							createTeamString(ownTeam),
							matchResult.getPlayerMatchResult(player.getId()).getResultStatus().asRelationalVerb,
							createSeveralTeamsString(otherTeams),
							dateFormat.format(matchResult.getDate()));
				}).toList());
		Optional<MatchResult> maybeAnyMatchResult = matchResults.stream().filter(Optional::isPresent).map(Optional::get).findAny();
		if (maybeAnyMatchResult.isEmpty()) {
			return EmbedCreateSpec.builder()
					.title("Match history not found")
					.description("Match history not found")
					.build();
		}
		return EmbedCreateSpec.builder()
				.title(String.format("Match history for %s: %s", player.getTag(), maybeAnyMatchResult.get().getGame().getName()))
				.description(embedText)
				.build();
	}

	private static String createTeamString(List<Player> team) {
		return String.join(", ", team.stream().map(player -> player.getTag()).toList());
	}

	private static String createSeveralTeamsString(List<List<Player>> teams) {
		if (teams.size() == 1) return createTeamString(teams.get(0));

		return String.join(", ", teams.stream().map(team -> String.format("(%s)", createTeamString(team))).toList());
	}

	private static int embedRankSpaces = 6;
	private static int embedRatingSpaces = 8;
	private static int embedWinsSpaces = 5;// TODO abhaengig von den daten
	private static String embedName = "`   Rank  Rating   Wins Losses  Name`";
	private static String embedNameWithDraws = "`   Rank  Rating    Wins Losses Draws Name`";
	private static String descriptionSpacerLine = "-----------------------------------------------";

	public static EmbedCreateSpec createRankingsEmbed(RankingsExcerpt rankingsExcerpt) {
		Game game = rankingsExcerpt.game();
		String title = rankingsExcerpt.tagToHighlight().isEmpty() ? game.getName() + " leaderboard"
				: String.format("Rankings for %s: %s", rankingsExcerpt.tagToHighlight().get(),
				rankingsExcerpt.game().getName());

		if (rankingsExcerpt.rankingsEntries().isEmpty()) {
			return EmbedCreateSpec.builder()
					.title(title)
					.description("Not present in rankings.")
					.footer(String.format("%s players total", rankingsExcerpt.numTotalPlayers()), null)
					.build();
		}

		List<RankingsEntry> rankingsEntries = rankingsExcerpt.rankingsEntries();
		String leaderboardString = "";
		for (int i = 0; i < rankingsEntries.size(); i++) {
			RankingsEntry rankingsEntry = rankingsEntries.get(i);
			String numDrawsString = game.isAllowDraw() ? entryOf(rankingsEntry.getDraws(), embedWinsSpaces) : "";
			String rankingsRow = entryOf(i + rankingsExcerpt.highestRank(), embedRankSpaces)
					+ entryOf(rankingsEntry.getRating(), embedRatingSpaces)
					+ entryOf(rankingsEntry.getWins(), embedWinsSpaces)
					+ entryOf(rankingsEntry.getLosses(), embedWinsSpaces)
					+ numDrawsString
					+ "  " + rankingsEntry.getPlayerTag() + "\n";
			if (rankingsExcerpt.tagToHighlight().isPresent() && rankingsRow.contains(rankingsExcerpt.tagToHighlight().get()))
				rankingsRow = "+" + rankingsRow.substring(1);
			leaderboardString += rankingsRow;
		}
		if (leaderboardString.equals("")) leaderboardString = "no games played so far";
		leaderboardString = "```diff\n" + leaderboardString + "```";

		return EmbedCreateSpec.builder()
				.title(title)
				.description(descriptionSpacerLine)
				.addField(EmbedCreateFields.Field.of(
						game.isAllowDraw() ? embedNameWithDraws : embedName,
						leaderboardString,
						true))
				.footer(String.format("%s players total", rankingsExcerpt.numTotalPlayers()), null)
				.build();
	}

	private static String entryOf(String data, int totalSpaces) {
		data = Strings.padEnd(data, (totalSpaces + data.length()) / 2, ' ');
		return Strings.padStart(data, totalSpaces, ' ');
	}

	private static String entryOf(int data, int totalSpaces) {
		return entryOf(String.valueOf(data), totalSpaces);
	}

	private static String entryOf(double data, int totalSpaces) {
		return entryOf(FormatTools.formatRating(data), totalSpaces);
	}

	public static String makeTitleForIncompleteMatch(Match match, boolean hasPlayerReported, boolean isConflict) {
		if (isConflict) {
			if (hasPlayerReported) {
				return "There is conflicting reporting. You can redo your reporting, or file a dispute.";
			} else {
				return "There is conflicting reporting. You can file a dispute.";
			}
		} else {
			if (hasPlayerReported) {
				return "Not all players have reported yet.";
			} else {
				return String.format("Your match of %s %s is starting. " +// TODO queue-name weg wenn nur 1 queue
								"I removed you from all other queues you joined on this server, if any. " + // TODO auflisten welche queues
								"Please play the match and come back to report the result afterwards.",
						match.getQueue().getGame().getName(),
						match.getQueue().getName());
			}
		}
	}

	public static EmbedCreateSpec createQueueGroupMessageEmbed(MatchFinderQueue queue, List<User> users, String activeUserTag) {
		String title = String.format("%s has nominated the following group to join the queue %s%s:",
				activeUserTag, queue.getGame().getName(),
				queue.getGame().hasSingularQueue() ? "" : " " + queue.getName());
		return null; /*EmbedCreateSpec.builder()
				.title(title)
				.addFields(users.stream().map())
				*/
		// TODO
	}

	public static EmbedCreateSpec createHelpEmbed(String title, String content) {
		return EmbedCreateSpec.builder()
				.addField(EmbedCreateFields.Field.of(title, content, true))
				.build();
	}
}
