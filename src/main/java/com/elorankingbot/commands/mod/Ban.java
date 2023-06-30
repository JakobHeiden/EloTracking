package com.elorankingbot.commands.mod;

import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.command.annotations.ModCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Player;
import com.elorankingbot.service.Services;
import com.elorankingbot.timedtask.DurationParser;
import com.elorankingbot.timedtask.TimedTask;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Optional;

import static com.elorankingbot.timedtask.DurationParser.minutesToString;

@ModCommand
@GlobalCommand
public class Ban extends SlashCommand {

	private User playerUser;
	private Player player;
	private String reasonGiven;
	private int duration;

	public Ban(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("ban")
				.description(getShortDescription())
				.addOption(ApplicationCommandOptionData.builder()
						.name("mode").description("Ban for a duration, or permanently, or unban?")
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
						.name("player").description("Choose a player")
						.type(ApplicationCommandOption.Type.USER.getValue())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("duration").description(
								"How long, if applicable. Append any of m,h,d,w to get mins, hours, days, or weeks. Default is mins")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("reason").description("Give a reason. This will be relayed to the player")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(false)
						.build())
				.defaultPermission(true)
				.build();
	}

	public static String getShortDescription() {
		return "Ban a player for a duration, or permanently, or unban a player";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `mode` Whether to ban the player for a duration, or permanently, or to lift an existing ban " +
				"on a player.\n" +
				"`Required:` `player` Which player to ban or unban.\n" +
				"`Optional:` `duration` For how long to ban the player. This option is technically optional, but the " +
				"command will not work if `mode` = `duration` and this option is not set. " +
				"Durations are in minutes by default, but can be entered in hours, days, or weeks, by adding the " +
				"letters h, d, or w to the duration.\n" +
				"`Optional:` `reason` Give a reason for the ban. This will be included in the ban message sent to the player.\n";
	}

	protected void execute() {
		playerUser = event.getOption("player").get().getValue().get().asUser().block();
		if (playerUser.isBot()) {
			event.reply("Bots cannot be banned.").subscribe();
			return;
		}

		dbService.getPlayerOrGenerateIfNotPresent(guildId, playerUser);
		player = dbService.findPlayerByGuildIdAndUserId(guildId, playerUser.getId().asLong()).get();
		reasonGiven = event.getOption("reason").isPresent() ?
				String.format(" Reason given: \"%s\"", event.getOption("reason").get().getValue().get().asString())
				: "";

		String mode = event.getOption("mode").get().getValue().get().asString();
		switch (mode) {
			case "permaban" -> {
				sendPermabanMessages();
				player.setUnbanAtTimeSlot(-1);
				queueScheduler.removePlayerFromAllQueues(server, player);
			}
			case "duration" -> {
				if (event.getOption("duration").isEmpty()) {
					event.reply("Please enter a duration.").subscribe();
					return;
				}
				String durationString = event.getOption("duration").get().getValue().get().asString();
				Optional<Integer> maybeDuration = DurationParser.parse(durationString);
				if (maybeDuration.isEmpty()) {
					event.reply("Please enter a valid duration. Examples: 90, 3h, 5d, 10w").subscribe();
					return;
				}
				duration = maybeDuration.get();
				sendDurationBanMessages();
				player.setUnbanAtTimeSlot((timedTaskScheduler.getCurrentIndex() + duration) % timedTaskScheduler.getNumberOfTimeSlots());
				timedTaskScheduler.addTimedTask(TimedTask.TimedTaskType.PLAYER_UNBAN, duration,
						guildId, player.getUserId(), null);
				queueScheduler.removePlayerFromAllQueues(server, player);
			}
			case "unban" -> {
				if (!player.isBanned()) {
					event.reply("That player is not banned currently.").subscribe();
					return;
				}
				sendUnbanMessages();
				player.setUnbanAtTimeSlot(-2);
			}
		}
		dbService.savePlayer(player);
	}

	private void sendPermabanMessages() {
		if (player.isBanned()) {
			bot.sendDM(playerUser, event, String.format("%s has updated your ban to be permanent, or until unbanned.%s",
							event.getInteraction().getUser().getTag(), reasonGiven));
			event.reply(String.format("%s's ban is updated to be permanent, or until unbanned. I informed them about it.%s",
					playerUser.getTag(), reasonGiven)).subscribe();
		} else {
			bot.sendDM(playerUser, event, String.format("%s has banned you permanently, or until unbanned.%s",
							event.getInteraction().getUser().getTag(), reasonGiven));
			event.reply(String.format("%s is banned permanently, or until unbanned. I informed them about it.%s",
					playerUser.getTag(), reasonGiven)).subscribe();
		}
	}

	private void sendDurationBanMessages() {
		if (player.isBanned()) {
			bot.sendDM(playerUser, event, String.format("%s has updated your ban to end after %s, from now.%s",
							event.getInteraction().getUser().getTag(), minutesToString(duration), reasonGiven));
			event.reply(String.format("%s's ban has been updated to end after %s, from now. I informed them about it.%s",
					playerUser.getTag(), minutesToString(duration), reasonGiven)).subscribe();
		} else {
			bot.sendDM(playerUser, event, String.format("%s has banned you for %s.%s",
							event.getInteraction().getUser().getTag(), minutesToString(duration), reasonGiven));
			event.reply(String.format("%s is banned for %s. I informed them about it.%s",
					playerUser.getTag(), minutesToString(duration), reasonGiven)).subscribe();
		}
	}

	private void sendUnbanMessages() {
		bot.sendDM(playerUser, event, String.format("%s has lifted your ban.%s", event.getInteraction().getUser().getTag(), reasonGiven));
		event.reply(String.format("%s has been unbanned. I informed them about it.%s",
				playerUser.getTag(), reasonGiven)).subscribe();
	}
}
