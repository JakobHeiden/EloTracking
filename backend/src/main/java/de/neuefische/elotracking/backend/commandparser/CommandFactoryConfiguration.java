package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.configuration.CommandAbbreviationMapper;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Slf4j
@Configuration
public class CommandFactoryConfiguration {

	@Autowired
    CommandAbbreviationMapper commandAbbreviationMapper;

	@Bean
	public Function<MessageWrapper, Command> commandFactory() {
		return msgWrapper -> createCommand(msgWrapper);
	}

	@Bean
	public Function<EventWrapper, Command> slashCommandFactory() {
		return eventWrapper -> createCommand(eventWrapper);
	}

	@Bean
	@Scope("prototype")
	public Command createCommand(MessageWrapper msgWrapper) {
		Message msg = msgWrapper.msg();
		String commandString = msg.getContent().split(" ")[0].substring(1).toLowerCase();
		log.trace("commandString = " + commandString);
		commandString = commandAbbreviationMapper.mapIfApplicable(commandString);
		String commandClassName = commandString.substring(0, 1).toUpperCase() + commandString.substring(1);
		try {
			return (Command) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
					.getConstructor(Message.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
					.newInstance(msg, msgWrapper.service(), msgWrapper.bot(), msgWrapper.queue());
		} catch (Exception e) {//TODO
			if (e.getClass().equals(ClassNotFoundException.class)) {
				return null;//new Unknown(msg, msgWrapper.service(), msgWrapper.bot(), msgWrapper.queue());
			} else {
				e.printStackTrace();//TODO
				return null;
			}
		}
	}

	@Bean
	@Scope("prototype")
	public Command createCommand(EventWrapper eventWrapper) {
		Event event = eventWrapper.getEvent();
		String commandClassName = "commandStringNotSet";
		if (event instanceof ApplicationCommandInteractionEvent) {
			commandClassName = ((ApplicationCommandInteractionEvent) event).getCommandName();
		}
		if (event instanceof ReactionAddEvent) {
			commandClassName = eventWrapper.getCommandString();
		}
		commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
		log.trace("commandString = " + commandClassName);
		try {
			return (Command) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
					.getConstructor(Event.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
					.newInstance(event, eventWrapper.getService(), eventWrapper.getBot(), eventWrapper.getQueue());
		} catch (Exception e) {
			eventWrapper.getBot().sendToAdmin(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
