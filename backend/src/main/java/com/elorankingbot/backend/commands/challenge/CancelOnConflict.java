package com.elorankingbot.backend.commands.challenge;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.Buttons;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;

public class CancelOnConflict extends ButtonCommandRelatedToChallenge {

	public CancelOnConflict(ButtonInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		boolean bothCalledForCancel = false;
		if (isChallengerCommand) {
			challenge.setChallengerCalledForCancel(true);
			bothCalledForCancel = challenge.isAcceptorCalledForCancel();
		} else {
			challenge.setAcceptorCalledForCancel(true);
			bothCalledForCancel = challenge.isChallengerCalledForCancel();
		}

		if (!bothCalledForCancel) oneCalledForCancel(parentMessage, targetMessage, challenge, service);
		if (bothCalledForCancel) bothCalledForCancel(parentMessage, targetMessage, challenge, game, service, queue);
		event.acknowledge().subscribe();
	}

	static void oneCalledForCancel(Message parentMessage, Message targetMessage, ChallengeModel challenge,
								   EloRankingService service) {
		service.saveChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a cancel :negative_squared_cross_mark:. If your opponent does as well, " +
						"the match will be canceled. You can still file a dispute.")
				.makeLastLineBold()
				.update()
				.withComponents(ActionRow.of(
						Buttons.dispute(targetMessage.getChannelId().asLong()))).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("Your opponent called for a cancel :negative_squared_cross_mark:. " +
						"You can agree to a cancel or file a dispute.")
				.update()
				.withComponents(ActionRow.of(
						Buttons.agreeToCancelOnConflict(parentMessage.getChannelId().asLong()),
						Buttons.dispute(parentMessage.getChannelId().asLong()))).subscribe();
	}

	static void bothCalledForCancel(Message parentMessage, Message targetMessage,
									ChallengeModel challenge, Game game,
									EloRankingService service, TimedTaskQueue queue) {
		service.deleteChallenge(challenge);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You agreed to a cancel :negative_squared_cross_mark:. The match is canceled.")
				.update()
				.withComponents(none).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.addLine("Your opponent agreed to a cancel :negative_squared_cross_mark:. The match is canceled.")
				.update()
				.withComponents(none).subscribe();

		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				parentMessage.getId().asLong(), parentMessage.getChannelId().asLong(), null);
		queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
				targetMessage.getId().asLong(), targetMessage.getChannelId().asLong(), null);
	}
}
