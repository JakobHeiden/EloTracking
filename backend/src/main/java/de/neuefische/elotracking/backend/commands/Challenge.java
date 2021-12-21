package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.command.Emojis;
import de.neuefische.elotracking.backend.command.MessageContent;
import de.neuefische.elotracking.backend.model.ChallengeModel;
import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTask;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class Challenge extends ApplicationCommandInteractionCommand {

	public Challenge(ApplicationCommandInteractionEvent event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		super(event, service, bot, queue);
	}

	public void execute() {
		setupGameIfNotPresent();

		long challengerId = event.getInteraction().getUser().getId().asLong();
		long acceptorId = 0L;
		if (event instanceof ChatInputInteractionEvent) {
			acceptorId = ((ChatInputInteractionEvent) event).getOption("player").get().getValue().get().asUser().block().getId().asLong();
		} else if (event instanceof UserInteractionEvent) {
			acceptorId = ((UserInteractionEvent) event).getTargetId().asLong();
		}

		if (challengerId == acceptorId) {
			event.reply("You cannot challenge yourself.").withEphemeral(true).subscribe();
			return;
		}
		if (service.challengeExistsByParticipants(guildId, challengerId, acceptorId)) {
			event.reply("You already challenged that player. You should have received a private message from me...")
					.withEphemeral(true).subscribe();
			return;
		}

		MessageContent challengerMessageContent = new MessageContent(
				String.format("You have challenged <@%s> to a match. I'll let you know when they react.", acceptorId));
		MessageCreateSpec challengerMessageSpec = MessageCreateSpec.builder()
				.content(challengerMessageContent.get())
				.build();
		Message challengerMessage = bot.sendToUser(challengerId, challengerMessageSpec).block();

		MessageContent acceptorMessageContent = new MessageContent(
				String.format("You have been challenged to a match by <@%s>. Accept?", challengerId))
				.makeLastLineBold();
		MessageCreateSpec acceptorMessageSpec = MessageCreateSpec.builder()
				.content(acceptorMessageContent.get())
				.addComponent(ActionRow.of(
						Button.primary("accept:" + challengerMessage.getChannelId().asString(),
								Emojis.checkMark, "Accept"),
						Button.primary("reject:" + challengerMessage.getChannelId().asString(),
								Emojis.crossMark, "Reject")
				)).build();
		Message acceptorMessage = bot.sendToUser(acceptorId, acceptorMessageSpec).block();

		service.addNewPlayerIfPlayerNotPresent(guildId, challengerId);
		ChallengeModel challenge = new ChallengeModel(guildId,
				challengerId, challengerMessage.getId().asLong(),
				acceptorId, acceptorMessage.getId().asLong());

		queue.addTimedTask(TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY, game.getOpenChallengeDecayTime(), guildId);
		service.saveChallenge(challenge);
		event.reply(String.format("Challenge is registered. I have sent you and %s a message.",
				bot.getPlayerName(acceptorId)))
				.withEphemeral(true).subscribe();
	}

	private void setupGameIfNotPresent() {
		Optional<Game> maybeGame = service.findGameByGuildId(guildId);
		if (maybeGame.isEmpty()) {
			game = new Game(guildId, "name not set");
			service.saveGame(game);
		} else {
			game = maybeGame.get();
		}
	}
}
