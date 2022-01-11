package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

import java.util.Optional;

// Subclasses must start with a capital letter and have no other capital letters to be recognized by the parser
public abstract class SlashCommand {

	protected final EloRankingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;
	protected final ChatInputInteractionEvent event;
	protected long guildId;
	protected Game game;
	protected boolean needsModRole;
	protected boolean needsAdminRole;

	protected SlashCommand(ChatInputInteractionEvent event, EloRankingService service,
						   DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
		this.needsModRole = false;
		this.needsAdminRole = false;

		this.guildId = event.getInteraction().getGuildId().get().asLong();

		Optional<Game> maybeGame = service.findGameByGuildId(guildId);
		if (maybeGame.isPresent()) this.game = maybeGame.get();
	}

	public abstract void execute();

	protected boolean canExecute() {
		if (needsModRole) {
			if (!event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId()))
					&& !event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))) {
				event.reply("You need the Elo Moderator role to use that command").subscribe();
				bot.sendToOwner(String.format("%s has accessed a mod command without permission on guild %s",
						event.getInteraction().getUser().getTag(), guildId));
				return false;
			}
		}
		if (needsAdminRole) {
			if (!event.getInteraction().getMember().get().getRoleIds()
					.contains(Snowflake.of(game.getAdminRoleId()))) {
				event.reply("You need the Elo Admin role to use that command").subscribe();
				bot.sendToOwner(String.format("%s has accessed an admin command without permission on guild %s",
						event.getInteraction().getUser().getTag(), guildId));
				return false;
			}
		}

		return true;
	}
}
