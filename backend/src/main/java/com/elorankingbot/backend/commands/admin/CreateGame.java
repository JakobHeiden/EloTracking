package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

@AdminCommand
public class CreateGame extends SlashCommand {

	public CreateGame(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("creategame")
				.description("Create a game to track elo rating for")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("nameofgame").description("What do you call this game?")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true).build())
				.build();
	}

	public void execute() {
		String nameOfGame = event.getOption("nameofgame").get().getValue().get().asString();
		Game game = new Game(guildId, nameOfGame);
		server.addGame(game);
		service.saveServer(server);

		bot.deployCommand(guildId, AddQueue.getRequest(server)).subscribe();

		event.reply(String.format("Game %s has been created. However, there is no way yet for players to find a match. " +
				"Use /addqueue or /addchallenge to either add a queue or a challenge modality to the game.",
				nameOfGame)).subscribe();
	}
}
