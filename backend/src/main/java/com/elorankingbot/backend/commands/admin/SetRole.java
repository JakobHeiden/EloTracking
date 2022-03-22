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
public class SetRole extends SlashCommand {

	private Set<String> adminCommands, modCommands;
	private Role adminRole, modRole;

	public SetRole(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.adminCommands = dbService.getAdminCommands();
		this.modCommands = dbService.getModCommands();
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("setrole")
				.description("Link elo permissions to a role")
				.defaultPermission(false)
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

	public void execute() {
		String adminOrMod = event.getOption("adminormod").get().getValue().get().asString();
		String nameOfRole;
		if (adminOrMod.equals("admin")) {
			adminRole = event.getOption("role").get().getValue().get().asRole()
					.block();
			server.setAdminRoleId(adminRole.getId().asLong());
			adminCommands.forEach(commandName -> bot.setAdminPermissionToAdminCommand(server, commandName));
			modCommands.forEach(commandName -> bot.setAdminAndModPermissionsToModCommand(server, commandName));
			nameOfRole = adminRole.getName();
		} else {
			modRole = event.getOption("role").get().getValue().get().asRole()
					.block();
			server.setModRoleId(modRole.getId().asLong());
			modCommands.forEach(commandName -> bot.setAdminAndModPermissionsToModCommand(server, commandName));
			nameOfRole = modRole.getName();
		}
		dbService.saveServer(server);

		event.reply(String.format("Linked %s permissions to %s. This may take a minute to update on the server.",
				adminOrMod, nameOfRole)).subscribe();
	}
}