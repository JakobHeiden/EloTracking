package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.PlayerCommand;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@PlayerCommand
public class Help extends SlashCommand {

	private final Services services;
	private final CommandClassScanner commandClassScanner;
	public static final String customId = "help";

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.services = services;
		this.commandClassScanner = services.commandClassScanner;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("help")
				.description(getShortDescription())
				.defaultPermission(true)
				.build();
	}

	public static String getShortDescription() {
		return "Get help on how to use the bot.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"The command will display some general help, and a menu to to display help on selected topics, and every bot command.";
	}

	protected void execute() {
		String topic = "General Help";
		event.reply().withEmbeds(createHelpEmbed(services, topic))
				.withComponents(createActionRow())
				.block();
	}

	public static void executeSelectMenuSelection(Services services, SelectMenuInteractionEvent event) {
		event.getMessage().get().edit().withEmbeds(createHelpEmbed(services, event.getValues().get(0))).subscribe();
		event.deferEdit().subscribe();
	}

	private static EmbedCreateSpec createHelpEmbed(Services services, String topic) {
		DiscordBotService bot = services.bot;
		CommandClassScanner commandClassScanner = services.commandClassScanner;
		String embedTitle;
		String embedText = "";
		switch (topic) {
			case "General Help" -> {
				embedTitle = topic;
				embedText = "*work in progress*";//TODO!
			}
			case "Command List" -> {
				embedTitle = topic;
				String shortDescription = null;
				embedText = "Not all commands will be present on the server at all times. For example, all moderator " +
						"commands will be absent until the moderator role is set with `/setrole`.\n";
				for (String commandClassName : commandClassScanner.getAllCommandClassNames()) {
					try {
						shortDescription = (String) Class.forName(commandClassScanner.getCommandStringToClassName()
										.get(commandClassName.toLowerCase()))
								.getMethod("getShortDescription").invoke(null);
					} catch (ReflectiveOperationException e) {
						bot.sendToOwner("Reflection error in Help");
						e.printStackTrace();
					}
					embedText += String.format("`/%s` %s\n", commandClassName.toLowerCase(), shortDescription);
				}
			}
			case "Concept: Rankings and Queues" -> {
				embedTitle = topic;
				embedText = "One server can have several rankings. Each ranking can have several queues.\n" +
						"A ranking can be a game, or it you can have several rankings for the same game.\n" +
						"For example: You could have a ranking \"Starcraft-versus\" with one queue \"1v1\", and a ranking " +
						"\"Starcraft-team\" with two queues \"2v2\" and \"3v3\". That way, 2v2 and 3v3 would " +
						"share a rating and a leaderboard, while 1v1 would be separate.";
			}
			case "Concept: Matchmaking, Rating Spread, Rating Elasticity" -> {
				embedTitle = topic;
				embedText = "Queues have a setting called `maxratingspread`. This defines the maximum rating distance " +
						"between the strongest player and the weakest player in a match.\n" +
						"There is also another setting called `ratingelasticity`, given in ratings points per minute, " +
						"which defines how fast (if at all) the matchmaker will consider matches that violate " +
						"`maxratingspread`.\n" +
						"Use `/edit` to change these settings. \n" +
						"The default for `matchratingspread` is NO_LIMIT, which turns the feature off.\n" +
						"The default for `ratingelasticity` is 100 points per minute.\n" +
						"`ratingelasticity` is applied in fractions, not only each full minute.\n";
			}
			default -> {
				embedTitle = "Help on /" + topic;
				String commandClassName = commandClassScanner.getCommandStringToClassName().get(topic.toLowerCase());
				try {
					embedText = (String) Class.forName(commandClassName)
							.getMethod("getLongDescription").invoke(null);
				} catch (ReflectiveOperationException e) {
					bot.sendToOwner("Reflection error in Help");
					e.printStackTrace();
				}
			}
		}

		return EmbedCreateSpec.builder()
				.addField(EmbedCreateFields.Field.of(embedTitle, embedText, true))
				.build();
	}

	private ActionRow createActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getAllCommandClassNames().stream()
				.map(commandClassName -> SelectMenu.Option.of("/" + commandClassName, commandClassName)).toList());
		menuOptions.add(0, SelectMenu.Option.of("Concept: Matchmaking, Rating Spread, Rating Elasticity",
				"Concept: Matchmaking, Rating Spread, Rating Elasticity"));
		menuOptions.add(0, SelectMenu.Option.of("Concept: Rankings and Queues", "Concept: Rankings and Queues"));
		menuOptions.add(0, SelectMenu.Option.of("Command List", "Command List"));
		menuOptions.add(0, SelectMenu.Option.of("General Help", "General Help"));
		return ActionRow.of(SelectMenu.of(customId, menuOptions));
	}
}
