package com.elorankingbot.backend.command_legacy;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@AdminCommand
public class Set extends SlashCommand {

	private static final List<String> integerVariables = List.of(
			"openchallengedecay", "acceptedchallengedecay", "matchautoresolve", "messagecleanup", "leaderboardlength");

	public Set(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("set")
				.description("Set a variable for the server")
				.defaultPermission(false)
				.addOption(ApplicationCommandOptionData.builder()
						.name("variable").description("The variable you wish to set")
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Name of the game").value("name").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Allow for draws").value("allowdraw").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Open challenges decay after").value("openchallengedecay").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Accepted challenges decay after").value("acceptedchallengedecay").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Matches auto-resolve after").value("matchautoresolve").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Messages cleanup after").value("messagecleanup").build())
						.addChoice(ApplicationCommandOptionChoiceData.builder().name("Leaderboard number of ranks displayed").value("leaderboardlength").build())
						.required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("value").description("The value the variable should take, in minutes if applicable")// TODO eingaben in stunden, tagen etc
						.type(ApplicationCommandOption.Type.STRING.getValue())
						.required(true)
						.build())
				.build();
	}

	public void execute() {
		String variable = event.getOption("variable").get().getValue().get().asString();
		String value = event.getOption("value").get().getValue().get().asString();
		if (integerVariables.contains(variable) && !value.matches("[0-9]+")) {
			event.reply("Please enter a positive integer.").subscribe();
			return;
		}
		if (variable.equals("allowdraw") &&
				!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t")
				|| value.equalsIgnoreCase("false") || value.equalsIgnoreCase("f"))) {
			event.reply("Please enter one of the following: \"true\", \"false\", \"t\", \"f\"").subscribe();
			return;
		}

		int valueAsInt = -1;
		boolean valueAsBoolean = false;
		if (integerVariables.contains(variable)) valueAsInt = Integer.parseInt(value);
		if (variable.equals("allowdraw"))
			valueAsBoolean = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("t");
		/*
		switch (variable) {
			case "name":
				game.setName(value);
				event.reply(String.format("The name of the game has been set to %s.", value)).subscribe();
				break;
			case "allowdraw":
				game.setAllowDraw(valueAsBoolean);
				event.reply(String.format("Reporting draws is now %s.", valueAsBoolean ? "possible" : "impossible")).subscribe();
				bot.deployCommand(game.getGuildId(), ForceMatch.getRequest(valueAsBoolean)).subscribe();
				break;
			case "openchallengedecay":
				game.setOpenChallengeDecayTime(valueAsInt);
				event.reply(String.format("Open challenges now decay after %s minutes.", value)).subscribe();// TODO ouputs besser formatieren
				break;
			case "acceptedchallengedecay":
				game.setAcceptedChallengeDecayTime(valueAsInt);
				event.reply(String.format("Accepted challenges now decay after %s minutes.", value)).subscribe();
				break;
			case "matchautoresolve":
				game.setMatchAutoResolveTime(valueAsInt);
				event.reply(String.format("Matches reported by only one party now resolve after %s minutes.", value)).subscribe();
				break;
			case "messagecleanup":
				game.setMessageCleanupTime(valueAsInt);
				event.reply(String.format("Messages are now getting cleaned up after %s minutes.", value)).subscribe();
				break;
			case "leaderboardlength":
				game.setLeaderboardLength(valueAsInt);
				event.reply((String.format("Leaderboard length is now %s.", valueAsInt))).subscribe();
				bot.updateLeaderboard(game);
				break;
		}
		service.saveGame(game);

		 */
	}
}
