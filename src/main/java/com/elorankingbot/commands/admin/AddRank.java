package com.elorankingbot.commands.admin;

import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.RankingCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.Player;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.extern.apachecommons.CommonsLog;

import java.util.List;

import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@CommonsLog
@AdminCommand
@RankingCommand
public class AddRank extends SlashCommand {

    public AddRank(ChatInputInteractionEvent event, Services services) {
        super(event, services);
    }

    public static ApplicationCommandRequest getRequest(Server server) {
        var requestBuilder = ApplicationCommandRequest.builder()
                .name("addrank")
                .description(getShortDescription())
                .defaultPermission(true);
        if (server.getGames().size() > 1) {
            requestBuilder.addOption(ApplicationCommandOptionData.builder()
                    .name("ranking")
                    .description("Add a rank to which ranking?")
                    .type(STRING.getValue())
                    .required(true)
                    .addAllChoices(server.getGames().stream().map(game ->
                                    (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData.builder()
                                            .name(game.getName())
                                            .value(game.getName())
                                            .build())
                            .toList())
                    .build());
        }
        return requestBuilder.addOption(ApplicationCommandOptionData.builder()
                        .name("role")
                        .description("Which role to assign as a rank?")
                        .type(ROLE.getValue())
                        .required(true).build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("rating")
                        .description("The minimum rating to acquire the rank")
                        .type(INTEGER.getValue())
                        .required(true).build())
                .build();
    }

    public static String getShortDescription() {
        return "Assign a role as a rank, to be automatically assigned based on player rating.";
    }

    public static String getLongDescription() {
        return getShortDescription() + "\n" +
                "`Required:` `ranking` Add a rank to this ranking. " +
                "This option won't be present if the server only has one ranking.\n" +
                "`Required:` `role` The role to make into a rank.\n" +
                "`Required:` `rating` The minimum rating to attain the new rank.\n" +
                "If a player qualifies for more than one rank, only the highest rank will apply.";
    }

    protected void execute() {
        int rating;
        try {
            rating = Math.toIntExact(event.getOption("rating").get().getValue().get().asLong());
        } catch (ArithmeticException e) {
            event.reply("Selected rating is too large.").subscribe();
            return;
        }
        Role role = event.getOption("role").get().getValue().get().asRole().block();
        if (role.isManaged()) {
            event.reply("This role is managed by an integration and cannot be made a rank. " +
                    "Usually this means that this role is a bot role. Please choose a different role.").subscribe();
            return;
        }
        if (role.isEveryone()) {
            event.reply("Cannot make @everyone a rank.").subscribe();
            return;
        }
        List<Long> allRanks = server.getGames().stream().flatMap(game -> game.getRequiredRatingToRankId().values().stream()).toList();
        if (allRanks.contains(role.getId().asLong())) {
            event.reply("This role is already assigned to a rank and cannot be assigned to a rank again.").subscribe();
            return;
        }

        Game game = server.getGames().size() > 1 ?
                server.getGame(event.getOption("ranking").get().getValue().get().asString())
                : server.getGames().get(0);
        game.getRequiredRatingToRankId().put(rating, role.getId().asLong());
        dbService.saveServer(server);
        for (Player player : dbService.findAllPlayersForServer(server)) {
            matchService.updatePlayerRank(game, player, manageRoleFailedCallbackFactory());
        }
        event.reply(String.format("@%s will now automatically be assigned to any player of %s who reaches %s rating.",
                role.getName(), game.getName(), rating)).subscribe();
        if (!bot.isBotRoleHigherThanGivenRole(role)) {
            event.createFollowup(String.format("I currently hold no role that is higher than %s." +
                                    " As a result I cannot assign %s to players." +
                                    " Please move the %s role up in the hierarchy, or assign me a role that is above %s.",
                            role.getName(), role.getName(), bot.getBotIntegrationRole(server).getName(), role.getName()))
                    .block();
        }
    }
}
