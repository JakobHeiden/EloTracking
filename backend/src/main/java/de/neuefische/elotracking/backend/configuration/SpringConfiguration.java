package de.neuefische.elotracking.backend.configuration;

import de.neuefische.elotracking.backend.command.Command;
import de.neuefische.elotracking.backend.command.Unknown;
import de.neuefische.elotracking.backend.parser.CommandAbbreviationMapper;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.text.SimpleDateFormat;
import java.util.function.Function;

@Slf4j
@Configuration
public class SpringConfiguration {

    @Autowired
    CommandAbbreviationMapper commandAbbreviationMapper;

    @Bean
    public GatewayDiscordClient createClient() {
        GatewayDiscordClient client = DiscordClientBuilder
                .create(System.getenv("DISCORD_BOT_TOKEN"))
                .build()
                .login()
                .block();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    log.info("Logged in as {}#{}", self.getUsername(), self.getDiscriminator());
                });

        return client;
    }

    @Bean
    public Function<Message, Command> commandFactory() {
        return message -> createCommand(message);
    }

    @Bean
    @Scope("prototype")
    public Command createCommand(Message message) {
        String commandString = message.getContent().split(" ")[0].substring(1).toLowerCase();
        commandString = commandAbbreviationMapper.mapIfApplicable(commandString);
        String commandClassName = commandString.substring(0,1).toUpperCase() + commandString.substring(1);
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

    @Bean
    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd--HH-mm-ss");
    }
}
