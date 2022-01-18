package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Arrays;

public class Permission extends SlashCommand {

	static String[] adminCommands = {"reset", "permission"};
	static String[] modCommands = {"forcematch"};

	private Role adminRole;
	private Role modRole;

	public Permission(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("permission")
				.description("Link elo permissions to a role")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("admin").description("Link elo admin permissions to a role")
						.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
						.addOption(ApplicationCommandOptionData.builder()
								.name("role").description("Link elo admin permissions to this role")
								.type(ApplicationCommandOption.Type.ROLE.getValue())
								.required(true).build())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("moderator").description("Link elo moderator permissions to a role")
						.type(1)
						.addOption(ApplicationCommandOptionData.builder()
								.name("role").description("Link elo moderator permissions to this role")
								.type(ApplicationCommandOption.Type.ROLE.getValue())
								.required(true).build())
						.build())
				.build();
	}

	public void execute() {
		String adminOrMod, nameOfRole;
		if (event.getOption("admin").isPresent()) {
			adminRole = event.getOption("admin").get().getOption("role").get().getValue().get().asRole()
					.block();
			game.setAdminRoleId(adminRole.getId().asLong());
			updatePermissionsForAdminAndModCommands();
			adminOrMod = "admin";
			nameOfRole = adminRole.getName();
		} else {
			modRole = event.getOption("moderator").get().getOption("role").get().getValue().get().asRole()
				.block();
			game.setModRoleId(modRole.getId().asLong());
			updatePermissionsForModCommands();
			adminOrMod = "moderator";
			nameOfRole = modRole.getName();
		}
		service.saveGame(game);

		event.reply(String.format("Linked %s permissions to %s. This may take a minute to update on the server.", adminOrMod, nameOfRole)).subscribe();
	}

	private void updatePermissionsForAdminAndModCommands() {
		modRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getModRoleId())).block();
		Arrays.stream(adminCommands).forEach(commandName ->
				bot.setDiscordCommandPermissions(guildId, commandName, adminRole));
		Arrays.stream(modCommands).forEach(commandName ->
				bot.setDiscordCommandPermissions(guildId, commandName, adminRole, modRole));
	}

	private void updatePermissionsForModCommands() {
		adminRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getAdminRoleId())).block();
		Arrays.stream(modCommands).forEach(
				commandName -> bot.setDiscordCommandPermissions(guildId, commandName, adminRole, modRole));
	}
}
