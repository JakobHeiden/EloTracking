package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.commands.player.match.ButtonCommandRelatedToMatch;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.EmbedBuilder;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;

public class Redo extends ButtonCommandRelatedToMatch {

	public Redo(ButtonInteractionEvent event, Services services) {
		super(event, services);
	}

	public void execute() {
		Player activePlayer = match.getPlayer(activeUserId);
		bot.getPlayerMessage(activePlayer, match)
				.subscribe(message -> {
					String embedTitle = "There is conflicting reporting. You can file a dispute.";
					ActionRow actionRow = null;//Report.createConflictActionRow(match.getId(), game.isAllowDraw(), false);
					EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbedOld(embedTitle, match, activePlayer.getTag());
					message.edit().withEmbeds(embedCreateSpec).withComponents(actionRow).subscribe();
				});
		event.acknowledge().subscribe();
	}

	/* TODO! weg
	private void oneCalledForRedo() {
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
	}

	private void bothCalledForRedo(Message parentMessage, Message targetMessage, ChallengeModel challenge, Game game,
								   DBService service) {
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
	}

	static ActionRow createActionRow(long channelId, boolean allowDraw) {
		if (allowDraw) return ActionRow.of(
				Buttons.win(channelId),
				Buttons.lose(channelId),
				Buttons.draw(channelId));
		else return ActionRow.of(
				Buttons.win(channelId),

			Buttons.lose(channelId));
			}
	 */
}