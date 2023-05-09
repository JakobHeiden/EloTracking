package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.DiscordCommandManager;
import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.command.annotations.ModCommand;
import com.elorankingbot.backend.command.annotations.OwnerCommand;
import com.elorankingbot.backend.commands.admin.SetPermission;
import com.elorankingbot.backend.configuration.ApplicationPropertiesLoader;
import com.elorankingbot.backend.logging.ExceptionHandler;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.*;
import com.elorankingbot.backend.timedtask.TimedTaskScheduler;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import lombok.extern.apachecommons.CommonsLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@CommonsLog
public abstract class Command {

	protected final DBService dbService;
	protected final DiscordBotService bot;
	protected final ChannelManager channelManager;
	protected final DiscordCommandManager discordCommandManager;
	protected final MatchService matchService;
	protected final QueueScheduler queueScheduler;
	protected final TimedTaskScheduler timedTaskScheduler;
	protected final ApplicationPropertiesLoader props;
	protected final ExceptionHandler exceptionHandler;
	protected final DeferrableInteractionEvent event;
	protected final Snowflake guildSnowflake;
	protected final long guildId;
	protected final Server server;
	protected final User activeUser;
	protected final long activeUserId;
	protected boolean userIsAdmin;
	protected boolean alreadySentManageRoleFailedFollowup = false;
	protected final Consumer<Object> NO_OP = object -> {};

	protected Command(DeferrableInteractionEvent event, Services services) {
		this.dbService = services.dbService;
		this.bot = services.bot;
		this.channelManager = services.channelManager;
		this.discordCommandManager = services.discordCommandManager;
		this.matchService = services.matchService;
		this.queueScheduler = services.queueScheduler;
		this.timedTaskScheduler = services.timedTaskScheduler;
		this.props = services.props;
		this.exceptionHandler = services.exceptionHandler;
		this.event = event;
		this.guildSnowflake = event.getInteraction().getGuildId().get();
		this.guildId = guildSnowflake.asLong();
		this.server = dbService.getOrCreateServer(guildId);
		this.activeUser = event.getInteraction().getUser();
		this.activeUserId = activeUser.getId().asLong();
	}

	protected abstract void execute() throws Exception;

	public void doExecute() throws Exception {
		Date start = new Date();
		logExecutionStart();

		// bypass permission check when admin role is not set or not present
		if (this.getClass() == SetPermission.class) {
			boolean adminRoleExists = event.getInteraction().getGuild().block()
					.getRoles().map(role -> role.getId().asLong()).collectList().block()
					.contains(server.getAdminRoleId());
			if (server.getAdminRoleId() == 0L || !adminRoleExists) {
				execute();
				return;
			}
		}

		List<Long> memberRoleIds = new ArrayList<>(event.getInteraction().getMember().get().getRoleIds()
				.stream().map(Snowflake::asLong).toList());
		memberRoleIds.add(guildId);
		boolean userIsOwner = activeUserId == props.getOwnerId();
		userIsAdmin = memberRoleIds.contains(server.getAdminRoleId()) || userIsOwner;
		boolean userIsMod = memberRoleIds.contains(server.getModRoleId());
		if (this.getClass().isAnnotationPresent(OwnerCommand.class)
				&& event.getInteraction().getUser().getId().asLong() != props.getOwnerId()) {
			event.reply("This command requires Owner permissions.").withEphemeral(true).subscribe();
			bot.sendToOwner(String.format("User %s tried executing %s on %s", activeUser.getTag(), this.getClass().getSimpleName(), server.getGuildId()));
			return;
		}
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

		int duration = (int) (new Date().getTime() - start.getTime());
		logExecutionFinish(duration);
	}

	private void logExecutionStart() {
		log.debug(String.format("execute %s by %s on %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				event.getInteraction().getGuildId().get().asString()));
	}

	private void logExecutionFinish(int duration) {
		String executionSummary = String.format("%s by %s on %s in %s",
				this.getClass().getSimpleName(),
				event.getInteraction().getUser().getTag(),
				bot.getServerIdAndName(server),
				duration);
		if (duration < 3000) {
			log.trace("Done executing " + executionSummary);
		} else {
			log.warn("Slow command: " + executionSummary);
			//bot.sendToOwner("Slow command: " + executionSummary);
		}
	}

	protected void acknowledgeEvent() {
		// acknowledge() being deprecated seems bullshit. The supposed replacement methods don't do what's advertised
		event.acknowledge().subscribe(NO_OP, this::forwardToExceptionHandler);
	}

	protected void forwardToExceptionHandler(Throwable throwable) {
		exceptionHandler.handleUnexpectedCommandException(throwable, event, this.getClass().getSimpleName());
	}

	protected Function<Role, Consumer<Throwable>> manageRoleFailedCallbackFactory() {
		return role -> throwable -> {
			if (!alreadySentManageRoleFailedFollowup) {
				alreadySentManageRoleFailedFollowup = true;
				event.createFollowup(String.format(
						"I was unable to assign or remove @%s. Please check my permissions and that @%s is above @%s in the role hierarchy.",
						role.getName(), bot.getBotIntegrationRole(event.getInteraction().getGuildId().get().asLong()).getName(), role.getName())).subscribe();
			}
		};
	}
}
