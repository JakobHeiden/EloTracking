package com.elorankingbot.backend.commands.player.challenge;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class Lose extends ButtonCommandRelatedToChallenge {

	public Lose(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
				GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.LOSE);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.LOSE);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) processFirstToReport();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) processHarmony();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) processConflict();
		event.acknowledge().subscribe();
	}

	private void processFirstToReport() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a loss :arrow_down:. I'll let you know when your opponent reports.")
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent reported a loss :arrow_down:.")
				.resend().subscribe(super::updateAndSaveChallenge);
	}

	private void processHarmony() {
		/*
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getChallengerUserId());
		service.addNewPlayerIfPlayerNotPresent(guildId, challenge.getAcceptorUserId());
		Match match = new Match(guildId,// TODO vllt per if umbauen
				isChallengerCommand ? challenge.getAcceptorUserId() : challenge.getChallengerUserId(),
				isChallengerCommand ? challenge.getChallengerUserId() : challenge.getAcceptorUserId(),
				isChallengerCommand ? challenge.getAcceptorTag() : challenge.getChallengerTag(),
				isChallengerCommand ? challenge.getChallengerTag() : challenge.getAcceptorTag(),
				false);
		double[] eloResults = service.updateRatingsAndSaveMatchAndPlayers(match);// TODO transaction machen?
		service.deleteChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a loss :arrow_down:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine("Your opponent reported a loss :arrow_down:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		bot.postToResultChannel(game, match);
		bot.updateLeaderboard(game);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), match);

		 */
	}

	private void processConflict() {
		/*
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a loss :arrow_down:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold()
				.update()
				.withComponents(Win.createActionRow(challenge.getId())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent reported a loss :arrow_down:. " +
						"Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold()
				.resend()
				.withComponents(Win.createActionRow(challenge.getId()))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}
}
