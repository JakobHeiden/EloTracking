package com.elorankingbot.backend.commands.owner;

import com.elorankingbot.backend.command.annotations.OwnerCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.Server;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;

import java.util.*;

@OwnerCommand
public class AllGuilds extends SlashCommand {

	public AllGuilds(ChatInputInteractionEvent event, Services services) {
		super(event, services);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name(AllGuilds.class.getSimpleName().toLowerCase())
				.description(AllGuilds.class.getSimpleName())
				.defaultPermission(true)
				.build();
	}

	protected void execute() throws Exception {
		event.deferReply().subscribe();
		try {
			Map<Integer, String> guilds = new HashMap<>();
			for (Server server : dbService.findAllServers()) {
				Guild guild;
				try {
					guild = bot.getGuild(server).block();
					guilds.put(guild.getMemberCount(), String.format("%s - %s:%s", guild.getMemberCount(), guild.getId().asString(), guild.getName()));
				} catch (ClientException ignored) {
				}
			}
			List<Integer> sortedKeys = new ArrayList<>(guilds.keySet().stream().toList());
			Collections.sort(sortedKeys);
			event.createFollowup(String.format("Guilds in total: %s\n", bot.getAllGuildIds().size()) +
					String.join("\n", sortedKeys.stream().map(guilds::get).toList())).withEphemeral(true).subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			event.createFollowup(e.getMessage()).withEphemeral(true)
					.subscribe(NO_OP, super::forwardToExceptionHandler);
		}
	}
}
