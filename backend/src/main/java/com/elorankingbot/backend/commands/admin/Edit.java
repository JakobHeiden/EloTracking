package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.SUB_COMMAND;
import static discord4j.core.object.command.ApplicationCommandOption.Type.SUB_COMMAND_GROUP;

@AdminCommand
public class Edit extends SlashCommand {

	public Edit(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		List<ApplicationCommandOptionData> optionsForVariables = List.of(
				ApplicationCommandOptionData.builder()
						.name("maxratingspread")
						.description("The maximum rating difference in a match. Enter 0 for no maximum")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(false)
						.build(),
				ApplicationCommandOptionData.builder()
						.name("ratingelasticity")
						.description("Accept larger rating difference after waiting, measured in rating points per minute")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(false)
						.build());

		List<ApplicationCommandOptionData> subCommandGroupsAndSubCommands = server.getGames().stream().map(game ->
				(ApplicationCommandOptionData) ApplicationCommandOptionData.builder()
						.name(game.getName()).description(game.getName())
						.type(SUB_COMMAND_GROUP.getValue())
						.addAllOptions(game.getQueues().stream().map(queue ->
								(ApplicationCommandOptionData) ApplicationCommandOptionData.builder()
										.name(queue.getName()).description(queue.getName())
										.type(SUB_COMMAND.getValue())
										.addAllOptions(optionsForVariables)
										.build()).toList())
						.build()).toList();

		return ApplicationCommandRequest.builder()
				.name("edit")
				.description("Edit a variable for a queue")
				.defaultPermission(false)
				.addAllOptions(subCommandGroupsAndSubCommands)
				.build();
	}

	public void execute() {

	}
}
