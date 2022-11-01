package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskScheduler;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class Challenge extends SlashCommand {

	public Challenge(ChatInputInteractionEvent event, Services services) {
		super(event, services);
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

	protected void execute() {
		User acceptorUser = event.getOption("player").get().getValue().get().asUser().block();
		if (acceptorUser.isBot()) {
			event.reply("You cannot challenge a bot.").withEphemeral(true).subscribe();
			return;
		}

		//staticExecute(acceptorUser, guildId, game, event, service, bot, queue);
	}

	public static void staticExecute(User acceptorUser, long guildId, Game game, ApplicationCommandInteractionEvent event,
									 DBService service, DiscordBotService bot, TimedTaskScheduler queue) {
		/*
		User challengerUser = event.getInteraction().getUser();
		long challengerId = challengerUser.getId().asLong();
		long acceptorId = acceptorUser.getId().asLong();

		Optional<Player> maybeChallenger = service.findPlayerByGuildIdAndUserId(guildId, challengerId);
		if (maybeChallenger.isPresent() && maybeChallenger.get().isBanned()) {
			Player challenger = maybeChallenger.get();
			event.reply(String.format("You are %s and cannot challenge other players.",
							challenger.getUnbanAtTimeSlot() == -1 ?
									"banned permanently, or until unbanned,"
									: "still banned for " +
									DurationParser.minutesToString(queue.getRemainingDuration(challenger.getUnbanAtTimeSlot()))))
					.subscribe();
			return;
		}
		Optional<Player> maybeAcceptor = service.findPlayerByGuildIdAndUserId(guildId, acceptorId);
		if (maybeAcceptor.isPresent() && maybeAcceptor.get().isBanned()) {
			event.reply(String.format("%s is currently banned and cannot be challenged.",
					acceptorUser.getTag())).subscribe();
			return;
		}

		if (challengerId == acceptorId) {
			event.reply("You cannot challenge yourself.")
					.withEphemeral(true).subscribe();
			return;
		}

		Optional<ChallengeModel> maybeChallenge = service.findChallengeByParticipants(guildId, challengerId, acceptorId);
		if (maybeChallenge.isPresent()) {
			if (maybeChallenge.get().isDispute())
				event.reply("You cannot challenge that player again while there is an unresolved dispute")
						.withEphemeral(true).subscribe();
			else
				event.reply("You already challenged that player. You should have received a private message from me...")
						.withEphemeral(true).subscribe();
			return;
		}

		MessageCreateSpec challengerMessageSpec = MessageCreateSpec.builder()
				.content(String.format("You have challenged %s to a match of %s. I'll let you know when they react.",
						acceptorUser.getTag(), game.getName()))
				.build();
		Message challengerMessage = bot.sendToUser(challengerId, challengerMessageSpec).block();
		MessageCreateSpec acceptorMessageSpec = MessageCreateSpec.builder()
				.content(String.format("**You have been challenged by %s to a match of %s. Accept?**",
						challengerUser.getTag(), game.getName()))
				.addComponent(ActionRow.of(
						Buttons.accept(challengerMessage.getId().asLong()),
						Buttons.decline(challengerMessage.getId().asLong())
				)).build();
		Message acceptorMessage = bot.sendToUser(acceptorId, acceptorMessageSpec).block();

		ChallengeModel challenge = new ChallengeModel(guildId,
				challengerId, challengerMessage.getId().asLong(), challengerMessage.getChannelId().asLong(), challengerUser.getTag(),
				acceptorId, acceptorMessage.getId().asLong(), acceptorMessage.getChannelId().asLong(), acceptorUser.getTag());
		service.saveChallenge(challenge);

		event.reply(String.format("Challenge is registered. I have sent you and %s a message.", challengerUser.getTag()))
				.withEphemeral(true).subscribe();
		queue.addTimedTask(
				TimedTask.TimedTaskType.OPEN_CHALLENGE_DECAY,
				0,//game.getOpenChallengeDecayTime(),
				0,//challenge.getId(),
				0L,
				null);

		 */
	}
}
