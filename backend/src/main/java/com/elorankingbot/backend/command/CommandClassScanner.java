package com.elorankingbot.backend.command;

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

	@Getter
	private final Map<String, String> commandStringToClassName;
	@Getter
	private final Set<String> adminCommands, modCommands, playerCommands;

	public CommandClassScanner() throws IOException {
		Set<Class> classes = ClassPath.from(ClassLoader.getSystemClassLoader())
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
				.peek(classInfo -> log.trace("scanning command class " + classInfo.getSimpleName()))
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
		this.playerCommands = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(PlayerCommand.class);
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
