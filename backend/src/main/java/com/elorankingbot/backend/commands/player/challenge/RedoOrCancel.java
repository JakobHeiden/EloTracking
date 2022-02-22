package com.elorankingbot.backend.commands.player.challenge;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

public class RedoOrCancel extends ButtonCommandRelatedToChallenge {

	public RedoOrCancel(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		boolean bothCalledForRedo;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForRedo(true);
			bothCalledForRedo = challenge.isAcceptorCalledForRedo();
		} else {
			challenge.setAcceptorCalledForRedo(true);
			bothCalledForRedo = challenge.isChallengerCalledForRedo();
		}

		boolean bothCalledForCancel;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForCancel(true);
			bothCalledForCancel = challenge.isAcceptorCalledForCancel();
		} else {
			challenge.setAcceptorCalledForCancel(true);
			bothCalledForCancel = challenge.isChallengerCalledForCancel();
		}

		boolean opponentDidNotCallEither = !bothCalledForCancel && !bothCalledForRedo;

		if (opponentDidNotCallEither) opponentDidNotCallEither();
		// this should be dead code, but might get accessed by simultaneous button presses by both parties...?
		// TODO figure this out
		else if (bothCalledForRedo) {
			bothCalledForRedo();
			bot.sendToOwner("Redo.bothCalledForRedo()");
		}
		else if (bothCalledForCancel) {
			CancelOnConflict.bothCalledForCancel(parentMessage, targetMessage, challenge, game, service, queue);
			bot.sendToOwner("Redo.bothCalledForRedo()");
		}
		event.acknowledge().subscribe();
	}

	private void opponentDidNotCallEither() {
		/*
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a redo or a cancel :person_shrugging:. You can still file a dispute.")
				.update()
				.withComponents(ActionRow.of(
						Buttons.dispute(challenge.getId()))).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.makeLastLineStrikeThrough()
				.addLine("Your opponent called for a redo or a cancel :person_shrugging:. " +
						"You can accept either, or file a dispute")
				.makeLastLineBold()
				.resend()
				.withComponents(ActionRow.of(
						Buttons.agreeToRedo(challenge.getId()),
						Buttons.agreeToCancelOnConflict(challenge.getId()),
						Buttons.dispute(challenge.getId())))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}

	private void bothCalledForRedo() {
		/*
		challenge.redo();

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine(String.format("You agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s?", game.isAllowDraw() ? " or draw" : ""))
				.makeLastLineBold()
				.update()
				.withComponents(Redo.createActionRow(targetMessage.getChannelId().asLong(), game.isAllowDraw())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine(String.format("Your opponent agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s?", game.isAllowDraw() ? " or draw" : ""))
				.makeLastLineBold()
				.resend()
				.withComponents(Redo.createActionRow(parentMessage.getChannelId().asLong(), game.isAllowDraw()))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}
}

