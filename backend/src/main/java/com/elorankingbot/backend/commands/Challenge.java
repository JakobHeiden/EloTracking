package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.Buttons;
import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.ChallengeModel;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class Challenge extends SlashCommand {

	public Challenge(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("challenge")
				.description("Challenge a player to a match")
				.addOption(ApplicationCommandOptionData.builder()
						.name("player").description("The player to challenge")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true)
						.build())
				.build();
	}

	public void execute() {
		User targetPlayer = event.getOption("player").get().getValue().get().asUser().block();
		if (targetPlayer.isBot()) {
			event.reply("You cannot challenge a bot.").withEphemeral(true).subscribe();
			return;
		}

		long acceptorId = targetPlayer.getId().asLong();
		staticExecute(acceptorId, guildId, game, event, service, bot, queue);
	}

	public static void staticExecute(long acceptorId, long guildId, Game game, ApplicationCommandInteractionEvent event,
									 EloRankingService service, DiscordBotService bot, TimedTaskQueue queue) {
		long challengerId = event.getInteraction().getUser().getId().asLong();

		if (challengerId == acceptorId) {
			event.reply("You cannot challenge yourself.")
					.withEphemeral(true).subscribe();
			return;
		}
		Optional<ChallengeModel> maybeChallenge = service.findChallengeByParticipants(guildId, challengerId, acceptorId);
		if (maybeChallenge.isPresent()) {
			if (maybeChallenge.get().isDispute())
				event.reply("You cannot challenge that player again while there is an unresolved dispute")
						.withEphemeral(true).subscribe();//TODO verlinken
			else event.reply("You already challenged that player. You should have received a private message from me...")
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
						Buttons.accept(challengerMessage.getChannelId().asLong()),
						Buttons.decline(challengerMessage.getChannelId().asLong())
				)).build();
		Message acceptorMessage = bot.sendToUser(acceptorId, acceptorMessageSpec).block();

		ChallengeModel challenge = new ChallengeModel(guildId,
				challengerId, challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(),
				acceptorId, acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong());

		queue.addTimedTask(
				TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY,
				game.getOpenChallengeDecayTime(),
				challenge.getId(),
				0L, null);
		service.saveChallenge(challenge);
		event.reply(String.format("Challenge is registered. I have sent you and %s a message.",
						bot.getPlayerName(acceptorId)))
				.withEphemeral(true).subscribe();
	}
}
