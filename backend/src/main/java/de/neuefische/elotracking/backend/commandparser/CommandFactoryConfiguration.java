package de.neuefische.elotracking.backend.commandparser;

import de.neuefische.elotracking.backend.commands.Command;
import de.neuefische.elotracking.backend.commands.Unknown;
import de.neuefische.elotracking.backend.service.DiscordBotService;
import de.neuefische.elotracking.backend.service.EloTrackingService;
import de.neuefische.elotracking.backend.timedtask.TimedTaskQueue;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.function.Function;

@Configuration
public class CommandFactoryConfiguration {

	@Autowired
	CommandAbbreviationMapper commandAbbreviationMapper;

	@Bean
	public Function<MessageWrapper, Command> commandFactory() {
		return msgWrapper -> createCommand(msgWrapper);
	}

	@Bean
	@Scope("prototype")
	public Command createCommand(MessageWrapper msgWrapper) {
		Message msg = msgWrapper.msg();
		String commandString = msg.getContent().split(" ")[0].substring(1).toLowerCase();
		commandString = commandAbbreviationMapper.mapIfApplicable(commandString);
		String commandClassName = commandString.substring(0, 1).toUpperCase() + commandString.substring(1);
		try {
			System.out.println(commandClassName);
			return (Command) Class.forName("de.neuefische.elotracking.backend.commands." + commandClassName)
					.getConstructor(Message.class, EloTrackingService.class, DiscordBotService.class, TimedTaskQueue.class)
					.newInstance(msg, msgWrapper.service(), msgWrapper.bot(), msgWrapper.queue());
		} catch (Exception e) {//TODO
			if (e.getClass().equals(ClassNotFoundException.class) || e.getClass().equals(NoSuchMethodException.class)) {
				return new Unknown(msg, msgWrapper.service(), msgWrapper.bot(), msgWrapper.queue());
			} else {
				e.printStackTrace();//TODO
				return null;
			}
		}
	}

}
