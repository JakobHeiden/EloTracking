package com.elorankingbot.backend.tools;

import com.elorankingbot.backend.model.*;
import com.google.common.base.Strings;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

import static com.elorankingbot.backend.tools.FormatTools.formatRating;

public class EmbedBuilder {

	public static EmbedCreateSpec createCompletedMatchEmbed(String title, Match match, MatchResult matchResult, String tagToHighlight) {
		return createMatchEmbedOrCompletedMatchEmbed(title, match, matchResult, tagToHighlight);
	}

	public static EmbedCreateSpec createMatchEmbed(String title, Match match, String tagToHighlight) {
		return createMatchEmbedOrCompletedMatchEmbed(title, match, null, tagToHighlight);
	}

	private static EmbedCreateSpec createMatchEmbedOrCompletedMatchEmbed(String title, Match match, MatchResult matchResult, String tagToHighlight) {
		MatchFinderQueue queue = match.getQueue();
		boolean isCompletedMatch = matchResult != null;

		List<String> embedTexts = new ArrayList<>();
		for (List<Player> players : match.getTeams()) {
			String embedText = "";
			for (Player player : players) {
				ReportStatus reportStatus = match.getReportStatus(player.getId());
				String reportStatusIcon = reportStatus.getEmoji().asUnicodeEmoji().get().getRaw();
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
			embedBuilder.addField(EmbedCreateFields.Field.of(
					"Team #" + (i + 1),
					embedTexts.get(i).replace(tagToHighlight, "**" + tagToHighlight + "**"),// TODO rating auch bold
					true));
		}

		return embedBuilder.build();
	}

	public static EmbedCreateSpec createMatchResultEmbed(MatchResult matchResult) {
		var embedBuilder = EmbedCreateSpec.builder();
		for (TeamMatchResult teamMatchResult : matchResult.getTeamMatchResults()) {
			String embedTitle = String.format("%s %s\n",
					teamMatchResult.getResultStatus().asNoun(),
					teamMatchResult.getResultStatus().getEmojiAsString());
			String embedText = "";
			for (PlayerMatchResult playerMatchResult : teamMatchResult.getPlayerMatchResults()) {
				embedText += String.format("%s (%s, %s)\n",
						playerMatchResult.getPlayerTag(),
						formatRating(playerMatchResult.getNewRating()),
						playerMatchResult.getRatingChangeAsString());
			}
			embedBuilder.addField(EmbedCreateFields.Field.of(embedTitle, embedText, true));
		}
		return embedBuilder.build();
	}

	private static int embedRankSpaces = 6;
	private static int embedRatingSpaces = 8;
	private static int embedWinsSpaces = 5;// TODO abhaengig von den daten
	private static String embedName = "`   Rank  Rating   Wins Losses  Name`";
	private static String embedNameWithDraws = "`   Rank  Rating    Wins Losses Draws Name`";

	public static EmbedCreateSpec createRankingsEmbed(RankingsExcerpt rankingsExcerpt) {
		List<RankingsEntry> rankingsEntries = rankingsExcerpt.rankingsEntries();
		Game game = rankingsExcerpt.game();
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
				.title(game.getName() + " Rankings")
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

}
