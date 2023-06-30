package com.elorankingbot.commands.player.help;

import com.elorankingbot.command.CommandClassScanner;
import com.elorankingbot.commands.MessageCommand;
import com.elorankingbot.service.DiscordBotService;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.List;

public class HelpComponents {

	static EmbedCreateSpec createHelpEmbed(DiscordBotService bot, CommandClassScanner commandClassScanner, String topic) throws Exception {
		String embedTitle = null;
		String embedText = "";
		switch (topic) {
			case "General Help" -> {
				embedText = "Please refer to these tutorials to get started:\n" +
						"[Tutorial: Basic bot setup](https://www.youtube.com/watch?v=rq8kD-mQujI)\n" +
						"[Tutorial: Join a queue, report a match](https://www.youtube.com/watch?v=u6VzIFM8md8)";
			}
			case "Player Command List" -> {
				embedText = "Not all commands will be present on the server at all times. For example, /join " +
						"will be absent unless there is at least one ranking and one queue.\n";
				for (String playerHelpEntry : commandClassScanner.getPlayerCommandHelpEntries()) {
					Class clazz = Class.forName(commandClassScanner.getFullClassName(playerHelpEntry));
					String shortDescription = (String) clazz.getMethod("getShortDescription").invoke(null);
					embedText += String.format("`%s` %s\n", helpEntryNameOf(clazz), shortDescription);
				}
			}
			case "Moderator Command List" -> {
				embedText = "Not all commands will be present on the server at all times. For example, /forcewin " +
						"will be absent unless there is at least one ranking and one queue.\n";
				for (String moderatorHelpEntry : commandClassScanner.getModCommandHelpEntries()) {
					Class clazz = Class.forName(commandClassScanner.getFullClassName(moderatorHelpEntry));
					String shortDescription = (String) clazz.getMethod("getShortDescription").invoke(null);
					embedText += String.format("`%s` %s\n", helpEntryNameOf(clazz), shortDescription);
				}
			}
			case "Admin Command List" -> {
				embedText = "Not all commands will be present on the server at all times. For example, /addqueue " +
						"will be absent unless there is at least one ranking.\n";
				for (String adminHelpEntry : commandClassScanner.getAdminCommandHelpEntries()) {
					Class clazz = Class.forName(commandClassScanner.getFullClassName(adminHelpEntry));
						String shortDescription = (String) clazz.getMethod("getShortDescription").invoke(null);
					embedText += String.format("`%s` %s\n", helpEntryNameOf(clazz), shortDescription);
				}
			}
			case "Concept: Rankings and Queues" -> {
				embedText = "One server can have several rankings. Each ranking can have several queues.\n" +
						"A ranking can be a game, or it you can have several rankings for the same game.\n" +
						"For example: You could have a ranking \"Starcraft-versus\" with one queue \"1v1\", and a ranking " +
						"\"Starcraft-team\" with two queues \"2v2\" and \"3v3\". That way, 2v2 and 3v3 would " +
						"share a rating and a leaderboard, while 1v1 would be separate.";
			}
			case "Concept: Matchmaking, Rating Spread, Rating Elasticity" -> {
				embedText = "Queues have a setting called `max-rating-spread`. This defines the maximum rating distance " +
						"between the strongest player and the weakest player in a match.\n" +
						"There is also another setting called `rating-elasticity`, given in ratings points per minute, " +
						"which defines how fast (if at all) the matchmaker will consider matches that violate " +
						"`max-rating-spread`.\n" +
						"Use `/settings` to change these settings. \n" +
						"The default for `max-rating-spread` is NO_LIMIT, which turns the feature off.\n" +
						"The default for `rating-elasticity` is 100 points per minute.\n" +
						"`rating-elasticity` is applied in fractions, not only each full minute.\n";
			}
			default -> {
				String commandName = topic;
				Class commandClass = Class.forName(commandClassScanner.getFullClassName(commandName));
				embedTitle = "Help on " + helpEntryNameOf(commandClass);
				try {
					embedText = (String) commandClass.getMethod("getLongDescription").invoke(null);
				} catch (ReflectiveOperationException e) {
					bot.sendToOwner("Reflection error in Help");
					e.printStackTrace();
				}
			}
		}
		if (embedTitle == null) {
			embedTitle = topic;
		}

		var builder = EmbedCreateSpec.builder()
				.addField(EmbedCreateFields.Field.of(embedTitle, embedText, true));
		return builder.build();
	}

	static ActionRow createConceptsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("General Help", "General Help"));
		menuOptions.add(SelectMenu.Option.of("Concept: Rankings and Queues", "Concept: Rankings and Queues"));
		menuOptions.add(SelectMenu.Option.of("Concept: Matchmaking, Rating Spread, Rating Elasticity",
				"Concept: Matchmaking, Rating Spread, Rating Elasticity"));
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":concepts", menuOptions).withPlaceholder("General Help, Command List, and Concepts"));
	}

	static ActionRow createPlayerCommandsActionRow(CommandClassScanner commandClassScanner) throws ClassNotFoundException {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("Player Command List", "Player Command List"));
		for (String playerCommandHelpEntry : commandClassScanner.getPlayerCommandHelpEntries()) {
			menuOptions.add(createSelectMenuOption(commandClassScanner, playerCommandHelpEntry));
		}
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":playercommands", menuOptions).withPlaceholder("Player Commands"));
	}

	static ActionRow createModCommandsActionRow(CommandClassScanner commandClassScanner) throws ClassNotFoundException {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("Moderator Command List", "Moderator Command List"));
		for (String modCommandClassName : commandClassScanner.getModCommandHelpEntries()) {
			SelectMenu.Option selectMenuOption = createSelectMenuOption(commandClassScanner, modCommandClassName);
			menuOptions.add(selectMenuOption);
		}
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":modcommands", menuOptions).withPlaceholder("Moderator Commands"));
	}

	static ActionRow createAdminCommandsActionRow(CommandClassScanner commandClassScanner) throws ClassNotFoundException {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("Admin Command List", "Admin Command List"));
		for (String adminCommandClassName : commandClassScanner.getAdminCommandHelpEntries()) {
			SelectMenu.Option selectMenuOption = createSelectMenuOption(commandClassScanner, adminCommandClassName);
			menuOptions.add(selectMenuOption);
		}
		return ActionRow.of(SelectMenu.of(SelectTopic.customId + ":admincommands", menuOptions).withPlaceholder("Admin Commands"));
	}

	static SelectMenu.Option createSelectMenuOption(CommandClassScanner commandClassScanner, String commandClassName) throws ClassNotFoundException {
		Class commandClass = Class.forName(commandClassScanner.getFullClassName(commandClassName));
		return SelectMenu.Option.of(helpEntryNameOf(commandClass), commandClassName.toLowerCase());
	}

	public static String helpEntryNameOf(Class clazz) {
		if (MessageCommand.class.isAssignableFrom(clazz)) {
				String regex = "([a-z])([A-Z])";
				String replacement = "$1 $2";
				return clazz.getSimpleName().replaceAll(regex, replacement);
		} else {
			return "/" + clazz.getSimpleName().toLowerCase();
		}
	}
}
