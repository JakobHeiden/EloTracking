package com.elorankingbot.backend.service;

import com.elorankingbot.backend.components.Buttons;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.TextChannelCreateMono;
import discord4j.core.spec.TextChannelEditMono;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.CHANNEL_DELETE;

@Component
public class ChannelManager {

	private final DBService dbService;
	private final DiscordBotService bot;
	private final TimedTaskQueue timedTaskQueue;

	public ChannelManager(Services services) {
		this.bot = services.bot;
		this.dbService = services.dbService;
		this.timedTaskQueue = services.timedTaskQueue;
	}

	private TextChannelEditMono setParentCategory(Channel channel, long categoryId) {
		return ((TextChannel) channel).edit().withParentId(Possible.of(Optional.of(Snowflake.of(categoryId))));
	}

	// Result
	public TextChannel getOrCreateResultChannel(Game game) {
		try {
			return (TextChannel) bot.getChannelById(game.getResultChannelId()).block();
		} catch (ClientException e) {// TODO funktioniert das ueberhaupt? testen!
			Guild guild = bot.getGuildById(game.getGuildId()).block();
			TextChannel resultChannel = guild.createTextChannel(String.format("%s match results", game.getName()))
					.withPermissionOverwrites(onlyBotCanSend(game.getServer()))
					.block();
			game.setResultChannelId(resultChannel.getId().asLong());
			dbService.saveServer(game.getServer());
			return resultChannel;
		}
	}

	public Message postToResultChannel(MatchResult matchResult) {
		Game game = matchResult.getGame();
		TextChannel resultChannel = getOrCreateResultChannel(game);
		return resultChannel.createMessage(EmbedBuilder.createMatchResultEmbed(matchResult)).block();
	}

	// Match
	public Category getOrCreateMatchCategory(Server server) {
		try {
			return (Category) bot.getChannelById(server.getMatchCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
					&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
				throw e;
			}
			Guild guild = bot.getGuildById(server.getGuildId()).block();
			Category matchCategory = guild.createCategory("elo matches")
					.withPermissionOverwrites(excludePublic(server))
					.block();
			server.setMatchCategoryId(matchCategory.getId().asLong());
			dbService.saveServer(server);
			return matchCategory;
		}
	}

