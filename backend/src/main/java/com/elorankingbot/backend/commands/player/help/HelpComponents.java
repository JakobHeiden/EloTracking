package com.elorankingbot.backend.commands.player.help;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

public class HelpComponents {

	static EmbedCreateSpec createHelpEmbed(Services services, String topic) {
		DiscordBotService bot = services.bot;
		CommandClassScanner commandClassScanner = services.commandClassScanner;
		String embedTitle;
		String embedText = "";
		switch (topic) {
			case "General Help" -> {
				embedTitle = topic;
				embedText = "Please refer to these tutorials to get started:\n" +
						"[Tutorial: Basic bot setup](https://www.youtube.com/watch?v=rq8kD-mQujI)\n" +
						"[Tutorial: Join a queue, report a match](https://www.youtube.com/watch?v=u6VzIFM8md8)";
			}
			case "Command List" -> {// TODO! das hier macht irgendwas an der message zu lang. handleException klappt nicht weil im EventParser noch
				// eine einzelloeslung fuer /help steht. einzelloesung muss weg, dann die command list irgendwie angezeigt werden
				embedTitle = topic;
				String shortDescription = null;
				embedText = "Not all commands will be present on the server at all times. For example, all moderator " +
						"commands will be absent until the moderator role is set with `/setrole`.\n";
				for (String commandClassName : commandClassScanner.getAllCommandClassNames()) {
					try {
						shortDescription = (String) Class.forName(commandClassScanner.getFullClassName(commandClassName))
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
						"The default for `maxratingspread` is NO_LIMIT, which turns the feature off.\n" +
						"The default for `ratingelasticity` is 100 points per minute.\n" +
						"`ratingelasticity` is applied in fractions, not only each full minute.\n";
			}
			default -> {
				String commandName = topic;
				Class commandClass = classForName(commandClassScanner, commandName);
				embedTitle = MessageCommand.class.isAssignableFrom(commandClass) ?
						"Help on " + MessageCommand.formatCommandName(commandClass)
						: "Help on /" + commandName;
				try {
					embedText = (String) commandClass.getMethod("getLongDescription").invoke(null);
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

	static ActionRow createConceptsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("General Help", "General Help"));
		menuOptions.add(SelectMenu.Option.of("Command List", "Command List"));
		menuOptions.add(SelectMenu.Option.of("Concept: Rankings and Queues", "Concept: Rankings and Queues"));
		menuOptions.add(SelectMenu.Option.of("Concept: Matchmaking, Rating Spread, Rating Elasticity",
				"Concept: Matchmaking, Rating Spread, Rating Elasticity"));
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":concepts", menuOptions).withPlaceholder("General Help, Command List, and Concepts"));
	}

	static ActionRow createPlayerCommandsActionRow(CommandClassScanner commandClassScanner) {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getPlayerCommandHelpEntries().stream()
				.map(playerCommandClassName -> createSelectMenuOption(commandClassScanner, playerCommandClassName))
				.toList());
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":playercommands", menuOptions).withPlaceholder("Player Commands"));
	}

	static ActionRow createModCommandsActionRow(CommandClassScanner commandClassScanner) {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getModCommandHelpEntries().stream()
				.map(modCommandClassName -> createSelectMenuOption(commandClassScanner, modCommandClassName))
				.toList());
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":modcommands", menuOptions).withPlaceholder("Moderator Commands"));
	}

	static ActionRow createAdminCommandsActionRow(CommandClassScanner commandClassScanner) {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getAdminCommandHelpEntries().stream()
				.map(adminCommandClassName -> createSelectMenuOption(commandClassScanner, adminCommandClassName))
				.toList());
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":admincommands", menuOptions).withPlaceholder("Admin Commands"));
	}

	static SelectMenu.Option createSelectMenuOption(CommandClassScanner commandClassScanner, String commandClassName) {
		Class commandClass = classForName(commandClassScanner, commandClassName);
		String selectMenuOptionLabel = MessageCommand.class.isAssignableFrom(commandClass) ?
				MessageCommand.formatCommandName(commandClass)
				: "/" + commandClassName.toLowerCase();
		return SelectMenu.Option.of(selectMenuOptionLabel, commandClassName.toLowerCase());
	}

	static Class classForName(CommandClassScanner commandClassScanner, String commandClassName) {
		try {
			return Class.forName(commandClassScanner.getFullClassName(commandClassName));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
