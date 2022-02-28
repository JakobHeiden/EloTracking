package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

public class Cancel extends ButtonCommandRelatedToMatch {

	public Cancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		ChallengeModel.ReportIntegrity reportIntegrity;
		if (isChallengerCommand) reportIntegrity = challenge.setChallengerReported(ChallengeModel.ReportStatus.CANCEL);
		else reportIntegrity = challenge.setAcceptorReported(ChallengeModel.ReportStatus.CANCEL);

		if (reportIntegrity == ChallengeModel.ReportIntegrity.FIRST_TO_REPORT) processFirstToReport();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.HARMONY) processHarmony();
		if (reportIntegrity == ChallengeModel.ReportIntegrity.CONFLICT) processConflict();
		event.acknowledge().subscribe();
	}

	private void processFirstToReport() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a cancel :negative_squared_cross_mark:. " +
						"I'll let you know when your opponent reacts.")
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent called for a cancel :negative_squared_cross_mark:.")
				.resend().subscribe(super::updateAndSaveChallenge);
	}

	private void processHarmony() {
		service.deleteChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You agreed to a cancel :negative_squared_cross_mark:. " +
						"The challenge has been canceled.")
				.makeAllItalic()
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine("Your opponent agreed to a cancel :negative_squared_cross_mark:. " +
						"The challenge has been canceled.")
				.makeAllItalic()
				.resend()
				.withComponents(none).subscribe();

		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
		//queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
		//		targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), null);
	}

	private void processConflict() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a cancel :negative_squared_cross_mark:. Your report and that of your " +
						"opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, and/or call for a cancel, or file a dispute.")
				.makeLastLineBold()
				.update()
				//.withComponents(createActionrow(challenge.getId()))
				.subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
						"Your report and that of your opponent is in conflict.")
				.addLine("You can call for a redo of the reporting, " +
						"and/or call for a cancel, or file a dispute.")
				.makeLastLineBold()
				.resend()
				//.withComponents(createActionrow(challenge.getId()))
				.subscribe(super::updateAndSaveChallenge);
	}

	private static ActionRow createActionrow(long challengeId) {
		return null;
		/*
		return ActionRow.of(
				Buttons.redo(challengeId),
				Buttons.cancelOnConflict(challengeId),
				Buttons.redoOrCancelOnConflict(challengeId),
				Buttons.dispute(challengeId));

		 */
	}
}
