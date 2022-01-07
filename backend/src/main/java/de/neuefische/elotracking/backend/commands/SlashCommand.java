package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

import java.util.Optional;

// Subclasses must start with a capital letter and have no other capital letters to be recognized by the parser
public abstract class SlashCommand {

	protected final EloTrackingService service;
	protected final DiscordBotService bot;
	protected final TimedTaskQueue queue;
	protected final GatewayDiscordClient client;
	protected final ChatInputInteractionEvent event;
	protected long guildId;
	protected Game game;
	protected boolean needsGame;
	protected boolean needsModRole;
	protected boolean needsAdminRole;

	protected SlashCommand(ChatInputInteractionEvent event, EloTrackingService service,
						   DiscordBotService bot, TimedTaskQueue queue, GatewayDiscordClient client) {
		this.event = event;
		this.service = service;
		this.bot = bot;
		this.queue = queue;
		this.client = client;
		this.needsGame = false;
		this.needsModRole = false;
		this.needsAdminRole = false;

		this.guildId = event.getInteraction().getGuildId().get().asLong();

		Optional<Game> maybeGame = service.findGameByGuildId(guildId);
		if (maybeGame.isPresent()) this.game = maybeGame.get();
	}

	public abstract void execute();

	protected boolean canExecute() {
		if (needsGame) {
			if (game == null) {
				event.reply("Please run /setup first.").subscribe();
				return false;
			}
		}
		if (needsModRole) {
			if (!event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getModRoleId()))
					&& !event.getInteraction().getMember().get().getRoleIds().contains(Snowflake.of(game.getAdminRoleId()))) {
				event.reply("You need the Elo Moderator role to use that command").subscribe();
				return false;
			}
		}
		if (needsAdminRole) {
			if (!event.getInteraction().getMember().get().getRoleIds()
					.contains(Snowflake.of(game.getAdminRoleId()))) {
				event.reply("You need the Elo Admin role to use that command").subscribe();
				return false;
			}
		}

		return true;
	}
}
