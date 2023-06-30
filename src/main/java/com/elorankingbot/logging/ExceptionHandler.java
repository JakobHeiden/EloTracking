package com.elorankingbot.logging;

import com.elorankingbot.service.DiscordBotService;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.rest.http.client.ClientException;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.util.function.BiFunction;
import java.util.function.Consumer;

@Component
@CommonsLog
public class ExceptionHandler {

	private final DiscordBotService bot;

	public static final String supportServerInvite = "https://discord.gg/n5mFYY272t";
	public static final Consumer<Object> NO_OP = object -> {
	};

	public ExceptionHandler(Services services) {
		this.bot = services.bot;
	}

	public void handleUnspecifiedCommandException(Throwable throwable, DeferrableInteractionEvent event, String commandName) {
		handleCommandException(throwable, event, commandName, "Unspecified Error");
	}

	public void handleAsyncException(Throwable throwable, DeferrableInteractionEvent event, String commandName) {
		handleCommandException(throwable, event, commandName, "Asynchronous Error");
	}

	private void handleCommandException(Throwable throwable, DeferrableInteractionEvent event, String commandName, String errorDescription) {
		String guildString = event.getInteraction().getGuild().map(bot::guildAsString).onErrorReturn("unknown").block();
		String context = String.format("%s on %s by %s", commandName, guildString, event.getInteraction().getUser().getTag());
		handleException(throwable, context);

		String userErrorMessage = errorDescription + ": " + throwable.getMessage()
				+ "\nI sent a report to the developer."
				+ "\nIf this error persists, please join the bot support server: "
				+ supportServerInvite;
		event.reply(userErrorMessage).subscribe(NO_OP, throwable2 -> event.createFollowup(userErrorMessage).subscribe());
	}

	// this is also the entry point for exceptions occurring in QueueScheduler or TimedTaskScheduler
	public void handleException(Throwable throwable, String context) {
		String ownerErrorMessage = String.format("Error: %s:\n%s", context, throwable.getMessage());
		bot.sendToOwner(ownerErrorMessage);

		if (throwable instanceof ClientException) {
			ownerErrorMessage += "\nClientException caused by request:\n" + ((ClientException) throwable).getRequest();
		}
		log.error(ownerErrorMessage);
		throwable.printStackTrace(System.out);
	}

	// specific exception cases
	public BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory(DeferrableInteractionEvent event) {
		return (commandName, isDeploy) -> throwable -> handleCommandException(throwable, event, commandName,
				String.format("Unable to %s command %s", isDeploy ? "deploy" : "delete", commandName));
	}

	public BiFunction<String, Boolean, Consumer<Throwable>> updateCommandFailedCallbackFactory() {
		return (commandName, isDeploy) -> throwable -> handleException(throwable, commandName);
	}
}
