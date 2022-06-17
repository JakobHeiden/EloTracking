package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.admin.SetPermissions;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Command {

	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final GatewayDiscordClient client;
	protected final MatchService matchService;
	protected final QueueService queueService;
	protected final TimedTaskQueue timedTaskQueue;
	protected final ApplicationCommandInteractionEvent event;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;
	protected final long activeUserId;

	protected static final List none = new ArrayList<>();

	protected Command(ApplicationCommandInteractionEvent event, Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.matchService = services.matchService;
		this.queueService = services.queueService;
		this.client = services.client;// TODO alles an bot weiterleiten
		this.timedTaskQueue = services.timedTaskQueue;
		this.event = event;
		this.guildId = event.getInteraction().getGuildId().get().asLong();
		this.server = dbService.findServerByGuildId(guildId).get();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
	}

	public void doExecute() {
		log.debug(String.format("execute %s by %s on %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				event.getInteraction().getGuild().block().getName()));

		// bypass permission check when admin role is not set
		// TODO checken ob admin role existiert
		if (this.getClass() == SetPermissions.class && server.getAdminRoleId() == 0L) {
			execute();
			return;
		}

		List<Long> memberRoleIds = event.getInteraction().getMember().get().getRoleIds()
				.stream().map(Snowflake::asLong).toList();
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
		if (this.getClass().isAnnotationPresent(ModCommand.class)) {// TODO! admins koennen das auh ausfoheren
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
	}

	protected abstract void execute();
}
