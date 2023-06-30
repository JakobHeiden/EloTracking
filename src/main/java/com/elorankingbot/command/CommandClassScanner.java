package com.elorankingbot.command;

import com.elorankingbot.command.annotations.*;
import com.elorankingbot.commands.MessageCommand;
import com.elorankingbot.commands.SlashCommand;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@CommonsLog
public class CommandClassScanner {

    /*
    Command class names are camel case, as per java convention.
    Discord slash command names must be lowercase.
    "Command strings" are either discord command names (for slash commands), or the first token of the event's custom id.
    To accommodate mapping to command classes, all command strings are lowercase.
    Message Commands differ: discord allows capitalization and spaces for their command names, and for UX reasons
    both are used in the command name. The command string is still lowercase for these.

    Java class names map to discord command names by going toLowerCase().
    Incoming events are mapped from command string to Java class name using CommandClassScanner.commandStringToFullClassName.
    */
    private Map<String, String> commandStringToFullClassName;
    @Getter
    // these are also used for setting permissions, which is currently out of order
    private Set<String> adminCommandHelpEntries, modCommandHelpEntries, playerCommandHelpEntries,
            globalCommandClassNames, ownerCommandClassNames, rankingCommandClassNames, queueCommandClassNames;
    private Set<Class> allMyClasses;

    public CommandClassScanner() throws IOException {
        allMyClasses = getAllMyClasses();
        gatherHelpEntries();
        commandStringToFullClassName = buildCommandStringToFullClassName();
        gatherCommandClassNamesForDiscordCommands();
    }

    private Set<Class> getAllMyClasses() throws IOException {
        return ClassPath.from(ClassLoader.getSystemClassLoader())
                .getAllClasses()
                .stream()
                .filter(classInfo -> classInfo.getPackageName().contains("com.elorankingbot.commands"))
                // for some reason GitHub needs this
                .filter(classInfo -> !classInfo.getPackageName().contains("target.classes"))
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
    }

    private void gatherHelpEntries() {
        adminCommandHelpEntries = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(AdminCommand.class) && superclassImpliesHelpEntry(clazz))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        adminCommandHelpEntries.forEach(className -> log.trace("admin command " + className));

        modCommandHelpEntries = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(ModCommand.class) && superclassImpliesHelpEntry(clazz))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        modCommandHelpEntries.forEach(className -> log.trace("mod command " + className));

        playerCommandHelpEntries = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(PlayerCommand.class) && superclassImpliesHelpEntry(clazz))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        playerCommandHelpEntries.forEach(className -> log.trace("player command " + className));
    }

    private ImmutableMap<String, String> buildCommandStringToFullClassName() {
        ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
        allMyClasses.forEach(clazz -> mapBuilder.put(clazz.getSimpleName().toLowerCase(), clazz.getName()));
        return mapBuilder.build();
    }

    private void gatherCommandClassNamesForDiscordCommands() {
        globalCommandClassNames = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(GlobalCommand.class))
                .map(clazz -> clazz.getSimpleName().toLowerCase().replace(" ", ""))
                .collect(Collectors.toSet());
        globalCommandClassNames.forEach(className -> log.trace("global command " + className));

        ownerCommandClassNames = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(OwnerCommand.class))
                .map(clazz -> clazz.getSimpleName().toLowerCase().replace(" ", ""))
                .collect(Collectors.toSet());
        ownerCommandClassNames.forEach(className -> log.trace("owner command " + className));

        rankingCommandClassNames = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(RankingCommand.class))
                .map(clazz -> clazz.getSimpleName().toLowerCase().replace(" ", ""))
                .collect(Collectors.toSet());
        rankingCommandClassNames.forEach(className -> log.trace("ranking command " + className));

        queueCommandClassNames = allMyClasses.stream()
                .filter(clazz -> clazz.isAnnotationPresent(QueueCommand.class))
                .map(clazz -> clazz.getSimpleName().toLowerCase().replace(" ", ""))
                .collect(Collectors.toSet());
        queueCommandClassNames.forEach(className -> log.trace("queue command " + className));
    }

    private boolean superclassImpliesHelpEntry(Class clazz) {
        return SlashCommand.class.isAssignableFrom(clazz) || MessageCommand.class.isAssignableFrom(clazz);
    }

    public String getFullClassName(String commandStringOrClassName) {
        return commandStringToFullClassName.get(commandStringOrClassName.toLowerCase());
    }
}
