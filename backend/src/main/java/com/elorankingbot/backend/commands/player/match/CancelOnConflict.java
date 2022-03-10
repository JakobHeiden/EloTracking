package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class CancelOnConflict extends ButtonCommandRelatedToMatch {

	public CancelOnConflict(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		/*
		boolean bothCalledForCancel = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForCancel(true);
			bothCalledForCancel = challenge.isAcceptorCalledForCancel();
		} else {
			challenge.setAcceptorCalledForCancel(true);
			bothCalledForCancel = challenge.isChallengerCalledForCancel();
		}

		if (!bothCalledForCancel) oneCalledForCancel();
		if (bothCalledForCancel) bothCalledForCancel(parentMessage, targetMessage, challenge, game, dbservice, queue);
		event.acknowledge().subscribe();
	}

	private void oneCalledForCancel() {
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a cancel :negative_squared_cross_mark:. If your opponent does as well, " +
						"the match will be canceled. You can still file a dispute.")
				.makeLastLineBold()
				.update()
				//.withComponents(ActionRow.of(
				//		Buttons.dispute(challenge.getId())))
			.subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
						"You can agree to a cancel or file a dispute.")
				.resend()
				//.withComponents(ActionRow.of(
				//		Buttons.agreeToCancelOnConflict(challenge.getId()),
				//		Buttons.dispute(challenge.getId())))
				.subscribe(super::updateAndSaveChallenge);
	}

	static void bothCalledForCancel(Message parentMessage, Message targetMessage,
									ChallengeModel challenge, Game game,
									DBService service, TimedTaskQueue queue) {
		service.deleteChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You agreed to a cancel :negative_squared_cross_mark:. The match is canceled.")
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine("Your opponent agreed to a cancel :negative_squared_cross_mark:. The match is canceled.")
				.resend()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE,
				0,//game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE,
				0,//game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), null);

		 */
	}
}
