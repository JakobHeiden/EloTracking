package com.elorankingbot.commands.mod;

import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.*;
import com.elorankingbot.service.MatchService;
import com.elorankingbot.service.Services;
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
		event.deferReply().subscribe();
		Game game = server.getGame(event.getOptions().get(0).getName());
		boolean isSingularQueue = event.getOptions().get(0).getOptions().get(0).getType().equals(USER);
		queue = isSingularQueue ? game.getQueues().stream().findAny().get()
				: game.getQueue(event.getOptions().get(0).getOptions().get(0).getName());
		var userOptions = isSingularQueue ? event.getOptions().get(0).getOptions()
				: event.getOptions().get(0).getOptions().get(0).getOptions();
		users = userOptions.stream().map(option -> option.getValue().get().asUser().block()).toList();
		for (User user : users) {
			if (user.isBot()) {
				event.createFollowup(String.format("%s is a bot and cannot be part of a match.", user.getTag())).subscribe(NO_OP, super::forwardToExceptionHandler);
				return;
			}
		}
		if (users.size() > new HashSet<>(users).size()) {
			event.createFollowup("A user cannot be in a match more than once.").subscribe();
			return;
		}

		teams = makeTeams();
		match = new Match(queue, teams);
		doReports();
		MatchResult matchResult = MatchService.generateMatchResult(match);
		String resolveMessage = String.format(String.format("%s has force-resolved a match of %s.",
				event.getInteraction().getUser().getTag(), game.getName()));
		EmbedCreateSpec matchEmbed = matchService.processForcedMatchResult(matchResult, users, resolveMessage, event, manageRoleFailedCallbackFactory());
		event.createFollowup().withEmbeds(matchEmbed).subscribe(NO_OP, super::forwardToExceptionHandler);
	}

	private List<List<Player>> makeTeams() {
		List<List<Player>> result = new ArrayList<>(queue.getNumTeams());
		for (int teamIndex = 0; teamIndex < queue.getNumTeams(); teamIndex++) {
			result.add(new ArrayList<>(queue.getNumPlayersPerTeam()));
			for (int playerIndex = 0; playerIndex < queue.getNumPlayersPerTeam(); playerIndex++) {
				User user = users.get(teamIndex * queue.getNumPlayersPerTeam() + playerIndex);
				result.get(teamIndex).add(dbService.getPlayerOrGenerateIfNotPresent(server.getGuildId(), user));
			}
		}
		return result;
	}

	protected abstract void doReports();
}
