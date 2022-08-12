package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class Command {// TODO koennen die abstrakten zwischenklassen weg? die scheinen nichts zu machen
	// die commands werden unterschiedlich gebaut, insbesondere der CommandClassName wird unterschiedlich gefolgert, das reicht aber nicht als grund

	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final GatewayDiscordClient client;
	protected final MatchService matchService;
	protected final QueueService queueService;
	protected final TimedTaskQueue timedTaskQueue;
	protected final DeferrableInteractionEvent event;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;
	protected final long activeUserId;

	protected static final List none = new ArrayList<>();

	protected Command(DeferrableInteractionEvent event, Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.matchService = services.matchService;
		this.queueService = services.queueService;
		this.client = services.client;// TODO alles an bot weiterleiten
		this.timedTaskQueue = services.timedTaskQueue;
		this.event = event;
		this.guildId = event.getInteraction().getGuildId().get().asLong();
		Optional<Server> maybeServer = dbService.findServerByGuildId(guildId);
		if (maybeServer.isEmpty()) {
			String errorMessage = "Server not found";
			log.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		this.server = maybeServer.get();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
	}

	public void doExecute() {
		String executeLog = String.format("execute %s by %s on %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				event.getInteraction().getGuild().block().getName());
		log.debug(executeLog);

		// bypass permission check when admin role is not set
		// TODO checken ob admin role existiert
		if (this.getClass() == SetPermission.class && server.getAdminRoleId() == 0L) {
			execute();
			return;
		}

		List<Long> memberRoleIds = new ArrayList<>(event.getInteraction().getMember().get().getRoleIds()
				.stream().map(Snowflake::asLong).toList());
		memberRoleIds.add(Long.valueOf(guildId));
		boolean userIsAdmin = memberRoleIds.contains(server.getAdminRoleId());
		boolean userIsMod = memberRoleIds.contains(server.getModRoleId());
		if (this.getClass().isAnnotationPresent(AdminCommand.class)) {
			if (server.getAdminRoleId() == 0L) {
				event.reply("This command requires Admin permissions. The Admin role is not currently set. " +
						"Please use /setpermissions.").subscribe();
				return;
			}
			if (!userIsAdmin) {
				event.reply(String.format("This command requires the <@&%s> role.", server.getAdminRoleId()))
						.withEphemeral(true).subscribe();
				return;
			}
		}
		if (this.getClass().isAnnotationPresent(ModCommand.class)) {
			if (!userIsAdmin && server.getModRoleId() == 0L) {
				event.reply("This command requires Moderator permissions. The Moderator role is not currently set. " +
						"Please use /setpermissions.").subscribe();
				return;
			}
			if (!userIsAdmin && !userIsMod) {
				event.reply(String.format("This command requires the <@&%s> role.", server.getModRoleId()))
						.withEphemeral(true).subscribe();
				return;
			}
		}

		execute();

		log.trace(String.format("Done executing %s by %s on %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				event.getInteraction().getGuild().block().getName()));
	}

	protected void acknowledgeEvent() {
		event.deferReply().subscribe();
	}

	protected abstract void execute();
}
