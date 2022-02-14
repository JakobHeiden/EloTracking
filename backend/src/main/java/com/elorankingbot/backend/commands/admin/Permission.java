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
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Set;

@AdminCommand
public class Permission extends SlashCommand {

	private Set<String> adminCommands, modCommands;
	private Role adminRole, modRole;

	public Permission(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
					  TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.adminCommands = service.getAdminCommands();
		this.modCommands = service.getModCommands();
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
		adminCommands.forEach(commandName ->
				bot.setDiscordCommandPermissions(guildId, commandName, adminRole));
		modCommands.forEach(commandName ->
				bot.setDiscordCommandPermissions(guildId, commandName, adminRole, modRole));
	}

	private void updatePermissionsForModCommands() {
		adminRole = client.getRoleById(Snowflake.of(guildId), Snowflake.of(game.getAdminRoleId())).block();
		modCommands.forEach(
				commandName -> bot.setDiscordCommandPermissions(guildId, commandName, adminRole, modRole));
	}
}
