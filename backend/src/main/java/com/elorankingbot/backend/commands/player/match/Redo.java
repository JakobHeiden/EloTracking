package com.elorankingbot.backend.commands.player.match;

import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;

public class Redo extends ButtonCommandRelatedToMatch {

	public Redo(ButtonInteractionEvent event, Services services) {
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

		if (!bothCalledForRedo) oneCalledForRedo();
		if (bothCalledForRedo) bothCalledForRedo(parentMessage, targetMessage, challenge, game, service);
		event.acknowledge().subscribe();
	}

	private void oneCalledForRedo() {
		/*
		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine("You called for a redo :leftwards_arrow_with_hook:. If your opponent does as well, " +
						"reports will be redone. You can still file a dispute.")
				.update()
				.withComponents(ActionRow.of(
						Buttons.dispute(challenge.getId()))).subscribe();
		new MessageUpdater(targetMessage)
				.makeAllNotBold()
				.makeLastLineStrikeThrough()
				.addLine("Your opponent called for a redo :leftwards_arrow_with_hook:. " +
						"You can agree to a redo or file a dispute.")
				.makeLastLineBold()
				.resend()
				.withComponents(ActionRow.of(
						Buttons.agreeToRedo(challenge.getId()),
						Buttons.dispute(challenge.getId())))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}

	private void bothCalledForRedo(Message parentMessage, Message targetMessage, ChallengeModel challenge, Game game,
								  EloRankingService service) {
		/*
		challenge.redo();

		new MessageUpdater(parentMessage)
				.makeAllNotBold()
				.addLine(String.format("You agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s?", game.isAllowDraw() ? " or draw" : ""))
				.makeLastLineBold()
				.update()
				.withComponents(createActionRow(challenge.getId(), game.isAllowDraw())).subscribe();
		new MessageUpdater(targetMessage)
				.addLine(String.format("Your opponent agreed to a redo :leftwards_arrow_with_hook:. Reports are redone. " +
						"Did you win or lose%s?", game.isAllowDraw() ? " or draw" : ""))
				.makeLastLineBold()
				.resend()
				.withComponents(createActionRow(challenge.getId(), game.isAllowDraw()))
				.subscribe(super::updateAndSaveChallenge);

		 */
	}

	static ActionRow createActionRow(long channelId, boolean allowDraw) {
		return null;
		/*
		if (allowDraw) return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.draw(channelId));
		else return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId));

		 */
	}
}
