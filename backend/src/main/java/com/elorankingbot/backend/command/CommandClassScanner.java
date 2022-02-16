package com.elorankingbot.backend.command;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CommandClassScanner {
	/*
	Commands mit public static boolean isModCommand() versehen
	Permission baut sich seine listen anhand der map und isModCommand selber

	das ding mit der q nochmal anschauen und evtl umbauen
	 */
	@Getter
	private final Map<String, String> commandStringToClassName;
	@Getter
	private final Set<String> adminCommands, modCommands;

	public CommandClassScanner() throws IOException {
		Set<Class> classes = ClassPath.from(ClassLoader.getSystemClassLoader())
				.getAllClasses()
				.stream()
				.filter(clazz -> clazz.getPackageName()
						.contains("com.elorankingbot.backend.commands"))
				.map(classInfo -> classInfo.load())
				.filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
				// apparently some reflected class by Spring is getting caught up in this...
				.filter(clazz -> !clazz.getSimpleName().equals(""))
				.collect(Collectors.toSet());

		this.adminCommands = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(AdminCommand.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return false;
					}
				})
				.map(clazz -> clazz.getSimpleName())
				.collect(Collectors.toSet());
		this.modCommands = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(ModCommand.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return false;
					}
				})
				.map(clazz -> clazz.getSimpleName())
				.collect(Collectors.toSet());

		ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
		classes.forEach(clazz -> mapBuilder.put(clazz.getSimpleName().toLowerCase(), clazz.getName()));
		this.commandStringToClassName = mapBuilder.build();
	}
}
