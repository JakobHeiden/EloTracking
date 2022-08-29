package com.elorankingbot.backend.commands.mod;

import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.MatchService;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.USER;

public abstract class ForceMatch extends SlashCommand {

	protected List<List<Player>> teams;
	protected Match match;
	protected MatchFinderQueue queue;
	protected List<User> users;
	protected Game game;

	protected ForceMatch(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	protected void execute() {
		Game game = server.getGame(event.getOptions().get(0).getName());
		boolean isSingularQueue = event.getOptions().get(0).getOptions().get(0).getType().equals(USER);
		queue = isSingularQueue ? game.getQueues().stream().findAny().get()
				: game.getQueue(event.getOptions().get(0).getOptions().get(0).getName());
		var userOptions = isSingularQueue ? event.getOptions().get(0).getOptions()
				: event.getOptions().get(0).getOptions().get(0).getOptions();
		users = userOptions.stream().map(option -> option.getValue().get().asUser().block()).toList();
		for (User user : users) {
			if (user.isBot()) {
				event.reply(String.format("%s is a bot and cannot be part of a match.", user.getTag())).doOnError(super::forwardToEventParser).subscribe();
				return;
			}
		}
		if (users.size() > new HashSet<>(users).size()) {
			event.reply("A user cannot be in a match more than once.").subscribe();
			return;
		}

		teams = makeTeams();
		match = new Match(queue, teams);
		doReports();
		MatchResult matchResult = MatchService.generateMatchResult(match);
		String resolveMessage = String.format(String.format("%s has force-resolved a match of %s.",
				event.getInteraction().getUser().getTag(), game.getName()));
		EmbedCreateSpec matchEmbed = matchService.processForcedMatchResult(matchResult, users, resolveMessage);
		event.reply().withEmbeds(matchEmbed).doOnError(super::forwardToEventParser).subscribe();
	}

	private List<List<Player>> makeTeams() {
		List<List<Player>> result = new ArrayList<>(queue.getNumTeams());
		for (int teamIndex = 0; teamIndex < queue.getNumTeams(); teamIndex++) {
			result.add(new ArrayList<>(queue.getNumPlayersPerTeam()));
			for (int playerIndex = 0; playerIndex < queue.getNumPlayersPerTeam(); playerIndex++) {
				User user = users.get(teamIndex * queue.getNumPlayersPerTeam() + playerIndex);
				result.get(teamIndex).add(dbService.getPlayerOrGenerateIfNotPresent(
						server.getGuildId(), user.getId().asLong(), user.getTag()));
			}
		}
		return result;
	}

	protected abstract void doReports();
}
