package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
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
    protected DiscordBot bot;
    protected final Message msg;
    protected final String channelId;
    @Getter
    protected final List<String> botReplies;
    protected boolean needsRegisteredChannel;
    protected boolean needsUserTag;

    protected Command(Message msg) {
        this.msg = msg;
        this.channelId = msg.getChannelId().asString();
        this.botReplies = new LinkedList<String>();

        this.needsRegisteredChannel = false;
        this.needsUserTag = false;
    }

    public abstract void execute();

    protected boolean canExecute() {
        boolean canExecute = true;
        if (this.needsRegisteredChannel) {
            if (service.findGameByChannelId(channelId).isEmpty()) {
                canExecute = false;
                botReplies.add("Needs register");
            }
        }
        if (this.needsUserTag) {
            if (msg.getUserMentionIds().size() != 1) {
                canExecute = false;
                botReplies.add("Needs user tag");
            }
        }
        return canExecute;
    }
}