	public TextChannelCreateMono createMatchChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = excludePublic(server);
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		String channelName = createMatchChannelName(match.getTeams());
		Category matchCategory = getOrCreateMatchCategory(server);
		return bot.getGuildById(match.getGame().getGuildId()).block()
				.createTextChannel(channelName)
				.withParentId(matchCategory.getId())
				.withPermissionOverwrites(permissionOverwrites);
	}

	private String createMatchChannelName(List<List<Player>> teams) {
		String tentativeString = String.join("-vs-", teams.stream()
				.map(team -> String.join("-", team.stream().map(Player::getTag).toList())).toList());
		if (tentativeString.length() <= 100) {
			return tentativeString;
		} else {
			return tentativeString.substring(0, 100);
		}
	}

	void sendMatchMessage(TextChannel channel, Match match) {
		String title = String.format("Your match of %s is starting. " +
						"I removed you from all other queues you joined on this server, if any. " +
						"Please play the match and come back to report the result afterwards.",
				match.getQueue().getFullName());
		EmbedCreateSpec embedCreateSpec = EmbedBuilder.createMatchEmbed(title, match);
		Message message = channel.createMessage(match.getAllMentions())
				.withEmbeds(embedCreateSpec)
				.withComponents(createMatchActionRow(match)).block();
		message.pin().subscribe();
		match.setMessageId(message.getId().asLong());
		match.setChannelId(message.getChannelId().asLong());
	}

	private static ActionRow createMatchActionRow(Match match) {
		UUID matchId = match.getId();
		if (match.getGame().isAllowDraw()) return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.draw(matchId),
				Buttons.cancel(matchId),
				Buttons.dispute(matchId));
		else return ActionRow.of(
				Buttons.win(matchId),
				Buttons.lose(matchId),
				Buttons.cancel(matchId),
				Buttons.dispute(matchId));
	}

	// Dispute
	public Category getOrCreateDisputeCategory(Server server) {// TODO evtl Optional<Guild> mit als parameter, um den request zu sparen?
		try {
			return (Category) bot.getChannelById(server.getDisputeCategoryId()).block();
		} catch (ClientException e) {
			if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
					&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
				throw e;
			}
			Guild guild = bot.getGuildById(server.getGuildId()).block();
			Category disputeCategory = guild.createCategory("elo disputes")
					.withPermissionOverwrites(excludePublic(server))
					.block();
			server.setDisputeCategoryId(disputeCategory.getId().asLong());
			dbService.saveServer(server);
			return disputeCategory;
		}
	}

	public void moveToDisputes(Server server, Channel channel) {
		Category disputeCategory = getOrCreateDisputeCategory(server);
		setParentCategory(channel, disputeCategory.getId().asLong()).subscribe();
	}

	public void createDisputeMessage(TextChannel channel, Match match, String activeUserTag) {
		channel.createMessage(String.format("**%s filed a dispute. Please state your view of the conflict so a <@&%s> can resolve it.**",
						activeUserTag, match.getServer().getModRoleId()))
				.withComponents(createDisputeActionRow(match))
				.subscribe();
	}

	public static ActionRow createDisputeActionRow(Match match) {
		int numTeams = match.getTeams().size();
		UUID matchId = match.getId();
		List<ActionComponent> buttons = new ArrayList<>(numTeams);
		for (int i = 0; i < numTeams; i++) {
			buttons.add(Buttons.ruleAsWin(matchId, i));
		}
		if (match.getGame().isAllowDraw()) buttons.add(Buttons.ruleAsDraw(matchId));
		buttons.add(Buttons.ruleAsCancel(matchId));
		return ActionRow.of(buttons);
	}

	public TextChannelCreateMono createDisputeChannel(Match match) {
		Server server = match.getServer();
		List<PermissionOverwrite> permissionOverwrites = excludePublic(server);
		match.getPlayers().forEach(player -> permissionOverwrites.add(allowPlayerView(player)));
		Category disputeCategory = getOrCreateDisputeCategory(server);
		return bot.getGuildById(match.getGame().getGuildId()).block()
				.createTextChannel(createMatchChannelName(match.getTeams()))
				.withParentId(disputeCategory.getId())
				.withPermissionOverwrites(permissionOverwrites);
	}

	// Leaderboard
	private Message createLeaderboardChannelAndMessage(Game game) {
		Guild guild = bot.getGuildById(game.getGuildId()).block();
		TextChannel leaderboardChannel = guild.createTextChannel(String.format("%s Leaderboard", game.getName()))
				.withPermissionOverwrites(onlyBotCanSend(game.getServer()))
				.block();
		Message leaderboardMessage = leaderboardChannel.createMessage("creating leaderboard...").block();
		game.setLeaderboardMessageId(leaderboardMessage.getId().asLong());
		game.setLeaderboardChannelId(leaderboardChannel.getId().asLong());
		return leaderboardMessage;
	}

	public void refreshLeaderboard(Game game) {
		Message leaderboardMessage;
		try {
			leaderboardMessage = bot.getMessage(game.getLeaderboardMessageId(), game.getLeaderboardChannelId()).block();
		} catch (ClientException e) {
			leaderboardMessage = createLeaderboardChannelAndMessage(game);
			dbService.saveServer(game.getServer());// TODO muss das?
		}
		leaderboardMessage.edit()
				.withContent(Possible.of(Optional.empty()))
				.withEmbeds(EmbedBuilder.createRankingsEmbed(dbService.getLeaderboard(game))).subscribe();
	}

	// Archive
	public Category getOrCreateArchiveCategory(Server server) {
		List<Long> categoryIds = server.getArchiveCategoryIds();
		Category archiveCategory;
		int index = 0;
		while (true) {
			if (index >= categoryIds.size()) {
				Guild guild = bot.getGuildById(server.getGuildId()).block();
				archiveCategory = guild.createCategory(String.format("elo archive%s", index == 0 ? "" : index + 1))
						.withPermissionOverwrites(excludePublic(server))
						.block();
				categoryIds.add(archiveCategory.getId().asLong());
				dbService.saveServer(server);
				break;
			}
			try {
				archiveCategory = (Category) bot.getChannelById(categoryIds.get(index)).block();
			} catch (ClientException e) {
				if (!e.getErrorResponse().get().getFields().get("message").toString().equals("Unknown Channel")
						&& !e.getErrorResponse().get().toString().contains("CHANNEL_PARENT_INVALID")) {
					throw e;
				}
				Guild guild = bot.getGuildById(server.getGuildId()).block();
				archiveCategory = guild.createCategory(String.format("elo archive%s", index == 0 ? "" : " " + (index + 1)))
						.withPermissionOverwrites(excludePublic(server))
						.block();
				categoryIds.set(index, archiveCategory.getId().asLong());
				dbService.saveServer(server);
				break;
			}
			if (archiveCategory.getChannels().count().block() < 47) {
				break;
			}
			index++;
		}
		return archiveCategory;
	}

	public void moveToArchive(Server server, Channel channel) {
		Category archiveCategory = getOrCreateArchiveCategory(server);
		setParentCategory(channel, archiveCategory.getId().asLong()).subscribe();
		timedTaskQueue.addTimedTask(CHANNEL_DELETE, 60, channel.getId().asLong(), 0L, null);
		((TextChannel) channel).createMessage("**I have moved this channel to the archive. " +
				"I will delete this channel in one hour.**").subscribe();
	}

	// write permissions
	private List<PermissionOverwrite> onlyBotCanSend(Server server) {
		return List.of(PermissionOverwrite.forRole(
						Snowflake.of(server.getEveryoneId()),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)),
				PermissionOverwrite.forMember(
						Snowflake.of(bot.botId),
						PermissionSet.of(Permission.SEND_MESSAGES),
						PermissionSet.none()));
	}

	// view permissions
	public List<PermissionOverwrite> excludePublic(Server server) {// TODO die ganzen categories aufraeumen
		return new ArrayList<>(List.of(denyEveryoneView(server), allowAdminView(server), allowModView(server), allowBotView()));
	}

	private static PermissionOverwrite denyEveryoneView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getGuildId()),
				PermissionSet.none(),
				PermissionSet.of(Permission.VIEW_CHANNEL));
	}

	private static PermissionOverwrite allowAdminView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getAdminRoleId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	private static PermissionOverwrite allowModView(Server server) {
		return PermissionOverwrite.forRole(Snowflake.of(server.getModRoleId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	private static PermissionOverwrite allowPlayerView(Player player) {
		return PermissionOverwrite.forMember(Snowflake.of(player.getUserId()),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}

	private PermissionOverwrite allowBotView() {
		return PermissionOverwrite.forMember(Snowflake.of(bot.botId),
				PermissionSet.of(Permission.VIEW_CHANNEL),
				PermissionSet.none());
	}
}
