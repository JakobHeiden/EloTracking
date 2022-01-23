package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;

public class Redoorcancel extends ButtonCommandRelatedToChallenge {

	public Redoorcancel(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		boolean bothCalledForRedo = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForRedo(true);
			bothCalledForRedo = challenge.isAcceptorCalledForRedo();
		} else {
			challenge.setAcceptorCalledForRedo(true);
			bothCalledForRedo = challenge.isChallengerCalledForRedo();
		}

		boolean bothCalledForCancel = false;
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
			Redo.bothCalledForRedo(parentMessage, targetMessage, challenge, game, service);
			bot.sendToOwner("Redo.bothCalledForRedo()");
		}
		else if (bothCalledForCancel) {
			CancelOnConflict.bothCalledForCancel(parentMessage, targetMessage, challenge, game, service, queue);
			bot.sendToOwner("Redo.bothCalledForRedo()");
		}
		event.acknowledge().subscribe();
	}

	private void opponentDidNotCallEither() {
		service.saveChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a redo or a cancel :person_shrugging:. You can still file a dispute.")
				.update()
				.withComponents(ActionRow.of(
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.makeLastLineStrikeThrough()
				.addLine("Your opponent called for a redo or a cancel :person_shrugging:. " +
						"You can accept either, or file a dispute")
				.makeLastLineBold()
				.update()
				.withComponents(ActionRow.of(
						Buttons.agreeToRedo(parentMessage.getChannelId().asLong()),
						Buttons.agreeToCancelOnConflict(parentMessage.getChannelId().asLong()),
						Buttons.dispute(parentMessage.getChannelId().asLong()))).subscribe();
	}
}

