package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

public class Forcematch extends SlashCommand {

	private User user1;
	private User user2;
	private String reason;
	private Match match;
	private boolean isDraw;
	private String templatePlayer1;
	private String templatePlayer2;

	public Forcematch(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest(boolean allowDraw) {
		var modeOptionBuilder = ApplicationCommandOptionData.builder()
				.name("mode").description(allowDraw ?
						"Force a win, a draw, or undo the most recent match of two players?"
						: "Force a win, or undo the most recent match of two players?")
				.type(ApplicationCommandOption.Type.STRING.getValue()).required(true)
				.addChoice(ApplicationCommandOptionChoiceData.builder()
						.name("win").value("win").build());
		if (allowDraw) modeOptionBuilder.addChoice(ApplicationCommandOptionChoiceData.builder()
				.name("draw").value("draw").build());
		modeOptionBuilder.addChoice(ApplicationCommandOptionChoiceData.builder()
				.name("undo").value("undo").build());

		return ApplicationCommandRequest.builder()
				.name("forcematch")
				.description("Force resolve or undo a match for two players")
				.addOption(modeOptionBuilder.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("player1").description("The first (winning if forcing a win) player")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("player2").description("The second (losing if forcing a win) player")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the players involved")
						.type(ApplicationCommandOption.Type.STRING.getValue()).required(false)
						.build())
				.defaultPermission(false)
				.build();
	}

	public void execute() {
		user1 = event.getOption("player1").get().getValue().get().asUser().block();
		user2 = event.getOption("player2").get().getValue().get().asUser().block();
		if (user1.isBot() || user2.isBot()) {
			event.reply("Cannot force a match involving a bot.").subscribe();
			return;
		}
		if (user1.equals(user2)) {
			event.reply("Player1 and player2 must not be the same player.").subscribe();
			return;
		}

		reason = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";

		String mode = event.getOption("mode").get().getValue().get().asString();
		if (mode.equals("draw")) {
			isDraw = true;
			forceResolveMatch();
		}
		if (mode.equals("win")) {
			isDraw = false;
			forceResolveMatch();
		}
		if (mode.equals("undo")) {
			undoMatch();
		}
	}

	private void forceResolveMatch() {
		match = new Match(guildId, user1.getId().asLong(), user2.getId().asLong(), isDraw);
		double[] eloResults = service.updateRatings(match);
		service.saveMatch(match);
		templatePlayer1 = isDraw ?
				"%s has forced a draw with %s. Your rating went from %s to %s.%s"
				: "%s has forced a win over %s. Your rating went from %s to %s.%s";
		templatePlayer2 = isDraw ?
				"%s has forced a draw with %s. Your rating went from %s to %s.%s"
				: "%s has forced a loss to %s. Your rating went from %s to %s.%s";
		informPlayers(eloResults);
		bot.postToResultChannel(game, match);
		String template = isDraw ? "Forced a draw between %s and %s.%s" : "Forced a win for %s over %s.%s";
		event.reply(String.format(template, user1.getTag(), user2.getTag(), reason)).subscribe();
	}

	private void informPlayers(double[] eloResults) {
		MessageContent player1MessageContent = new MessageContent(
				String.format(templatePlayer1,
						event.getInteraction().getUser().getTag(), user2.getTag(),
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2]),
						reason))
				.makeAllItalic();
		MessageCreateSpec player1MessageSpec = MessageCreateSpec.builder()
				.content(player1MessageContent.get()).build();
		user1.getPrivateChannel().subscribe(channel -> channel.createMessage(player1MessageSpec).subscribe());

		MessageContent player2MessageContent = new MessageContent(
				String.format(templatePlayer2,
						event.getInteraction().getUser().getTag(), user1.getTag(),
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3]),
						reason))
				.makeAllItalic();
		MessageCreateSpec player2MessageSpec = MessageCreateSpec.builder()
				.content(player2MessageContent.get()).build();
		user2.getPrivateChannel().subscribe(channel -> channel.createMessage(player2MessageSpec).subscribe());
	}

	private void undoMatch() {
		Optional<Match> maybeMostRecentMatch = service.findMostRecentMatch(user1.getId().asLong(), user2.getId().asLong());
		if (maybeMostRecentMatch.isEmpty()) {
			event.reply("These players have no recorded matches.").subscribe();
			return;
		}

		match = maybeMostRecentMatch.get();
		if (match.getWinnerId() != user1.getId().asLong()) {
			User temp = user2;
			user2 = user1;
			user1 = temp;
		}
		double[] eloResultsfromUndo = updateRatingsForUndo();
		service.deleteMatch(match);
		templatePlayer1 = "%s has reverted your most recent match with %s. Your rating went from %s to %s.%s";
		templatePlayer2 = "%s has reverted your most recent match with %s. Your rating went from %s to %s.%s";
		informPlayers(eloResultsfromUndo);
		event.reply(String.format("Reverted the last recorded match between %s and %s.",
				user1.getTag(), user2.getTag())).subscribe();
	}

	private double[] updateRatingsForUndo() {
		Player winner = service.findPlayerByGuildIdAndUserId(guildId, match.getWinnerId()).get();
		double winnerOldRating = winner.getRating();
		double winnerNewRating = winnerOldRating - (match.getWinnerNewRating() - match.getWinnerOldRating());
		winner.setRating(winnerNewRating);
		service.savePlayer(winner);

		Player loser = service.findPlayerByGuildIdAndUserId(guildId, match.getLoserId()).get();
		double loserOldRating = loser.getRating();
		double loserNewRating = loserOldRating - (match.getLoserNewRating() - match.getLoserOldRating());
		loser.setRating(loserNewRating);
		service.savePlayer(loser);

		return new double[]{winnerOldRating, loserOldRating, winnerNewRating, loserNewRating};
	}
}