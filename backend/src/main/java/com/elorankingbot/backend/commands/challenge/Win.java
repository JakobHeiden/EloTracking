package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Match;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

public class Win extends ButtonCommandRelatedToChallenge {

	public Win(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue,
			   GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.WIN);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.WIN);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) processFirstToReport();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) processHarmony();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) processConflict();
		event.acknowledge().subscribe();
	}

	private void processFirstToReport() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. I'll let you know when your opponent reports.")
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent reported a win :arrow_up:.")
				.resend().subscribe(super::updateAndSaveChallenge);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_AUTO_RESOLVE, game.getMatchAutoResolveTime(),
				challenge.getId(), 0L, null);
	}

	private void processHarmony() {
		Match match = new Match(guildId,
				isChallengerCommand ? challenge.getChallengerId() : challenge.getAcceptorId(),
				isChallengerCommand ? challenge.getAcceptorId() : challenge.getChallengerId(),
				false);
		double[] eloResults = service.updateRatingsAndSaveMatch(match);// TODO transaction machen?
		service.deleteChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[0]), service.formatRating(eloResults[2])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine("Your opponent reported a win :arrow_up:. The match has been resolved:")
				.addLine(String.format("Your rating went from %s to %s.",
						service.formatRating(eloResults[1]), service.formatRating(eloResults[3])))
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		bot.postToResultChannel(game, match);

		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), match);
		queue.addTimedTask(TimedTask.TimedTaskType.MATCH_SUMMARIZE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), match);
	}

	private void processConflict() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, or file a dispute.")
				.makeLastLineBold()
				.update()
				.withComponents(createActionRow(challenge.getId())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent reported a win :arrow_up:. Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, " +
						"or file a dispute.")
				.makeLastLineBold()
				.resend()
				.withComponents(createActionRow(challenge.getId()))
				.subscribe(super::updateAndSaveChallenge);
	}

	static ActionRow createActionRow(long challengeId) {
		return ActionRow.of(
				Buttons.redo(challengeId),
				Buttons.cancelOnConflict(challengeId),
				Buttons.redoOrCancelOnConflict(challengeId),
				Buttons.dispute(challengeId));
	}
}
