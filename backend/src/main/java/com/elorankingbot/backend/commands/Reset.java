package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

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
		if (game == null) {
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

		service.deleteAllDataForGameExceptGame(game);

		if (event.getOption("factoryreset").get().getValue().get().asBoolean()) {
			event.reply("Deleting matches.\n" +
					"Deleting challenges.\n" +
					"Deleting players.\n" +
					"Deleting game.").subscribe();// TODO createFollowup Done! wenn subscribes durch sind
			service.deleteGame(game);
			deleteResultChannel();
			deleteDisputeCategory();
			resetGuildCommands();
		} else {
			event.reply("Deleting matches.\n" +
					"Deleting challenges.\n" +
					"Deleting players.").subscribe();
			clearResultChannel();
		}
	}

	private void deleteResultChannel() {
		guild.getChannelById(Snowflake.of(game.getResultChannelId())).subscribe(
				guildChannel -> guildChannel.delete().subscribe());
		channel.createMessage("Deleting result channel.").subscribe();
	}

	private void deleteDisputeCategory() {
		guild.getChannelById(Snowflake.of(game.getDisputeCategoryId())).subscribe(
				category -> category.delete().subscribe());
		channel.createMessage("Deleting dispute channel category.").subscribe();
	}

	private void resetGuildCommands() {
		bot.deleteAllGuildCommands(guildId).blockLast();
		bot.deployCommandToGuild(Setup.getRequest(), guildId).subscribe();
		channel.createMessage("Resetting server commands.").subscribe();
	}

	private void clearResultChannel() {
		guild.getChannelById(Snowflake.of(game.getResultChannelId()))
				.map(channel -> (TextChannel) channel)
				.subscribe(textChannel -> textChannel.getLastMessage().subscribe(
						lastMessage -> {
							textChannel.getMessagesBefore(lastMessage.getId()).subscribe(
									message -> message.delete().subscribe());
							lastMessage.delete().subscribe();
						}));
		channel.createMessage("Deleting result messages.").subscribe();
	}

	private void cleanCorruptState() {
		event.reply("Error: Cannot find game in database. Something went wrong. " +
				"I will reset the server as best I can. " +
				"You may have to remove the result channel and the dispute category manually.").subscribe();
		resetGuildCommands();
	}
}
