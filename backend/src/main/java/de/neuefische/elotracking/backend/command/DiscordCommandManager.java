package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.commands.Challenge;
import de.neuefische.elotracking.backend.commands.ChallengeAsUserInteraction;
import de.neuefische.elotracking.backend.commands.Forcewin;
import de.neuefische.elotracking.backend.configuration.ApplicationPropertiesLoader;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DiscordCommandManager {

	private final GatewayDiscordClient client;
	private final ApplicationService applicationService;
	private final long entenwieseId;

	public static String[] commandsThatNeedModRole = {"forcewin", "forcedraw"};
	public static String[] commandsThatNeedAdminRole = {};
	public static List<ApplicationCommandRequest> allNecessaryGlobalApplicationCommandRequests() {
		return List.of(
				Challenge.getRequest(),
				ChallengeAsUserInteraction.getRequest(),
				Forcewin.getRequest());
	}

	public DiscordCommandManager(ApplicationPropertiesLoader applicationPropertiesLoader, GatewayDiscordClient client) {
		this.client = client;
		this.applicationService = client.getRestClient().getApplicationService();
		this.entenwieseId = Long.parseLong(applicationPropertiesLoader.getEntenwieseId());

		/*if (applicationPropertiesLoader.isUseGlobalCommands()) {
			deleteGlobalCommandsNotNecessary();
			deployGlobalCommandsThatAreNotPresent();
		} else {
			bulkOverwriteGuildCommands();
		}*/
	}

	private void deployGlobalCommandsThatAreNotPresent() {
		List<String> globalCommandsPresent = applicationService
				.getGlobalApplicationCommands(client.getSelfId().asLong())
				.map(applicationCommandData -> applicationCommandData.name())
				.collectList()
				.block();
		for (ApplicationCommandRequest request : allNecessaryGlobalApplicationCommandRequests()) {
			if (!globalCommandsPresent.contains(request.name())) {
				applicationService.createGlobalApplicationCommand(client.getSelfId().asLong(), request).subscribe();
				log.info(String.format("Deploying global command %s", request.name()));
			}
		}
	}

	private void deleteGlobalCommandsNotNecessary() {
		List<ApplicationCommandData> globalCommandsPresent = applicationService
				.getGlobalApplicationCommands(client.getSelfId().asLong())
				.collectList()
				.block();
		List<String> globalCommandsNecessary = allNecessaryGlobalApplicationCommandRequests().stream()
				.map(request -> request.name())
				.collect(Collectors.toList());
		for (ApplicationCommandData commandData : globalCommandsPresent) {
			if (!globalCommandsNecessary.contains(commandData.name())) {
				applicationService.deleteGlobalApplicationCommand(
						client.getSelfId().asLong(), Long.parseLong(commandData.id())).subscribe();
				log.info(String.format("Deleting global command %s", commandData.name()));
			}
		}
	}

	private void bulkOverwriteGuildCommands() {
		log.info("Deploying guild commands...");
		applicationService.bulkOverwriteGuildApplicationCommand(
						client.getSelfId().asLong(),
						entenwieseId,
						allNecessaryGlobalApplicationCommandRequests())
				.subscribe();
	}
}
