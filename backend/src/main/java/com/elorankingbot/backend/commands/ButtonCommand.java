package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.service.DBService;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class ButtonCommand {// TODO von Command erben, mit @ModCommand etc versehen, @NoHelp bauen damit Help
	// es nicht anzeigt

	protected final ButtonInteractionEvent event;
	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue timedTaskQueue;
	protected final GatewayDiscordClient client;
	protected final MatchService matchService;

	protected static final List none = new ArrayList<>();

	protected ButtonCommand(ButtonInteractionEvent event, Services services) {
		this.event = event;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.timedTaskQueue = services.timedTaskQueue;
		this.client = services.client;
		this.matchService = services.matchService;
	}

	public void doExecute() {
		execute();
	}

	public abstract void execute();// TODO! logging wie in slashcommand. aber nur fuer related to dispute... mal schauen wie man das baut
}
