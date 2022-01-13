package com.elorankingbot.backend.commands;

import com.elorankingbot.backend.command.MessageContent;
import com.elorankingbot.backend.model.Game;
import com.elorankingbot.backend.service.DiscordBotService;
import com.elorankingbot.backend.service.EloRankingService;
import com.elorankingbot.backend.timedtask.TimedTaskQueue;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public class Setup extends SlashCommand {

	private Guild guild;
	private long botId;
	private Role adminRole;
	private Role modRole;
	private ApplicationService applicationService;
	private MessageContent reply;

	public Setup(ChatInputInteractionEvent event, EloRankingService service, DiscordBotService bot,
				 TimedTaskQueue queue, GatewayDiscordClient client) {
		super(event, service, bot, queue, client);
		this.applicationService = client.getRestClient().getApplicationService();
		this.guild = event.getInteraction().getGuild().block();
		this.botId = client.getSelfId().asLong();
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
				.addOption(ApplicationCommandOptionData.builder()
						.name("adminrole").description("Choose an Elo Admin role. This role can change my settings. " +
								"Alternatively assign this later.")
						.type(8).required(false).build())
				.addOption(ApplicationCommandOptionData.builder()
						.name("moderatorrole").description("Choose an Elo Moderator role. This role is " +
								"for dispute resolution. Alternatively assign this later.")
						.type(8).required(false).build())
				.build();
	}

	// TODO! setup -> reset -> setup - commands vom timing her sortieren, warten, oder so
	public void execute() {
		reply = new MessageContent("Setup performed. Here is what I did:");
		game = new Game(guild.getId().asLong(),
				event.getOption("nameofgame").get().getValue().get().asString());

		assignModAndAdminRole();
		createResultChannel();
		createDisputeCategory();
		game.setAllowDraw(event.getOption("allowdraw").get().getValue().get().asBoolean());
		service.saveGame(game);

		deleteSetupCommand();
		if (game.isAllowDraw()) bot.deployCommandToGuild(Forcedraw.getRequest(), guild, adminRole, modRole).subscribe();
		bot.deployCommandToGuild(Forcewin.getRequest(), guild, adminRole, modRole).subscribe();
		bot.deployCommandToGuild(Challenge.getRequest(), guild).subscribe();
		bot.deployCommandToGuild(ChallengeAsUserInteraction.getRequest(), guild).subscribe();
		bot.deployCommandToGuild(Reset.getRequest(), guild, adminRole).subscribe();
		reply.addLine("- I updated my commands on this server");

		reply.addLine(String.format("- I created a web page with rankings: http://%s/%s",
						service.getPropertiesLoader().getBaseUrl(), guildId));
		reply.addLine(String.format("Follow my announcement channel: <#%s>",
				service.getPropertiesLoader().getAnnouncementChannelId()));

		event.reply(reply.get()).doOnError(error -> System.out.println(error.getMessage())).subscribe();

		bot.sendToOwner(String.format("Setup performed on guild %s:%s with %s members",
				guild.getId(), guild.getName(), guild.getMemberCount()));
	}

	private void assignModAndAdminRole() {
		boolean adminRolePresent = event.getOption("adminrole").isPresent();
		boolean modRolePresent = event.getOption("modrole").isPresent();
		Role everyone = null;
		if (!adminRolePresent || !modRolePresent) everyone = guild.getEveryoneRole().block();

		if (adminRolePresent) {
			adminRole = event.getOption("adminrole").get().getValue().get().asRole().block();
			reply.addLine(String.format("- I assigned Elo Admin permissions to %s", adminRole.getName()));
		} else {
			adminRole = everyone;
			reply.addLine("- I assigned Elo Admin permissions to @everyone. I suggest you use /addpermission soon.");
		}
		game.setAdminRoleId(adminRole.getId().asLong());

		if (modRolePresent) {
			modRole = event.getOption("moderatorrole").get().getValue().get().asRole().block();
			reply.addLine(String.format("- I assigned Elo Moderator permissions to %s", modRole.getName()));
		} else {
			modRole = everyone;
			reply.addLine("- I assigned Elo Moderator permissions to @everyone. I suggest you use /addpermission soon.");
		}
		game.setModRoleId(modRole.getId().asLong());
	}

	private void createResultChannel() {
		TextChannel resultChannel = guild.createTextChannel("Elo Ranking results")
				.withTopic(String.format("All resolved matches will be logged here. Leaderboard: http://%s/%s",
						service.getPropertiesLoader().getBaseUrl(), guild.getId().asString()))
				.withPermissionOverwrites(PermissionOverwrite.forRole(
						Snowflake.of(guildId),
						PermissionSet.none(),
						PermissionSet.of(Permission.SEND_MESSAGES)))
				.block();
		game.setResultChannelId(resultChannel.getId().asLong());
		reply.addLine(String.format("- I created %s where I will post all match results", resultChannel.getMention()));
	}

	private void createDisputeCategory() {
		Category disputeCategory = guild.createCategory("elo disputes").withPermissionOverwrites(
				PermissionOverwrite.forRole(guild.getId(), PermissionSet.none(),
						PermissionSet.of(Permission.VIEW_CHANNEL)),
				PermissionOverwrite.forRole(adminRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none()),
				PermissionOverwrite.forRole(modRole.getId(), PermissionSet.of(Permission.VIEW_CHANNEL),
						PermissionSet.none())).block();
		game.setDisputeCategoryId(disputeCategory.getId().asLong());
		reply.addLine(String.format("- I created a category %s where I will create dispute channels as needed. " +
				"It is only visible to Elo Admins and Moderators", disputeCategory.getMention()));
	}

	private void deleteSetupCommand() {
		applicationService.getGuildApplicationCommands(botId, guildId)
				.filter(applicationCommandData -> applicationCommandData.name().equals("setup"))
				.map(applicationCommandData ->
						applicationService.deleteGuildApplicationCommand(
										botId, guildId, Long.parseLong(applicationCommandData.id()))
								.doOnError(e -> System.out.println(e.toString()))
								.subscribe()).subscribe();
	}
}
