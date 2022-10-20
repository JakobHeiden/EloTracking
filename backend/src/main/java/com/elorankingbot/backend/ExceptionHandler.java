package com.elorankingbot.backend;

import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.rest.http.client.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;
import java.util.function.Consumer;

@Component
@Slf4j
public class ExceptionHandler {

	private final DiscordBotService bot;

	private static final String supportServerInvite = "https://discord.com/invite/hCAJXasrhd";
	private final Consumer<Object> NO_OP = object -> {
	};

	public ExceptionHandler(Services services) {
		this.bot = services.bot;
	}

	public void handleUnexpectedCommandException(Throwable throwable, DeferrableInteractionEvent event, String commandName) {
		handleCommandException(throwable, event, commandName, "Unexpected Error");
	}

	private void handleCommandException(Throwable throwable, DeferrableInteractionEvent event, String commandName, String errorSpecificString) {
		String guildName = event.getInteraction().getGuild().map(Guild::getName).onErrorReturn("unknown").block();
		String context = String.format("%s on %s by %s", commandName, guildName, event.getInteraction().getUser().getTag());
		handleException(throwable, context);

		String userErrorMessage = errorSpecificString + ": " + throwable.getMessage()
				+ "\nI sent a report to the developer."
				+ "\nIf this error persists, please join the bot support server: "
				+ supportServerInvite;
		event.reply(userErrorMessage).subscribe(NO_OP, throwable2 -> event.createFollowup(userErrorMessage).subscribe());
	}

	// this is also the entry point for exceptions occuring in QueueScheduler or TimedTaskScheduler
	public void handleException(Throwable throwable, String context) {
		String ownerErrorMessage = String.format("Error: %s:\n%s", context, throwable.getMessage());
		bot.sendToOwner(ownerErrorMessage);

		log.error(ownerErrorMessage);
		if (throwable instanceof ClientException) {
			log.error("ClientException caused by request:\n" + ((ClientException) throwable).getRequest());
		}
		throwable.printStackTrace();
	}

	// specific exception cases
	public BiFunction<String, Boolean, Consumer<Throwable>> createUpdateCommandFailedCallbackFactory(DeferrableInteractionEvent event) {
		return (commandName, isDeploy) -> throwable -> handleCommandException(throwable, event, commandName,
				String.format("Unable to %s command %s", isDeploy ? "deploy" : "delete", commandName));
	}
}
