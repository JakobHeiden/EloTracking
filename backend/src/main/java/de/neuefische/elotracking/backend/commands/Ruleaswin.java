package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.Match;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;

public class Ruleaswin extends ButtonCommandForDispute {

	private long winnerId;
	private long loserId;
	private double[] eloResults;
	private Match match;
	private boolean isChallengerWin;

	public Ruleaswin(ButtonInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (!isByModeratorOrAdmin()) return;

		isChallengerWin = event.getCustomId().split(":")[2].equals("true");
		winnerId = isChallengerWin ? challenge.getChallengerId() : challenge.getAcceptorId();
		loserId = isChallengerWin ? challenge.getAcceptorId() : challenge.getChallengerId();

		match = new Match(challenge.getGuildId(), winnerId, loserId, false);
		eloResults = service.updateRatings(match);
		service.saveMatch(match);
		service.deleteChallenge(challenge);

		postToDisputeChannel(String.format(
				"%s has ruled the match a win :arrow_up: for <@%s> and a loss :arrow_down: for <@%s>.",
				moderatorName, winnerId, loserId));
		bot.postToResultChannel(game, match);
		postToWinnerAndLoserChannels();
		addMatchSummarizeToQueue(match);
	}

	private void postToWinnerAndLoserChannels() {
		Message winnerMessage = isChallengerWin ? challengerMessage : acceptorMessage;
		MessageContent winnerMessageContent = new MessageContent(winnerMessage.getContent())
				.addLine(String.format("%s has ruled this as a win :arrow_up: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s", eloResults[0], eloResults[2]))
				.makeAllItalic();
		winnerMessage.edit().withContent(winnerMessageContent.get())
				.withComponents(none).subscribe();

		Message loserMessage = isChallengerWin ? acceptorMessage : challengerMessage;
		MessageContent loserMessageContent = new MessageContent(loserMessage.getContent())
				.addLine(String.format("%s has ruled this as a loss :arrow_down: for you.", moderatorName))
				.addLine(String.format("Your rating went from %s to %s", eloResults[1], eloResults[3]))
				.makeAllItalic();
		loserMessage.edit().withContent(loserMessageContent.get())
				.withComponents(none).subscribe();
	}
}
