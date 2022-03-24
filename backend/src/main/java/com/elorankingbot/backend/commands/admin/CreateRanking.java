package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import static com.elorankingbot.backend.service.DiscordBotService.isLegalDiscordName;
import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@AdminCommand
public class CreateRanking extends SlashCommand {

	public CreateRanking(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("createranking")
				.description("Create a ranking")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("nameofranking").description("What do you call this ranking?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("allowdraw").description("Allow draw results and not just win or lose?")
						.type(STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("allow draws").value("allow").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("no draws").value("nodraw").build())
						.required(true).build())
				.build();
	}


	public static String getShortDescription() {
		return "Create a new ranking on the server.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `nameofranking` The name of the new ranking.\n" +
				"`Required:` `allowdraw` Wether or not to allow draw as a result in matches for this ranking.\n" +
				"For more information on rankings, see `/help:` `Concept: Rankings and Queues`.";
	}

	public void execute() {
		String nameOfGame = event.getOption("nameofranking").get().getValue().get().asString();
		if (!isLegalDiscordName(nameOfGame)) {
			event.reply("Illegal name. Please use only letters, digits, dash, and underscore").subscribe();
			return;
		}
		boolean allowDraw = event.getOption("allowdraw").get().getValue().get().asString().equals("allow");
		Game game = new Game(server, nameOfGame, allowDraw);// TODO duplikate verhindern
		server.addGame(game);
		dbService.saveServer(server);

		bot.deployCommand(server, AddQueue.getRequest(server)).subscribe();
		bot.setAdminPermissionToAdminCommand(server, AddQueue.class.getSimpleName().toLowerCase());

		event.reply(String.format("Ranking %s has been created. However, there is no way yet for players to find a match. " +
				"Use /addqueue or /addchallenge to either add a queue or a challenge modality to the ranking.",
				nameOfGame)).subscribe();
	}
}
