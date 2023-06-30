package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.FormatTools;
import com.elorankingbot.command.DiscordCommandManager;
import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.commands.ModalSubmitCommand;
import com.elorankingbot.model.Game;
import com.elorankingbot.model.MatchFinderQueue;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;
import discord4j.discordjson.possible.Possible;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.elorankingbot.model.MatchFinderQueue.NO_LIMIT;

@AdminCommand
public class QueueVariableModal extends ModalSubmitCommand {

    private final DiscordCommandManager discordCommandManager;
    public static final String customId = QueueVariableModal.class.getSimpleName().toLowerCase();
    private static final String mustBeInteger = "Please enter a whole number.";
    private static final String mustBePositive = "Please enter a positive number.";

    public QueueVariableModal(ModalSubmitInteractionEvent event, Services services) {
        super(event, services);
        this.discordCommandManager = services.discordCommandManager;
    }

    static void present(Server server, SelectMenuInteractionEvent event) {
        String gameAndQueue = event.getValues().get(0).split(":")[0];
        String selectedVariable = event.getValues().get(0).split(":")[1];
        Game game = server.getGame(gameAndQueue.split(",")[0]);
        MatchFinderQueue queue = game.getQueue(gameAndQueue.split(",")[1]);
        event.presentModal(selectedVariable.equals("max-rating-spread")
                        ? "New value, -1 for no limit:" :
                        String.format("Enter a new value for: %s", selectedVariable),
                String.format("%s:%s,%s:%s", customId, queue.getGame().getName(), queue.getName(), selectedVariable),
                textInputForSetQueueVariable(queue, selectedVariable)
        ).subscribe();
    }

    private static Collection<LayoutComponent> textInputForSetQueueVariable(MatchFinderQueue queue, String selectedVariable) {
        String currentValueAsString = getValueFromQueue(queue, selectedVariable);
        return List.of(ActionRow.of(TextInput.small(
                "no custom id",
                String.format("Current value: %s", currentValueAsString))));
    }

    private static String getValueFromQueue(MatchFinderQueue queue, String variableName) {
        switch (variableName) {
            case "Name" -> {
                return queue.getName();
            }
            case "K" -> {
                return String.valueOf(queue.getK());
            }
            case "max-rating-spread" -> {
                return queue.getMaxRatingSpreadAsString();
            }
            case "rating-elasticity" -> {
                return String.valueOf(queue.getRatingElasticity());
            }
            default -> {
                return "error";
            }
        }
    }

    protected void execute() throws Exception {
        String gameAndQueueName = event.getCustomId().split(":")[1];
        String gameName = gameAndQueueName.split(",")[0];
        String queueName = gameAndQueueName.split(",")[1];
        String variableName = event.getCustomId().split(":")[2];
        String value = event.getComponents(TextInput.class).get(0).getValue().get();

        Game game = server.getGame(gameName);
        MatchFinderQueue queue = game.getQueue(queueName);
        Optional<String> maybeErrorMessage = setValue(queue, variableName, value);
        queue.setGame(game);
        System.out.println(queue.getGame().getName());
        String userFeedback;
        if (maybeErrorMessage.isPresent()) {
            userFeedback = String.format("\nError: %s -> %s: %s", variableName, value, maybeErrorMessage.get());
        } else {
            dbService.saveServer(server);
            if (variableName.equals("Name")) {
                discordCommandManager.updateQueueCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));
            }
            if (variableName.equals("max-rating-spread") && value.equals("-1")) value = "no limit";
            userFeedback = String.format("\n**Variable %s for queue %s is now set to %s.**", variableName, queueName, value);
        }
        event.getMessage().get().edit()
                .withContent(Possible.of(Optional.of(event.getMessage().get().getContent() + userFeedback)))
                .withEmbeds(SelectGameVariableOrQueue.queueSettingsEmbed(queue))
                .withComponents(SelectQueueVariable.menu(queue), ActionRow.of(Exit.button(), EscapeToGameMenu.button(game))).subscribe();
        acknowledgeEvent();
    }

    private Optional<String> setValue(MatchFinderQueue queue, String variableName, String value) {
        switch (variableName) {
            case "Name" -> {
                if (!FormatTools.isLegalDiscordName(value)) {
                    return Optional.of(FormatTools.illegalNameMessage());
                }
                queue.getGame().getQueueNameToQueue().remove(queue.getName());
                queue.setName(value);
                queue.getGame().getQueueNameToQueue().put(queue.getName(), queue);
                discordCommandManager.updateQueueCommands(server, exceptionHandler.updateCommandFailedCallbackFactory(event));
                return Optional.empty();
            }
            case "K" -> {
                int newK;
                try {
                    newK = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return Optional.of(mustBeInteger);
                }
                if (newK < 0) {
                    return Optional.of(mustBePositive);
                }
                queue.setK(newK);
                return Optional.empty();
            }
            case "max-rating-spread" -> {
                int maxRatingSpread;
                try {
                    maxRatingSpread = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return Optional.of(mustBeInteger);
                }
                if (maxRatingSpread < -1) {
                    return Optional.of(mustBePositive.replace(".", "") + ", or -1.");
                }
                queue.setMaxRatingSpread(maxRatingSpread == 0 ? NO_LIMIT : maxRatingSpread);
                return Optional.empty();
            }
            case "rating-elasticity" -> {
                int ratingElasticity;
                try {
                    ratingElasticity = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return Optional.of(mustBeInteger);
                }
                if (ratingElasticity < 0) {
                    return Optional.of(mustBePositive);
                } else {
                    queue.setRatingElasticity(ratingElasticity);
                    return Optional.empty();
                }
            }
            default -> {
                return Optional.of("Variable not recognized.");
            }
        }
    }
}
