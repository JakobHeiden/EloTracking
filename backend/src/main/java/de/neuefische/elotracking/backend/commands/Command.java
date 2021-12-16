package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

// Subclasses must start with a capital letter and have no other capital letters to be recognized by the parser
// Bot replies are processed in the parser
public abstract class Command {

	protected String defaultCommandPrefix;
	protected EloTrackingService service;
	protected DiscordBotService bot;
	protected TimedTaskQueue queue;
	@Getter
	protected final Event event;
	protected long guildId;
	protected Game game;
	@Getter
	private final List<String> botReplies;

	protected Command(Event event, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		if (event instanceof ApplicationCommandInteractionEvent) {
			this.guildId = ((ApplicationCommandInteractionEvent) event).getInteraction().getGuildId().get().asLong();
		}
		this.botReplies = new LinkedList<String>();
	}

	public abstract void execute();

	protected boolean canExecute() {// TODO kann evtl weg
		boolean canExecute = true;
		Optional<Game> maybeGame = service.findGameByChannelId(guildId);
		if (maybeGame.isEmpty()) {
			Game game = new Game(guildId, "name not set");
			service.saveGame(game);
			this.game = game;
		} else {
			this.game = maybeGame.get();
		}
		return canExecute;
	}

	protected void addBotReply(String reply) {
		botReplies.add(reply);
	}

	public void sendBotReplies() {
		for (String reply : botReplies) {
			bot.sendToAdmin(reply);
		}
	}
}
