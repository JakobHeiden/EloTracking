package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.*;

@AdminCommand
public class AddChallenge extends SlashCommand {

	public AddChallenge(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		ImmutableApplicationCommandRequest.Builder requestBuilder = ApplicationCommandRequest.builder()
				.name("addchallenge")
				.description("Add a challenge option to a game")
				.defaultPermission(false);

		if (server.getGames().size() > 1) {
			ImmutableApplicationCommandOptionData.Builder gameOptionBuilder =
					ApplicationCommandOptionData.builder()
					.name("game").description("Which game to add a challenge option to?")
					.type(ApplicationCommandOption.Type.STRING.getValue())
					.required(true);
			server.getGames().keySet().forEach(nameOfGame -> gameOptionBuilder
					.addChoice(ApplicationCommandOptionChoiceData.builder()
							.name(nameOfGame).value(nameOfGame).build()));
		}



		/*
				.addOption(gameOptionBuilder.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("numberofplayers").description("How many players per team? Set to 1 if not a team game")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("numberofteams").description("How many teams per match? Use 2 for (team) versus, or 3 or higher for free-for-all")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("allowdraw").description("Allow draw results and not just win or lose?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("allow draws").value("allowdraw").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("no draws").value("nodraw").build())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("nameofqueue").description("What do you call this queue?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("queuetype").description("Only if a team queue: is this a solo queue, or a premade team only queue, or a mixed queue?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("solo queue").value("solo").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("premade only").value("premade").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("mixed queue").value("mixed").build())
						.required(false).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("maxpremade").description("Only if a mixed queue: what is the maximum premade team size?")
						.type(ApplicationCommandOption.Type.INTEGER.getValue())
						.required(false).build())
				.build();

		 */
		return  null;
	}

	public void execute() {



		// request
		// guards
		// objekt bauen, speichern
		// commands deployen
		// event reply

	}
}
