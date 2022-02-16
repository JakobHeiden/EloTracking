package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;

@AdminCommand
public class Reset extends SlashCommand {

	private Guild guild;
	private final TextChannel channel;

	public Reset(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);

		this.channel = (TextChannel) event.getInteraction().getChannel().block();
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("reset")
				.description("Reset all elo data on the server, and optionally do a full reset of the bot")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("factoryreset").description("Should I do a full reset of the bot?")
						.type(ApplicationCommandOption.Type.BOOLEAN.getValue()).required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("areyousure").description(
								"This is not reversible. Are you completely sure? Enter the name of the game to proceed.")
						.type(ApplicationCommandOption.Type.STRING.getValue()).required(true)
						.build())
				.build();
	}

	public void execute() {
		/*if (game == null) {
			cleanCorruptState();
			return;
		}

		String entered = event.getOption("areyousure").get().getValue().get().asString();
		if (!entered.equals(game.getName())) {
			event.reply(String.format("Aborting. The name of the game is \"%s\". You entered \"%s\".",
					game.getName(), entered)).subscribe();
			return;
		}

		guild = client.getGuildById(Snowflake.of(game.getGuildId())).block();


		if (event.getOption("factoryreset").get().getValue().get().asBoolean()) {
			event.reply("Deleting match data.\n" +
					"Deleting challenge data.\n" +
					"Deleting player data.\n" +
					"Deleting game data.").subscribe();// TODO createFollowup Done! wenn subscribes durch sind
			service.deleteAllDataForGame(guildId);
			try {
				deleteResultChannel();
				deleteLeaderBoardChannel();
				deleteDisputeCategory();
			} catch (ClientException ignored) {}
			resetGuildCommands();
		} else {
			event.reply("Resetting all player ratings.").subscribe();
			service.resetAllPlayerRatings(guildId);
		}
	}

	private void deleteResultChannel() {
		guild.getChannelById(Snowflake.of(game.getResultChannelId())).subscribe(
				guildChannel -> guildChannel.delete().subscribe());
		channel.createMessage("Deleting result channel.").subscribe();
	}

	private void deleteLeaderBoardChannel() {
		guild.getChannelById(Snowflake.of(game.getLeaderboardChannelId())).subscribe(
				guildChannel -> guildChannel.delete().subscribe());
		channel.createMessage("Deleting leaderboard channel.").subscribe();
	}

	private void deleteDisputeCategory() {
		guild.getChannelById(Snowflake.of(game.getDisputeCategoryId())).subscribe(
				category -> category.delete().subscribe());
		channel.createMessage("Deleting dispute channel category.").subscribe();
	}

	private void resetGuildCommands() {
		bot.deleteAllGuildCommands(guildId).blockLast();
		bot.deployCommand(guildId, Setup.getRequest()).subscribe();
		channel.createMessage("Resetting server commands. This may take a minute to update on the server.").subscribe();
	}

	private void cleanCorruptState() {
		event.reply("Error: Cannot find game in database. Something went wrong. " +
				"I will reset the server as best I can. " +
				"You may have to remove the result channel and the dispute category manually.").subscribe();
		resetGuildCommands();
	}

		 */
	}
}
