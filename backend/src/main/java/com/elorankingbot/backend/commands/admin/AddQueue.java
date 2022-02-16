package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.*;

public class AddQueue extends SlashCommand {

	public AddQueue(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest(Server server) {
		ImmutableApplicationCommandOptionData.Builder gameOptionBuilder = ApplicationCommandOptionData.builder()
				.name("game").description("Which game to add a queue to?")
				.type(ApplicationCommandOption.Type.STRING.getValue())
				.required(true);
		server.getGames().keySet().forEach(nameOfGame -> gameOptionBuilder
				.addChoice(ApplicationCommandOptionChoiceData.builder()
						.name(nameOfGame).value(nameOfGame).build()));

		return ApplicationCommandRequest.builder()
				.name("addqueue")
				.description("Add a queue to a game")
				.defaultPermission(false)
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
	}

	public void execute() {
		int playersPerTeam = (int) event.getOption("numberofplayers").get().getValue().get().asLong();
		if (playersPerTeam < 1) {
			event.reply("Cannot create a game with less than 1 player per team").subscribe();
			return;
		}
		int numberOfTeams = (int) event.getOption("numberofteams").get().getValue().get().asLong();
		if (numberOfTeams < 2) {
			event.reply("Cannot create a game with less than 2 teams per match").subscribe();
			return;
		}
		Game game = server.getGames().get(event.getOption("game").get().getValue().get().asString());
		String nameOfQueue = event.getOption("nameofqueue").get().getValue().get().asString();
		if (game.getMatchFindModalities().containsKey(nameOfQueue)) {
			event.reply("A queue of that name already exists for that game. " +
					"Queue names must be unique for each game").subscribe();
			return;
		}
		MatchFinderQueue.QueueType queueType;
		int maxPremadeSize = 0;
		if (playersPerTeam == 1) {
			queueType = NOT_A_TEAM_QUEUE;
		} else {
			if (event.getOption("queuetype").isEmpty()) {
				event.reply("Please select an option for solo queue/premade queue/mixed queue " +
						"when creating a team queue").subscribe();
				return;
			}
			String queueTypeOption = event.getOption("queuetype").get().getValue().get().asString();
			if (queueTypeOption.equals("solo")) {
				queueType = SOLO;
			} else if (queueTypeOption.equals("premade")) {
				queueType = PREMADE;
			} else {
				if (event.getOption("maxpremade").isEmpty()) {
					event.reply("Please select a maximum premade team size when creating a mixed " +
							"solo and premade queue").subscribe();
					return;
				} else {
					queueType = MIXED;
					maxPremadeSize = (int) event.getOption("maxpremade").get().getValue().get().asDouble();
				}
			}
		}

		boolean allowDraw = event.getOption("allowdraw").get().getValue().get().asBoolean();
		MatchFinderQueue queue = new MatchFinderQueue(game, nameOfQueue, allowDraw, numberOfTeams, playersPerTeam,
				queueType, maxPremadeSize);
		game.addMatchFinderModality(queue);
		service.saveServer(server);


		// TODO deploy /queue command
		// user aufklaeren
	}
}
