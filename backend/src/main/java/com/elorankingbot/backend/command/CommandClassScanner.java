package com.elorankingbot.backend.command;

import com.elorankingbot.backend.command.annotations.AdminCommand;
import com.elorankingbot.backend.command.annotations.ModCommand;
import com.elorankingbot.backend.command.annotations.PlayerCommand;
import com.elorankingbot.backend.commands.MessageCommand;
import com.elorankingbot.backend.commands.SlashCommand;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CommandClassScanner {

	/*
	Command class names are camel case, as per java convention.
	Discord slash command names must be lowercase.
	"Command strings" are either discord command names (for slash commands), or the first token of the event's custom id.
	To accommodate mapping to command classes, all command strings are lowercase.
	Message Commands differ: discord allows capitalization and spaces for their command names, and for UX reasons
	both are used in the command name. The command string is still lowercase for these.

	Java class names map to discord command names by going toLowerCase().
	Incoming events are mapped from command string to Java class name using this.commandStringToFullClassName.
	*/
	private final Map<String, String> commandStringToFullClassName;
	@Getter
	// these are also used for setting permissions, which is currently out of order
	private final Set<String> adminCommandHelpEntries, modCommandHelpEntries, playerCommandHelpEntries;

	public CommandClassScanner() throws IOException {
		Set<Class> allMyClasses = ClassPath.from(ClassLoader.getSystemClassLoader())
				.getAllClasses()
				.stream()
				.filter(classInfo -> classInfo.getPackageName().contains("com.elorankingbot.backend.commands"))
				// for some reason GitHub needs this
				.filter(classInfo -> !classInfo.getPackageName().contains("target.classes"))
				// for some reason Heroku needs this
				.map(classInfo -> {
					try {
						return Class.forName(classInfo.getName().replace("BOOT-INF.classes.", ""));
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return null;
					}
				})
				.filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
				// apparently some reflected class by Spring is getting caught up in this...
				.filter(clazz -> !clazz.getSimpleName().equals(""))
				.collect(Collectors.toSet());

		this.adminCommandHelpEntries = allMyClasses.stream()
				.filter(clazz -> clazz.isAnnotationPresent(AdminCommand.class) && superclassImpliesHelpEntry(clazz))
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		adminCommandHelpEntries.forEach(className -> log.trace("admin command " + className));
		this.modCommandHelpEntries = allMyClasses.stream()
				.filter(clazz -> clazz.isAnnotationPresent(ModCommand.class) && superclassImpliesHelpEntry(clazz))
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		modCommandHelpEntries.forEach(className -> log.trace("mod command " + className));
		this.playerCommandHelpEntries = allMyClasses.stream()
				.filter(clazz -> clazz.isAnnotationPresent(PlayerCommand.class) && superclassImpliesHelpEntry(clazz))
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		playerCommandHelpEntries.forEach(className -> log.trace("player command " + className));

		ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
		allMyClasses.forEach(clazz -> mapBuilder.put(clazz.getSimpleName().toLowerCase(), clazz.getName()));
		this.commandStringToFullClassName = mapBuilder.build();
	}

	private boolean superclassImpliesHelpEntry(Class clazz) {
		return SlashCommand.class.isAssignableFrom(clazz) || MessageCommand.class.isAssignableFrom(clazz);
	}

	public String getFullClassName(String commandStringOrClassName) {
		return commandStringToFullClassName.get(commandStringOrClassName.toLowerCase());
	}
}
