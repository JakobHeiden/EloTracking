package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.AdminCommand;
import com.elorankingbot.backend.command.ModCommand;
import com.elorankingbot.backend.commands.admin.SetPermissions;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.List;

public abstract class SlashCommand {

	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final GatewayDiscordClient client;
	protected final MatchService matchService;
	protected final QueueService queueService;
	protected final TimedTaskQueue timedTaskQueue;
	protected final ChatInputInteractionEvent event;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;
	protected final long activeUserId;

	protected static final List none = new ArrayList<>();

	protected SlashCommand(ChatInputInteractionEvent event, Services services) {
		this.event = event;
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.matchService = services.matchService;
		this.queueService = services.queueService;
		this.client = services.client;
		this.timedTaskQueue = services.timedTaskQueue;

		this.guildId = event.getInteraction().getGuildId().get().asLong();
		this.server = dbService.findServerByGuildId(guildId).get();
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
	}

	public void doExecute() {
		if (this.getClass() == SetPermissions.class && server.getAdminRoleId() == 0L) {
			execute();
			return;
		}

		List<Long> memberRoleIds = event.getInteraction().getMember().get().getRoleIds()
				.stream().map(Snowflake::asLong).toList();
		if (this.getClass().isAnnotationPresent(AdminCommand.class)) {
			if (server.getAdminRoleId() == 0L) {
				event.reply("This command requires Admin permissions. The Admin role is not currently set. " +
						"Please use /setpermissions.").subscribe();
				return;
			}
			if (!memberRoleIds.contains(server.getAdminRoleId())) {
				event.reply(String.format("This command requires the <@&%s> role.", server.getAdminRoleId())).subscribe();
				return;
			}
		}
		if (this.getClass().isAnnotationPresent(ModCommand.class)) {
			if (server.getModRoleId() == 0L) {
				event.reply("This command requires Moderator permissions. The Moderator role is not currently set. " +
						"Please use /setpermissions.").subscribe();
				return;
			}
			if (!memberRoleIds.contains(server.getModRoleId())) {
				event.reply(String.format("This command requires the <@&%s> role.", server.getModRoleId())).subscribe();
				return;
			}
		}

		execute();
	}

	protected abstract void execute();
}
