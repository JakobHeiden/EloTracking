package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.commands.SlashCommand;
import de.neuefische.elotracking.backend.commands.Unknown;
import de.neuefische.elotracking.backend.configuration.CommandAbbreviationMapper;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
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
	public Function<EventWrapper, SlashCommand> slashCommandFactory() {
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
				return new Unknown(msg, msgWrapper.service(), msgWrapper.bot(), msgWrapper.queue());
			} else {
				e.printStackTrace();//TODO
				return null;
			}
		}
	}

	@Bean
	@Scope("prototype")
	public SlashCommand createCommand(EventWrapper eventWrapper) {
		ChatInputInteractionEvent event = eventWrapper.event();
		String commandClassName = event.getCommandName();
		commandClassName = commandClassName.substring(0, 1).toUpperCase() + commandClassName.substring(1);
		log.trace("commandString = " + commandClassName);
		try {
			return (SlashCommand) Class.forName("de.neuefische.elotracking.backend.commands.Slash" + commandClassName)
					.getConstructor(ChatInputInteractionEvent.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
					.newInstance(event, eventWrapper.service(), eventWrapper.bot(), eventWrapper.queue());
		} catch (Exception e) {
			eventWrapper.bot().sendToAdmin(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
