package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Set;

@AdminCommand
public class SetPermissions extends SlashCommand {

	private Set<String> adminCommands, modCommands;
	private Role adminRole, modRole;

	public SetPermissions(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.adminCommands = services.commandClassScanner.getAdminCommandClassNames();
		this.modCommands = services.commandClassScanner.getModCommandClassNames();
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(SetPermissions.class.getSimpleName().toLowerCase())
				.description(getShortDescription())
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("adminormod").description("Link admin or moderator permissions to a role?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("admin").value("admin").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("moderator").value("moderator").build())
						.required(true)
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("role").description("Link elo admin or moderator permissions to this role")
						.type(ApplicationCommandOption.Type.ROLE.getValue())
						.required(true).build())
				.build();
	}

	public static String getShortDescription() {
		return "Link elo permissions to a role.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `adminormod` Whether to set the admin role, or the moderator role.\n" +
				"`Required:` `role` The role to link permissions to.\n" +
				"Moderator commands are commands related to the day-to-day operation of the bot and include things like " +
				"dispute resolution, bans, and player rating manipulation.\n" +
				"Admin commands are those that alter the settings of the bot. Admin permissions include moderator permissions. " +
				"Deleting the admin role will make /setrole available to @everyone, so you cannot lock yourself out of " +
				"admin permissions.";
	}

	protected void execute() {
		String adminOrMod = event.getOption("adminormod").get().getValue().get().asString();
		String nameOfRole = null;
		if (adminOrMod.equals("admin")) {
			adminRole = event.getOption("role").get().getValue().get().asRole()
					.block();
			server.setAdminRoleId(adminRole.getId().asLong());
			adminCommands.forEach(commandName -> bot.setPermissionsForAdminCommand(server, commandName));
			modCommands.forEach(commandName -> bot.setPermissionsForModCommand(server, commandName));
			nameOfRole = adminRole.getName();
		}
		if (adminOrMod.equals("moderator")) {
			modRole = event.getOption("role").get().getValue().get().asRole()
					.block();
			server.setModRoleId(modRole.getId().asLong());
			modCommands.forEach(commandName -> bot.setPermissionsForModCommand(server, commandName));
			nameOfRole = modRole.getName();
		}
		dbService.saveServer(server);
		
		bot.deployCommand(server, CreateRanking.getRequest()).subscribe();

		event.reply(String.format("Linked %s permissions to %s. This may take a minute to update on the server.\n" +
						"I added the `/%s` command to this server.",
				adminOrMod, nameOfRole, CreateRanking.class.getSimpleName().toLowerCase())).subscribe();
	}
}
