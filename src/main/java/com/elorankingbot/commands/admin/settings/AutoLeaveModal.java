package com.elorankingbot.commands.admin.settings;

import com.elorankingbot.commands.ModalSubmitCommand;
import com.elorankingbot.model.Server;
import com.elorankingbot.service.Services;
import com.elorankingbot.timedtask.DurationParser;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.TextInput;
import discord4j.discordjson.possible.Possible;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AutoLeaveModal extends ModalSubmitCommand {

    public static final String customId = AutoLeaveModal.class.getSimpleName().toLowerCase();
    public static final String variableName = "auto_leave_queues_after";

    public AutoLeaveModal(ModalSubmitInteractionEvent event, Services services) {
        super(event, services);
    }

    static void present(Server server, SelectMenuInteractionEvent event) {
        event.presentModal(
                String.format("Enter: %s. 0 for never", variableName),
                AutoLeaveModal.customId,
                textInputForSetAutoLeave(server)
        ).subscribe();
    }

    private static Collection<LayoutComponent> textInputForSetAutoLeave(Server server) {
        String currentAutoLeaveQueuesAfterAsString = server.getAutoLeaveQueuesAfter() == 0 ?
                "never"
                : DurationParser.minutesToString(server.getAutoLeaveQueuesAfter());
        return List.of(ActionRow.of(TextInput.small(
                "none",
                String.format("Current value: %s", currentAutoLeaveQueuesAfterAsString))));
    }

    protected void execute() {
        acknowledgeEvent();
        String value = event.getComponents(TextInput.class).get(0).getValue().get();
        Optional<Integer> maybeValueInMinutes = DurationParser.parse(value);
        if (maybeValueInMinutes.isEmpty()) {
            event.createFollowup("Please enter a valid duration. Examples: 90, 3h, 5d, 10w")
                    .withEphemeral(true).subscribe();
            event.getMessage().get().edit()
                    .withComponents(SelectServerVariableOrGame.menu(server), ActionRow.of(Exit.button())).subscribe();
            return;
        }

        server.setAutoLeaveQueuesAfter(maybeValueInMinutes.get());
        dbService.saveServer(server);
        String newAutoLeaveQueuesAfterAsString = server.getAutoLeaveQueuesAfter() == 0 ?
                "never"
                : DurationParser.minutesToString(server.getAutoLeaveQueuesAfter());
        String userFeedback = String.format("\n**Variable %s is now set to %s.**",
                variableName, newAutoLeaveQueuesAfterAsString);
        event.getMessage().get().edit()
                .withEmbeds(Settings.serverSettingsEmbed(server))
                .withContent(Possible.of(Optional.of(event.getMessage().get().getContent() + userFeedback)))
                .withComponents(SelectServerVariableOrGame.menu(server), ActionRow.of(Exit.button())).subscribe();
    }
}
