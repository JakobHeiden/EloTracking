package com.elorankingbot.components;

import com.elorankingbot.FormatTools;
import com.elorankingbot.model.*;
import com.google.common.base.Strings;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.elorankingbot.FormatTools.formatRating;

public class EmbedBuilder {// TODO macht die klasse sinn? vllt eher thematisch sortieren und nicht nach technischem detail?

	public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy hh:mm");

	// TODO in solo matches keine teams anzeigen
	public static EmbedCreateSpec createMatchEmbed(String title, Match match) {
		MatchFinderQueue queue = match.getQueue();
		List<String> embedTexts = new ArrayList<>();
		for (List<Player> players : match.getTeams()) {
			StringBuilder embedText = new StringBuilder();
			for (Player player : players) {
				ReportStatus reportStatus = match.getReportStatus(player.getId());
				String reportStatusIcon = " " + reportStatus.emoji.asUnicodeEmoji().get().getRaw();
				if (match.getConflictingReports().contains(player)) {
					reportStatusIcon += Emojis.exclamation.asUnicodeEmoji().get().getRaw();
				}
				embedText.append(String.format("%s (%s)%s\n",
						player.getTag(),
						formatRating(player.getOrCreatePlayerGameStats(queue.getGame()).getRating()),
						reportStatusIcon));
			}
			embedTexts.add(embedText.toString());
		}

		var embedBuilder = EmbedCreateSpec.builder()
				.title(title);
		for (int i = 0; i < queue.getNumTeams(); i++) {
			embedBuilder.addField(EmbedCreateFields.Field.of("Team #" + (i + 1), embedTexts.get(i), true));
		}
		return embedBuilder.build();
	}

	public static EmbedCreateSpec createCompletedMatchEmbed(String title, MatchResult matchResult) {
		List<String> embedTexts = new ArrayList<>();
		for (TeamMatchResult teamMatchResult : matchResult.getTeamMatchResults()) {
			String embedText = "";
			for (PlayerMatchResult playerMatchResult : teamMatchResult.getPlayerMatchResults()) {
				Player player = playerMatchResult.getPlayer();
				ReportStatus resultStatus = playerMatchResult.getResultStatus();
				String resultStatusIcon = " " + resultStatus.emoji.asUnicodeEmoji().get().getRaw();
				embedText += String.format("%s (%s%s)%s\n",
						player.getTag(),
						formatRating(playerMatchResult.getNewRating()),
						", " + playerMatchResult.getRatingChangeAsString(),
						resultStatusIcon);
			}
			embedTexts.add(embedText);
		}

		var embedBuilder = EmbedCreateSpec.builder()
				.title(title);
		for (int i = 0; i < matchResult.getTeamMatchResults().size(); i++) {
			embedBuilder.addField(EmbedCreateFields.Field.of("Team #" + (i + 1), embedTexts.get(i), true));
		}
		return embedBuilder.build();
	}

	// TODO this really needs to be two separate methods that make use of common private methods
	// TODO no usages? what's up here?
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
								: formatRating(player.getOrCreatePlayerGameStats(queue.getGame()).getRating()),
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

		return embedBuilder.build();
	}

	public static EmbedCreateSpec createMatchResultEmbed(MatchResult matchResult) {// TODO anpassen, zb queue name und K...?
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
				matchResults.stream().map(maybeMatchResult -> createMatchHistoryEntry(player, maybeMatchResult)).toList());
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

	private static String createMatchHistoryEntry(Player player, Optional<MatchResult> maybeMatchResult) {
		if (maybeMatchResult.isEmpty()) return "Match not found";

		MatchResult matchResult = maybeMatchResult.get();
		List<Player> ownTeam = matchResult.getTeamMatchResults().stream()
				.filter(teamMatchResult -> teamMatchResult.getPlayers().contains(player))
				.findAny().get().getPlayers().stream().filter(pl -> !pl.equals(player)).toList();
		List<List<Player>> otherTeams = matchResult.getTeamMatchResults().stream()
				.map(TeamMatchResult::getPlayers)
				.filter(players -> !players.contains(player)).toList();
		String result = String.format("`%s` %s %s %s %s",
				dateFormat.format(matchResult.getTimestamp()),
				matchResult.getPlayerMatchResult(player.getId()).getResultStatus().asEmojiAsString(),
				createOwnTeamString(ownTeam),
				matchResult.getPlayerMatchResult(player.getId()).getResultStatus().asRelationalVerb,
				createSeveralTeamsString(otherTeams));
		if (matchResult.isReverted()) {
			result = String.format("~~%s~~ reverted %s", result, EmbedBuilder.dateFormat.format(matchResult.getRevertedWhen()));
		}
		return result;
	}

	private static String createOwnTeamString(List<Player> team) {
		if (team.isEmpty()) return "";
		return "with " + String.join(", ", team.stream().map(Player::getTag).toList()) + ",";
	}

	private static String createSeveralTeamsString(List<List<Player>> teams) {
		if (teams.size() == 1) return createOtherTeamString(teams.get(0));
		return String.join(", ", teams.stream().map(team -> String.format("(%s)", createOtherTeamString(team))).toList());
	}

	private static String createOtherTeamString(List<Player> team) {
		return String.join(", ", team.stream().map(Player::getTag).toList());
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
					.description(rankingsExcerpt.tagToHighlight().isPresent() ? "Not present in rankings." : "Rankings are empty.")
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

}
