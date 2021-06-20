package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;

public abstract class Command {

    @Autowired
    protected EloTrackingService service;
    @Autowired
    protected DiscordBotService bot;
    protected final Message msg;
    protected final String channelId;
    @Getter
    private final List<String> botReplies;
    protected boolean needsRegisteredChannel;
    protected boolean needsMention;
    protected boolean cantHaveTwoMentions;

    protected Command(Message msg) {
        this.msg = msg;
        this.channelId = msg.getChannelId().asString();
        this.botReplies = new LinkedList<String>();

        this.needsRegisteredChannel = false;
        this.needsMention = false;
        this.cantHaveTwoMentions = false;
    }

    public abstract void execute();

    protected boolean canExecute() {
        boolean canExecute = true;
        if (this.needsRegisteredChannel) {
            if (service.findGameByChannelId(channelId).isEmpty()) {
                canExecute = false;
                addBotReply("Needs register");
            }
        }
        if (this.needsMention) {
            if (msg.getUserMentionIds().size() != 1) {
                canExecute = false;
                addBotReply("Needs user tag");
            }
        }
        if (this.cantHaveTwoMentions) {
            if (msg.getUserMentionIds().size() > 1) {
                canExecute = false;
                addBotReply("You cannot mention more than one player with this command");
            }
        }
        return canExecute;
    }

    protected void addBotReply(String reply) {
        botReplies.add(reply);
    }
}
