package com.elorankingbot.backend.service;

import com.elorankingbot.backend.model.*;

import java.util.List;

public class RatingCalculations {

	private static int k = 16;// TODO

	public static MatchResult generateMatchResult(Match match) {
		MatchResult matchResult = new MatchResult(match);
		Game game = match.getQueue().getGame();
		for (List<Player> team : match.getTeams()) {
			List<Player> otherPlayers = match.getPlayers();
			team.forEach(otherPlayers::remove);
			double averageTeamRating = team.stream()
					.mapToDouble(pl -> pl.getGameStats(game).getRating())
					.average().getAsDouble();
			double averageOtherRating = otherPlayers.stream()
							.mapToDouble(pl -> pl.getGameStats(game).getRating())
							.average().getAsDouble();
			double numOtherTeams = match.getQueue().getNumTeams() - 1;
			double expectedResult = 1 / (numOtherTeams * (1 + Math.pow(10, (averageOtherRating - averageTeamRating) / 400)));

			TeamMatchResult teamResult = new TeamMatchResult();
			for (Player player : team) {
				double actualResult = match.getReportStatus(player.getId()).getValue();
				double oldRating = player.getGameStats(game).getRating();
				double newRating = oldRating + k * (actualResult - expectedResult);
				PlayerMatchResult playerMatchResult = new PlayerMatchResult(matchResult,
						player, player.getTag(),
						ReportStatus.valueOf(match.getReportStatus(player.getId()).name()),
						oldRating, newRating);
				teamResult.add(playerMatchResult);
			}

			matchResult.addTeamMatchResult(teamResult);
		}
		return matchResult;
	}


		/*
	public double[] updateRatingsAndSaveMatchAndPlayers(MatchResult matchResult) {// TODO evtl match zurueckgeben
		Player winner = playerDao.findById(Player.generateId(match.getGuildId(), match.getWinnerId())).get();
		Player loser = playerDao.findById(Player.generateId(match.getGuildId(), match.getLoserId())).get();

		double[] ratings = calculateElo(winner.getRating(), loser.getRating(),
				match.isDraw() ? 0.5 : 1, k);

		match.setWinnerOldRating(winner.getRating());
		match.setWinnerNewRating(ratings[2]);
		winner.setRating(ratings[2]);
		match.setLoserOldRating(loser.getRating());
		match.setLoserNewRating(ratings[3]);
		loser.setRating(ratings[3]);
		if (match.isDraw()) {
			winner.addDraw();
			loser.addDraw();
		} else {
			winner.addWin();
			loser.addLoss();
		}

		savePlayer(winner);
		savePlayer(loser);
		saveMatch(match);

		return ratings;

		return null;
	}

	private static double[] calculateElo(double rating1, double rating2, double player1Result, double k) {
		double player2Result = 1 - player1Result;
		double expectedResult1 = 1 / (1 + Math.pow(10, (rating2 - rating1) / 400));
		double expectedResult2 = 1 / (1 + Math.pow(10, (rating1 - rating2) / 400));
		double newRating1 = rating1 + k * (player1Result - expectedResult1);
		double newRating2 = rating2 + k * (player2Result - expectedResult2);
		return new double[]{rating1, rating2, newRating1, newRating2};
	}

		 */
	public static String formatRating(double rating) {
		return String.format("%.1f", Float.valueOf(Math.round(rating * 10)) / 10);
	}

}
