package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.CommandClassScanner;
import com.elorankingbot.backend.command.PlayerCommand;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.tools.EmbedBuilder;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import static discord4j.core.object.command.ApplicationCommandOption.Type.STRING;

@Slf4j
@PlayerCommand
public class Help extends SlashCommand {

	private final CommandClassScanner commandClassScanner;

	public Help(ChatInputInteractionEvent event, Services services) {
		super(event, services);
		this.commandClassScanner = services.commandClassScanner;
	}

	public static ApplicationCommandRequest getRequest(CommandClassScanner commandClassScanner) {
		return ApplicationCommandRequest.builder()
				.name("help")
				.description(getShortDescription())
				.defaultPermission(true)
				.addOption(ApplicationCommandOptionData.builder()
						.name("topic").description("Choose a topic or command")
						.type(STRING.getValue())
						.required(true)
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Command List")
								.value("Command List")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Concept: Rankings and Queues")
								.value("Concept: Rankings and Queues")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("Concept: Matchmaking, Rating Spread, Rating Elasticity")
								.value("Concept: Matchmaking, Rating Spread, Rating Elasticity")
								.build())
						.addAllChoices(commandClassScanner.getAllCommandClassNames().stream()
								.map(Help::createChoice).toList())
						.build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("displaypublic")
						.description("Display help information for you alone, or for everyone to see")
						.required(false)
						.type(STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("display in public")
								.value("is-public")
								.build())
						.addChoice(ApplicationCommandOptionChoiceData.builder()
								.name("display only for me (default)")
								.value("is-ephemeral")
								.build())
						.build())
				.build();
	}

	public static String getShortDescription() {
		return "Get a list of all commands, or get detailed information about a topic or command.";
	}

	public static String getLongDescription() {
		return getShortDescription() + "\n" +
				"`Required:` `topic` Which command or topic you want to get help on.\n" +
				"`Optional:` `displaypublic` Wether you want to display the help information just for yourself, or publicly " +
				"in the channel you're in. By default it is shown only you.";
	}

	public void execute() {
		String topic = event.getOptions().get(0).getValue().get().asString();
		boolean isEphemeralReply = event.getOptions().size() == 1
				|| event.getOptions().get(1).getValue().get().asString().equals("is-ephemeral");

		String embedTitle;
		StringBuilder embedText = new StringBuilder();
		switch (topic) {
			case "Command List" -> {
				embedTitle = topic;
				String shortDescription = null;
				for (String commandClassName : commandClassScanner.getAllCommandClassNames()) {
					try {
						shortDescription = (String) Class.forName(commandClassScanner.getCommandStringToClassName()
										.get(commandClassName.toLowerCase()))
								.getMethod("getShortDescription").invoke(null);
					} catch (ReflectiveOperationException e) {
						bot.sendToOwner("Reflection error in Help");
						e.printStackTrace();
					}
					embedText.append(String.format("`/%s` %s\n", commandClassName.toLowerCase(), shortDescription));
				}
			}
			case "Concept: Rankings and Queues" -> {
				embedTitle = topic;
				embedText = new StringBuilder("One server can have several rankings. Each ranking can have several queues.\n" +
						"A ranking can be a game, or it you can have several rankings for the same game.\n" +
						"For example: You could have a ranking \"Starcraft-versus\" with one queue \"1v1\", and a ranking " +
						"\"Starcraft-team\" with two queues \"2v2\" and \"3v3\". That way, 2v2 and 3v3 would " +
						"share a rating and a leaderboard, while 1v1 would be separate.");
			}
			case "Concept: Matchmaking, Rating Spread, Rating Elasticity" -> {
				embedTitle = topic;
				embedText = new StringBuilder("Queues have a setting called `maxratingspread`. This defines the maximum rating distance " +
						"between the strongest player and the weakest player in a match.\n" +
						"There is also another setting called `ratingelasticity`, given in ratings points per minute, " +
						"which defines how fast (if at all) the matchmaker will consider matches that violate " +
						"`maxratingspread`.\n" +
						"Use `\\edit` to change these settings. \n" +
						"The default for `matchratingspread` is NO_LIMIT, which turns the feature off.\n" +
						"The default for `ratingelasticity` is 100 points per minute.\n" +
						"`ratingelasticity` is applied in fractions, not only each full minute.\n");
			}
			default -> {
				embedTitle = "Help on " + topic;
				String commandClassName = commandClassScanner.getCommandStringToClassName().get(topic.substring(1));
				try {
					embedText = new StringBuilder((String) Class.forName(commandClassName)
							.getMethod("getLongDescription").invoke(null));
				} catch (ReflectiveOperationException e) {
					bot.sendToOwner("Reflection error in Help");
					e.printStackTrace();
				}
			}
		}

		event.reply().withEmbeds(EmbedBuilder.createHelpEmbed(embedTitle, embedText.toString()))
				.withEphemeral(isEphemeralReply).subscribe();
	}

	private static ApplicationCommandOptionChoiceData createChoice(String commandClassName) {
		String discordCommandName = "/" + commandClassName.toLowerCase();
		return ApplicationCommandOptionChoiceData.builder()
				.name(discordCommandName)
				.value(discordCommandName)
				.build();
	}
}
