package com.elorankingbot.backend.commands.mod;

import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Player;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.DurationParser;
import com.elorankingbot.backend.timedtask.TimedTask;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import com.elorankingbot.backend.tools.MessageUpdater;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

import static com.elorankingbot.backend.timedtask.DurationParser.minutesToString;

@ModCommand
public class Ban extends SlashCommand {

	private User playerUser;
	private Player player;
	private String reasonGiven;
	private int duration;

	public Ban(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("ban")
				.description("Ban a player for a duration, or permanently, or unban a player")
				.addOption(ApplicationCommandOptionData.builder()
						.name("mode").description("Ban a player for a duration, or permanently, or unban a player?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("ban for a duration").value("duration").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("permaban").value("permaban").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("unban").value("unban").build())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("player").description("Ban or unban this player")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("duration").description(
								"How long, if applicable. Append any of m,h,d,w to get mins, hours, days, or weeks. Default is mins")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(false)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the player")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
						.build())
				.defaultPermission(false)
				.build();
	}

	public void execute() {
		playerUser = event.getOption("player").get().getValue().get().asUser().block();
		if (playerUser.isBot()) {
			event.reply("Bots cannot be banned.").subscribe();
			return;
		}

		service.addNewPlayerIfPlayerNotPresent(guildId, playerUser.getId().asLong());
		player = service.findPlayerByGuildIdAndUserId(guildId, playerUser.getId().asLong()).get();
		reasonGiven = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";

		switch (event.getOption("mode").get().getValue().get().asString()) {
			case "permaban":
				permaban();
				deleteExistingOpenChallenges();
				break;
			case "duration":
				if (event.getOption("duration").isEmpty()) {
					event.reply("Please enter a duration.").subscribe();
					return;
				}
				String durationString = event.getOption("duration").get().getValue().get().getRaw();
				Optional<Integer> maybeDuration = DurationParser.parse(durationString);
				if (maybeDuration.isEmpty()) {
					event.reply("Please enter a valid duration. Examples: 90, 3h, 5d, 10w");
					return;
				}
				duration = maybeDuration.get();

				durationBan();
				deleteExistingOpenChallenges();
				break;
			case "unban":
				if (!player.isBanned()) {
					event.reply("That player is not banned currently.").subscribe();
					return;
				}

				unban();
				break;
		}
		service.savePlayer(player);
	}

	private void permaban() {
		if (player.isBanned()) {
			playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(
					String.format("%s has updated your ban to be permanent, or until unbanned.%s",
							event.getInteraction().getUser().getTag(), reasonGiven)).subscribe());

			event.reply(String.format("%s's ban is updated to be permanent, or until unbanned.%s",
					playerUser.getTag(), reasonGiven)).subscribe();
		} else {
			playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(
					String.format("%s has banned you permanently, or until unbanned.%s",
							event.getInteraction().getUser().getTag(), reasonGiven)).subscribe());

			event.reply(String.format("%s is banned permanently, or until unbanned.%s",
					playerUser.getTag(), reasonGiven)).subscribe();
		}

		player.setUnbanAtTimeSlot(-1);
	}

	private void durationBan() {
		if (player.isBanned()) {
			playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(
					String.format("%s has updated your ban to end after %s, from now.%s",
							event.getInteraction().getUser().getTag(), minutesToString(duration), reasonGiven)).subscribe());

			event.reply(String.format("%s's ban has been updated to end after %s, from now.%s",
					playerUser.getTag(), minutesToString(duration), reasonGiven)).subscribe();
		} else {
			playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(
					String.format("%s has banned you for %s.%s",
							event.getInteraction().getUser().getTag(), minutesToString(duration), reasonGiven)).subscribe());

			event.reply(String.format("%s is banned for %s.%s",
					playerUser.getTag(), minutesToString(duration), reasonGiven)).subscribe();
		}

		player.setUnbanAtTimeSlot((queue.getCurrentIndex() + duration) % queue.getNumberOfTimeSlots());

		queue.addTimedTask(TimedTask.TimedTaskType.PLAYER_UNBAN, duration,
				guildId, player.getUserId(), null);
	}

	private void unban() {
		playerUser.getPrivateChannel().subscribe(channel -> channel.createMessage(
				String.format("%s has lifted your ban.%s",
						event.getInteraction().getUser().getTag(), reasonGiven)).subscribe());

		event.reply(String.format("%s has been unbanned.%s",
				playerUser.getTag(), reasonGiven)).subscribe();

		player.setUnbanAtTimeSlot(-2);
	}

	private void deleteExistingOpenChallenges() {
		service.findAllChallengesByGuildIdAndPlayerId(guildId, player.getUserId()).stream()
				.forEach(challenge -> {
					if (challenge.isAccepted()) return;

					service.deleteChallenge(challenge);

					boolean isChallengerBanned = challenge.getChallengerId() == player.getUserId();
					bot.getChallengerMessage(challenge).subscribe(message ->
							new MessageUpdater(message)
									.makeAllNotBold()
									.addLine(isChallengerBanned ?
											"You have been banned. The challenge is canceled."
											: "Your opponent has been banned. The challenge is canceled.")
									.makeAllItalic()
									.resend()
									.withComponents(none)
									.subscribe(msg -> queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
											msg.getId().asLong(), challenge.getChallengerChannelId(), null)));
					bot.getAcceptorMessage(challenge).subscribe(message ->
							new MessageUpdater(message)
									.makeAllNotBold()
									.addLine(isChallengerBanned ?
											"Your opponent has been banned. The challenge is canceled."
											: "You have been banned. The challenge is canceled.")
									.makeAllItalic()
									.resend()
									.withComponents(none)
									.subscribe(msg -> queue.addTimedTask(TimedTask.TimedTaskType.MESSAGE_DELETE, game.getMessageCleanupTime(),
											msg.getId().asLong(), challenge.getAcceptorChannelId(), null)));
				});
	}
}
