package de.neuefische.elotracking.backend.commands;

import de.neuefische.elotracking.backend.model.Game;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

// Subclasses must start with a capital letter and have no other capital letters to be recognized by the parser
// Bot replies are processed in the parser
public abstract class Command {

    protected String defaultCommandPrefix;
    protected EloTrackingService service;
    protected DiscordBotService bot;
    protected TimedTaskQueue queue;
    protected final Message msg;
    protected final String channelId;
    protected Game game;
    @Getter
    private final List<String> botReplies;
    protected boolean needsRegisteredChannel = false;
    protected boolean needsMention = false;
    protected boolean cantHaveTwoMentions = false;

    protected Command(Message msg, EloTrackingService service, DiscordBotService bot, TimedTaskQueue queue) {
        this.msg = msg;
        this.service = service;
        this.bot = bot;
        this.queue = queue;
        this.channelId = msg.getChannelId().asString();
        this.botReplies = new LinkedList<String>();
        this.defaultCommandPrefix = service.getPropertiesLoader().getDefaultCommandPrefix();
    }

    public abstract void execute();

    protected boolean canExecute() {
        boolean canExecute = true;
        if (this.needsRegisteredChannel) {
            Optional<Game> maybeGame = service.findGameByChannelId(channelId);
            if (maybeGame.isEmpty()) {
                canExecute = false;
                addBotReply("Needs register");
            } else {
                this.game = maybeGame.get();
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
