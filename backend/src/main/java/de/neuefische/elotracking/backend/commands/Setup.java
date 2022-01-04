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
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Setup extends SlashCommand {

	public Setup(ChatInputInteractionEvent event, EloTrackingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
	}

	public void execute() {
		if (game != null) {
			event.reply("Setup was already performed before.").subscribe();
			return;
		}

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
	}
}
