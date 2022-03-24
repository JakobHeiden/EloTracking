package com.elorankingbot.backend.commands.admin;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.model.MatchFinderQueue;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.ArrayList;
import java.util.List;

import static com.elorankingbot.backend.model.MatchFinderQueue.NO_LIMIT;
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
						.name(game.getName().toLowerCase()).description(game.getName())
						.type(SUB_COMMAND_GROUP.getValue())
						.addAllOptions(game.getQueues().stream().map(queue ->
								(ApplicationCommandOptionData) ApplicationCommandOptionData.builder()
										.name(queue.getName().toLowerCase())
										.description(String.format("Edit the queue %s %s",game.getName(), queue.getName()))
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

	public static String getShortDescription() {
		return "Edit settings for a queue.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Optional:` `maxratingspread` See `/help:` `Concept: Matchmaking, Rating Spread, Rating Elasticity`\n" +
				"`Optional:` `ratingelasticity` See `/help:` `Concept: Matchmaking, Rating Spread, Rating Elasticity`\n" +
				"Select one or more options to edit them.";
	}

	public void execute() {
		List<String> botReplies = new ArrayList<>();
		Game game = server.getGame(event.getOptions().get(0).getName());
		MatchFinderQueue queue = game.getQueue(event.getOptions().get(0).getOptions().get(0).getName());
		List<ApplicationCommandInteractionOption> optionsForVariables = event.getOptions().get(0).getOptions().get(0).getOptions();
		if (optionsForVariables.size() == 0) {
			event.reply("No variable has been selected. You need to select a variable to edit it.").subscribe();
			return;
		}

		botReplies.add(String.format("Editing %s %s...", game.getName(), queue.getName()));
		optionsForVariables.forEach(option -> {
			switch (option.getName()) {
				case "maxratingspread" -> {
					int maxRatingSpread = (int) option.getValue().get().asLong();
					if (maxRatingSpread < 0) {
						botReplies.add("maxratingspread cannot be smaller than 0. Variable has not been changed.");
					} else {
						queue.setMaxRatingSpread(maxRatingSpread == 0 ? NO_LIMIT : maxRatingSpread);
						botReplies.add("maxratingspread is now set to " +
								(maxRatingSpread == 0 ? "no limit" : maxRatingSpread));
					}
				}
				case "ratingelasticity" -> {
					int ratingElasticity = (int) option.getValue().get().asLong();
					if (ratingElasticity < 0) {
						botReplies.add("ratingelasticity cannot be smaller than 0. Variable has not been changed.");
					} else {
						queue.setRatingElasticity(ratingElasticity);
						botReplies.add("ratingelasticity is now set to " + ratingElasticity);
					}
				}
			}
		});
		dbService.saveServer(server);

		event.reply(String.join("\n", botReplies)).subscribe();
	}
}
