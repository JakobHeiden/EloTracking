package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
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

	private final Guild guild;
	private final TextChannel channel;

	public Reset(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.needsAdminRole = true;

		this.guild = client.getGuildById(Snowflake.of(game.getGuildId())).block();
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
		if (!super.canExecute()) return;
		String entered = event.getOption("areyousure").get().getValue().get().asString();
		if (!entered.equals(game.getName())) {
			event.reply(String.format("Aborting. The name of the game is \"%s\". You entered \"%s\".",
					game.getName(), entered)).subscribe();
			return;
		}

		service.deleteAllDataForGameExceptGame(game);

		InteractionApplicationCommandCallbackReplyMono reply;
		if (event.getOption("factoryreset").get().getValue().get().asBoolean()) {
			event.reply("Deleting matches.\n" +
					"Deleting challenges.\n" +
					"Deleting players.\n" +
					"Deleting game.").subscribe();// TODO createFollowup Done! wenn subscribes durch sind
			service.deleteGame(game);
			deleteModAndAdminRoles();
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

	private void deleteModAndAdminRoles() {
		guild.getRoleById(Snowflake.of(game.getModRoleId())).subscribe(role -> role.delete().subscribe());
		guild.getRoleById(Snowflake.of(game.getAdminRoleId())).subscribe(role -> role.delete().subscribe());
		channel.createMessage("Deleting Elo Admin and Elo Moderator Roles.").subscribe();
	}

	private void deleteResultChannel() {
		guild.getChannelById(Snowflake.of(game.getResultChannelId())).subscribe(
				guildChannel -> guildChannel.delete().subscribe());
		channel.createMessage("Deleting result channel").subscribe();
	}

	private void deleteDisputeCategory() {
		guild.getChannelById(Snowflake.of(game.getDisputeCategoryId())).subscribe(
				category -> category.delete().subscribe());
		channel.createMessage("Deleting dispute channel category.").subscribe();
	}

	private void resetGuildCommands() {
		bot.deleteAllGuildCommands(guildId);
		bot.deployToGuild(Setup.getRequest(), guildId);
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
}
