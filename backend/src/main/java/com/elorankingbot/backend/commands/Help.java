package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

public class Help extends SlashCommand {

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("help")
				.description("Get a list of all commands, or get detailed information about a topic or command")
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("topic").description("Choose a topic or command")
						.type(STRING.getValue())
						.required(true)
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Command List")
								.value("command-list")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Concept: Rankings and Queues")
								.value("concept-rankings")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Concept: Matchmaking, Rating Spread, Rating Elasticity")
								.value("concept-matchmaking")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/help")
								.value("/help")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/setrole")
								.value("/setrole")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/createranking")
								.value("/createranking")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/addqueue")
								.value("/addqueue")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/edit")
								.value("/edit")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/join")
								.value("/join")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("/leave")
								.value("/leave")
								.build())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("displaypublic")
						.description("Display help information for you alone, or for everyone to see")
						.required(false)
						.type(STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("display in public")
								.value("is-public")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("display only for me (default)")
								.value("is-not-public")
								.build())
						.build())
				.build();
	}

	public void execute() {
		String topic = event.getOptions().get(0).getValue().get().asString();

		switch (topic) {
			case "command-list" -> {

			}
		}

	}
}
