package de.neuefische.elotracking.backend.command;

import de.neuefische.elotracking.backend.discord.DiscordBot;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public abstract class Command {
    protected EloTrackingService service;
    protected DiscordBot bot;
    protected Message msg;
    protected Channel channel;
    protected boolean canExecute;
    @Getter
    protected List<String> botReplies;
    protected boolean needsRegisteredChannel;
    protected boolean needsUserTag;

    protected Command(DiscordBot bot, EloTrackingService service, Message msg, Channel channel) {
        this.bot = bot;
        this.msg = msg;
        this.channel = channel;
        this.botReplies = new LinkedList<String>();
        this.service = service;
        this.canExecute = true;

        needsRegisteredChannel = false;
        needsUserTag = false;
    }

    protected void determineIfCanExecute() {
        if (this.needsRegisteredChannel) {
            if(!service.channelHasGameRegistered(channel.getId().asString())) {
                this.canExecute = false;
                botReplies.add("Needs register");
            }
        }
        if (this.needsUserTag) {
            if(msg.getUserMentionIds().size() != 1) {
                this.canExecute = false;
                botReplies.add("Needs user tag");
            }
        }
    }
}
