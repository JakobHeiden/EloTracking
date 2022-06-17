package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.PlayerCommand;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.commands.SlashCommand;
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
	public static final String customId = Help.class.getSimpleName().toLowerCase();

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.services = services;
		this.commandClassScanner = services.commandClassScanner;
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(Help.class.getSimpleName().toLowerCase())
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
		event.reply().withEmbeds(createHelpEmbed(services, topic))// TODO! wo kommen dann die anderen topics her? f[r Revert Match anpassen
				.withComponents(createConceptsActionRow(), createPlayerCommandsActionRow(), createModCommandsActionRow(),
						createAdminCommandsActionRow())
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
				embedText = "Please refer to these tutorials to get started:\n" +
						"[Tutorial: Basic bot setup](https://www.youtube.com/watch?v=rq8kD-mQujI)\n" +
						"[Tutorial: Join a queue, report a match](https://www.youtube.com/watch?v=u6VzIFM8md8)";
			}
			case "Command List" -> {
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
						"Help on " + MessageCommand.getCommandName(commandClass)
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

	private ActionRow createConceptsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>();
		menuOptions.add(SelectMenu.Option.of("General Help", "General Help"));
		menuOptions.add(SelectMenu.Option.of("Command List", "Command List"));
		menuOptions.add(SelectMenu.Option.of("Concept: Rankings and Queues", "Concept: Rankings and Queues"));
		menuOptions.add(SelectMenu.Option.of("Concept: Matchmaking, Rating Spread, Rating Elasticity",
				"Concept: Matchmaking, Rating Spread, Rating Elasticity"));
		return ActionRow.of(SelectMenu.of(customId + ":concepts", menuOptions).withPlaceholder("General Help, Command List, and Concepts"));
	}

	private ActionRow createPlayerCommandsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getPlayerCommandClassNames().stream()
				.map(this::createSelectMenuOption).toList());
		return ActionRow.of(SelectMenu.of(customId + ":playercommands", menuOptions).withPlaceholder("Player Commands"));
	}

	private ActionRow createModCommandsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getModCommandClassNames().stream()
				.map(this::createSelectMenuOption).toList());
		return ActionRow.of(SelectMenu.of(customId + ":modcommands", menuOptions).withPlaceholder("Moderator Commands"));
	}

	private ActionRow createAdminCommandsActionRow() {
		List<SelectMenu.Option> menuOptions = new ArrayList<>(commandClassScanner.getAdminCommandClassNames().stream()
				.map(this::createSelectMenuOption).toList());
		return ActionRow.of(SelectMenu.of(customId + ":admincommands", menuOptions).withPlaceholder("Admin Commands"));
	}

	private SelectMenu.Option createSelectMenuOption(String commandClassName) {
		Class commandClass = classForName(commandClassScanner, commandClassName);
		String selectMenuOptionLabel = MessageCommand.class.isAssignableFrom(commandClass) ?
				MessageCommand.getCommandName(commandClass)
				: "/" + commandClassName.toLowerCase();
		return SelectMenu.Option.of(selectMenuOptionLabel, commandClassName.toLowerCase());
	}

	private static Class classForName(CommandClassScanner commandClassScanner, String commandClassName) {
		try {
			return Class.forName(commandClassScanner.getFullClassName(commandClassName));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
