package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.commands.player.match.ButtonCommandRelatedToMatch;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Accept extends ButtonCommandRelatedToMatch {

	public Accept(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		/*
		challenge.setAccepted(true);

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You have accepted the challenge.")
				.addLine(String.format("Come back after the match and let me know if you won or lost %s.",
						game.isAllowDraw() ? " or drew" : ""))
				.makeLastLineBold()
				.update()
				.withComponents(createActionRow(challenge.getId(), game.isAllowDraw())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine("They have accepted your challenge.")
				.addLine(String.format("Come back after the match and let me know if you won or lost %s.",
						game.isAllowDraw() ? " or drew" : ""))
				.makeLastLineBold()
				.resend()
				.withComponents(createActionRow(challenge.getId(), game.isAllowDraw()))
				.subscribe(super::updateAndSaveChallenge);

		queue.addTimedTask(TimedTask.TimedTaskType.ACCEPTED_CHALLENGE_DECAY,
				game.getAcceptedChallengeDecayTime(), challenge.getId(), 0L, null);
		event.deferEdit().subscribe();// should work without, somehow is needed anyway...

		 */
	}

	private static ActionRow createActionRow(long channelId, boolean allowDraw) {
		return null;
		/*
		if (allowDraw) return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.draw(channelId),
				Buttons.cancel(channelId));
		else return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.cancel(channelId));

		 */
	}
}
