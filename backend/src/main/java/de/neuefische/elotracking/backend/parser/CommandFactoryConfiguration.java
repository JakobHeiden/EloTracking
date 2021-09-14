package de.neuefische.elotracking.backend.parser;

import de.neuefische.elotracking.backend.command.Command;
import de.neuefische.elotracking.backend.command.Unknown;
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
	public Function<Message, Command> commandFactory() {
		return message -> createCommand(message);
	}

	@Bean
	@Scope("prototype")
	public Command createCommand(Message message) {
		String commandString = message.getContent().split(" ")[0].substring(1).toLowerCase();
		commandString = commandAbbreviationMapper.mapIfApplicable(commandString);
		String commandClassName = commandString.substring(0, 1).toUpperCase() + commandString.substring(1);
		try {
			return (Command) Class.forName("de.neuefische.elotracking.backend.command." + commandClassName)
					.getConstructor(Message.class)
					.newInstance(message);
		} catch (Exception e) {//TODO
			if (e.getClass().equals(ClassNotFoundException.class) || e.getClass().equals(NoSuchMethodException.class)) {
				return new Unknown(message);
			} else {
				e.printStackTrace();//TODO
				return null;
			}
		}
	}

}
