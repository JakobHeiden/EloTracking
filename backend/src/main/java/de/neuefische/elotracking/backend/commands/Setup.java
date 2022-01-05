package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Setup extends SlashCommand {

	public Setup(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public static ApplicationCommandRequest getRequest() {
		return ApplicationCommandRequest.builder()
				.name("setup")
				.description("Get started with the bot")
				.addOption(ApplicationCommandOptionData.builder()
						.name("nameofgame").description("The name of the game you want to track elo rating for")
						.type(3).required(true).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("allowdraw").description("Allow draw results and not just win or lose?")
						.type(5).required(true).build())
				.build();
	}

	public void execute() {
		Guild guild = event.getInteraction().getGuild().block();

		game = new Game(guild.getId().asLong(),
				event.getOption("nameofgame").get().getValue().get().asString());

		Role adminRole = guild.createRole(RoleCreateSpec.builder().name("Elo Admin")
				.permissions(PermissionSet.none())
				.mentionable(true)
				.build()).block();
		game.setAdminRoleId(adminRole.getId().asLong());
		Role modRole = guild.createRole(RoleCreateSpec.builder().name("Elo Moderator")
				.permissions(PermissionSet.none())
				.build()).block();
		game.setModRoleId(modRole.getId().asLong());
		event.getInteraction().getMember().get().addRole(adminRole.getId()).subscribe();

		Createresultchannel.staticExecute(service, guild, game);

		Category disputeCategory = guild.createCategory("elo disputes").withPermissionOverwrites(
				PermissionOverwrite.forRole(guild.getId(), PermissionSet.none(),
						PermissionSet.of(Permission.VIEW_CHANNEL)),
				PermissionOverwrite.forRole(adminRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none()),
				PermissionOverwrite.forRole(modRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none())).block();
		game.setDisputeCategoryId(disputeCategory.getId().asLong());

		game.setAllowDraw(event.getOption("allowdraw").get().getValue().get().asBoolean());

		service.saveGame(game);

		event.reply("Setup performed. Here is what I did:\n" +
				"- I created the roles Elo Admin and Elo Moderator\n" +// TODO rollen erklaeren
				"- I made you an Elo Admin\n" +
				"- I created a channel where I will post all match results\n" +
				"- I created a channel category ELO DISPUTES only visible to Elo Admin and Elo Moderator\n" +
				"- I created a web page with rankings: "
				+ String.format("http://%s/%s\n", service.getPropertiesLoader().getBaseUrl(), guildId)
				+ "Players should now be able to challenge each other with the /challenge command or by going " +
				"right click on a user -> apps -> challenge.").subscribe();

		ApplicationService applicationService = client.getRestClient().getApplicationService();
		long botId = client.getSelfId().asLong();
		applicationService.getGuildApplicationCommands(botId, guildId)
				.filter(applicationCommandData -> applicationCommandData.name().equals("setup"))
				.map(applicationCommandData ->
						applicationService.deleteGuildApplicationCommand(
								botId, guildId, Long.parseLong(applicationCommandData.id()))
				.subscribe()).subscribe();
	}
}
