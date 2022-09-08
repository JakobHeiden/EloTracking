package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.components.FormatTools;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@AdminCommand
public class CreateRanking extends SlashCommand {

	private final List<Long> testServerIds;

	public CreateRanking(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.testServerIds = services.props.getTestServerIds();
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(CreateRanking.class.getSimpleName().toLowerCase())
				.description("Create a ranking")
				.defaultPermission(true)
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
				"`Required:` `allowdraw` Whether or not to allow draw as a result in matches for this ranking.\n" +
				"For more information on rankings, see `/help:` `Concept: Rankings and Queues`.";
	}

	protected void execute() {
		String nameOfGame = event.getOption("nameofranking").get().getValue().get().asString();
		if (!FormatTools.isLegalDiscordName(nameOfGame)) {
			event.reply(FormatTools.illegalNameMessage())
					.doOnError(super::forwardToEventParser).subscribe();
			return;
		}
		if (server.getGames().contains(new Game(server, nameOfGame, false))) {
			event.reply("A ranking of that name already exists.").subscribe();
			return;
		}

		boolean allowDraw = event.getOption("allowdraw").get().getValue().get().asString().equals("allow");
		boolean doCreateCategories = server.getDisputeCategoryId() == 0L;
		Game game = new Game(server, nameOfGame, allowDraw);// TODO duplikate verhindern
		server.addGame(game);
		try {
			channelManager.getOrCreateResultChannel(game);
			channelManager.refreshLeaderboard(game);
			channelManager.getOrCreateMatchCategory(server);
			channelManager.getOrCreateDisputeCategory(server);
			channelManager.getOrCreateArchiveCategory(server);
		} catch (ClientException e) {
			String failedRequest = "Unknown error.\\nPlease contact the developer, Ente#3460";
			if (e.getErrorResponse().get().getFields().get("message").equals("Missing Permissions")) {
				if (e.getRequest().getBody().toString().startsWith("ChannelCreateRequest")) {
					failedRequest = "Error: cannot create channel due to missing permission: Manage Channels";
				}
				if (e.getRequest().getBody().toString().startsWith("MessageCreateRequest")) {
					failedRequest = "Error: cannot create message due to missing permission: Send Messages";
				}// TODO richtig machen, generisch machen, refaktorn
			}
			event.reply(failedRequest).doOnError(super::forwardToEventParser).subscribe();
			e.printStackTrace();
			bot.sendToOwner(failedRequest);
			return;
		}
		dbService.saveServer(server);

		String updatedCommands = bot.updateGuildCommandsByRanking(server);

		event.reply(String.format("Ranking %s has been created. I also created <#%s> where I will post all match results%s" +
						"<#%s> where I put the leaderboard%s." +
						"\nHowever, there is no way yet for players to find a match. " +
						"Use `/addqueue` to either add a queue the ranking." +
						"\nThese commands have been updated: %s",
				nameOfGame,
				game.getResultChannelId(),
				doCreateCategories ? ", " : " and ",
				game.getLeaderboardChannelId(),
				doCreateCategories ? ", and channel categories for disputes and an archive" : "",
				updatedCommands))
				.doOnError(super::forwardToEventParser).subscribe();
		if (!testServerIds.contains(server.getGuildId())) {
			bot.sendToOwner(String.format("Created ranking %s on %s : %s",
					nameOfGame, guildId, event.getInteraction().getGuild().block().getName()));
		}
	}
}
