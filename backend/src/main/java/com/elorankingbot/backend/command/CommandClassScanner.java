package com.elorankingbot.backend.command;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CommandClassScanner {

	// command strings are simple class names in lowercase, full class name is class name with package path
	private final Map<String, String> commandStringToFullClassName;
	@Getter
	private final Set<String> adminCommandClassNames, modCommandClassNames, playerCommandClassNames;

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
				.collect(Collectors.toSet());

		this.adminCommandClassNames = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(AdminCommand.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return false;
					}
				})
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		adminCommandClassNames.forEach(className -> log.trace("admin command " + className));
		this.modCommandClassNames = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(ModCommand.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return false;
					}
				})
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		modCommandClassNames.forEach(className -> log.trace("mod command " + className));
		this.playerCommandClassNames = classes.stream()
				.filter(clazz -> {
					try {
						return Class.forName(clazz.getName()).isAnnotationPresent(PlayerCommand.class);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						return false;
					}
				})
				.map(Class::getSimpleName)
				.collect(Collectors.toSet());
		playerCommandClassNames.forEach(className -> log.trace("player command " + className));

		ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
		classes.forEach(clazz -> mapBuilder.put(clazz.getSimpleName().toLowerCase(), clazz.getName()));
		this.commandStringToFullClassName = mapBuilder.build();
	}

	public Set<String> getAllCommandClassNames() {// TODO macht das hier alles sinn? class name vs command string etc
		Set<String> allCommandClassnames = new HashSet<>();
		allCommandClassnames.addAll(playerCommandClassNames);
		allCommandClassnames.addAll(modCommandClassNames);
		allCommandClassnames.addAll(adminCommandClassNames);
		return allCommandClassnames;
	}

	public String getFullClassName(String commandStringOrClassName) {
		return commandStringToFullClassName.get(commandStringOrClassName.toLowerCase());
	}
}
