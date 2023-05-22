package com.elorankingbot.backend.commands.player;

import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.command.annotations.QueueCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.elorankingbot.backend.model.*;
import com.elorankingbot.backend.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.PREMADE;
import static com.elorankingbot.backend.model.MatchFinderQueue.QueueType.SOLO;
import static com.elorankingbot.backend.timedtask.TimedTask.TimedTaskType.LEAVE_QUEUES;
import static discord4j.core.object.command.ApplicationCommandOption.Type.*;

@PlayerCommand
@QueueCommand
public class Join extends SlashCommand {

    private static final int newMatchJoinTimeout = 30;

    public Join(ChatInputInteractionEvent event, Services services) {
        super(event, services);
    }

    public static ApplicationCommandRequest getRequest(Server server) {
        var requestBuilder = ApplicationCommandRequest.builder()
                .name("join")
                .description("Join a matchmaking queue")
                .defaultPermission(true);
        server.getGames().forEach(game -> {
            if (game.getQueues().size() == 1) {
                var queue = game.getQueues().stream().findAny().get();
                var queueOptionBuilder = ApplicationCommandOptionData.builder()
                        .name(game.getName().toLowerCase()).description(queue.getDescription())
                        .type(SUB_COMMAND.getValue());
                addUserOptions(queue, queueOptionBuilder);
                requestBuilder.addOption(queueOptionBuilder.build());
            } else {
                var gameOptionBuilder = ApplicationCommandOptionData.builder()
                        .name(game.getName().toLowerCase()).description("game name")
                        .type(SUB_COMMAND_GROUP.getValue());
                game.getQueues().forEach(queue -> {
                    var queueOptionBuilder = ApplicationCommandOptionData.builder()
                            .name(queue.getName().toLowerCase()).description(queue.getDescription())
                            .type(SUB_COMMAND.getValue());
                    addUserOptions(queue, queueOptionBuilder);
                    gameOptionBuilder.addOption(queueOptionBuilder.build());
                });
                requestBuilder.addOption(gameOptionBuilder.build());
            }
        });
        return requestBuilder.build();
    }

    private static void addUserOptions(MatchFinderQueue queue, ImmutableApplicationCommandOptionData.Builder queueOptionBuilder) {
        if (queue.getQueueType() != SOLO) {
            int maxPlayersInPremade = queue.getQueueType() == PREMADE ?
                    queue.getNumPlayersPerTeam() : queue.getMaxPremadeSize();
            for (int allyPlayerIndex = 1; allyPlayerIndex < maxPlayersInPremade; allyPlayerIndex++) {
                queueOptionBuilder.addOption(ApplicationCommandOptionData.builder()
                        .name("ally" + allyPlayerIndex).description("ally #" + allyPlayerIndex)
                        .type(USER.getValue())
                        .required(queue.getQueueType() == PREMADE)
                        .build());
            }
        }
    }

    public static String getShortDescription() {
        return "Join a queue.";
    }

    public static String getLongDescription() {
        return getShortDescription() + "\n" +
                "This command will not be present unless the server is configured to have at least one ranking and one " +
                "queue. There will be one `/join` command for each queue.\n" +
                "You can join as many queues as you like; Once one match starts, you will automatically be removed from " +
                "all queues.\n" +
                "For more information on queues, see `/help`:`Concept: Rankings and Queues`.";
    }

    protected void execute() {
        event.deferReply().withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());

        Game game = server.getGame(event.getOptions().get(0).getName());
        boolean isSingularQueue;
        var gameOptions = event.getOptions().get(0).getOptions();
        // queue name is not in options
        MatchFinderQueue queue;
        List<User> users;
        if (gameOptions.isEmpty() || gameOptions.get(0).getValue().isPresent()) {
            queue = game.getQueues().stream().findAny().get();
            users = gameOptions.stream()
                    .map(option -> option.getValue().get().asUser().block())
                    .collect(Collectors.toList());
            isSingularQueue = true;
            // queue name present in options
        } else {
            queue = game.getQueueNameToQueue().get(gameOptions.get(0).getName());
            users = gameOptions.get(0).getOptions().stream()
                    .map(option -> option.getValue().get().asUser().block())
                    .collect(Collectors.toList());
            isSingularQueue = false;
        }
        for (User user : users) {
            if (user.isBot()) {
                event.createFollowup("Bots cannot be added to the queue.").withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());
                return;
            }
        }
        users.add(activeUser);

        Group group = new Group(
                users.stream()
                        .map(user -> dbService.getPlayerOrGenerateIfNotPresent(guildId, user))
                        .collect(Collectors.toList()),
                game);
        for (Player player : group.getPlayers()) {
            for (Match match : dbService.findAllMatchesByPlayer(player)) {
                long secondsPassed = (new Date().getTime() - match.getTimestamp().getTime()) / 1000;
                if (secondsPassed < newMatchJoinTimeout) {
                    event.createFollowup((queue.getQueueType() == SOLO) ?
                                    String.format("You have recently been assigned a match. " +
                                            "Please wait another %s seconds before joining a queue again.", newMatchJoinTimeout - secondsPassed)
                                    : String.format("The player %s has recently been assigned a match " +
                                    "and cannot enter a queue for another %s seconds.", player.getTag(), newMatchJoinTimeout - secondsPassed))
                            .withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());
                    return;
                }
            }
            if (queue.hasPlayer(player)) {// TODO alle auflisten
                event.createFollowup(String.format("The player %s is already in this queue an cannot be added a second time.",
                                player.getTag()))// TODO unterscheiden nach active player
                        .withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());
                return;
            }
            if (player.isBanned()) {// TODO alle player auflisten
                event.createFollowup(String.format("The player %s is currently banned and cannot join a queue.", player.getTag()))
                        .withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());// TODO unterscheiden nach active player
                return;
            }
        }

        // TODO group queue

        queue.addGroup(group);
        Date now = new Date();
        for (Player player : group.getPlayers()) {
            timedTaskScheduler.addTimedTask(LEAVE_QUEUES, 180, player.getUserId(), guildId, now);
            player.setLastJoinedQueueAt(now);
            dbService.savePlayer(player);
        }
        dbService.saveServer(server);
        event.createFollowup(String.format("Queue %s joined. Once the match starts, " +
                        "I will create a channel for the match, and ping all participants.", queue.getFullName()))
                .withEphemeral(true).subscribe(NO_OP, asyncExceptionCallback());
    }

    private Consumer<Throwable> asyncExceptionCallback() {
        return throwable -> exceptionHandler.handleAsyncException(throwable, event, Join.class.getSimpleName().toLowerCase());
    }
}
