package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class SlashCommand {

	protected final EloRankingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;
	protected final ChatInputInteractionEvent event;
	protected long guildId;
	protected Game game;

	protected static final List none = new ArrayList<>();

	protected SlashCommand(ChatInputInteractionEvent event, EloRankingService service,
						   DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;

		this.guildId = event.getInteraction().getGuildId().get().asLong();
		Optional<Game> maybeGame = service.findGameByGuildId(guildId);
		if (maybeGame.isPresent()) this.game = maybeGame.get();
	}

	public abstract void execute();
}
