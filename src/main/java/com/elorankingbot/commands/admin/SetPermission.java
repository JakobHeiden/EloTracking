package com.elorankingbot.commands.admin;

import com.elorankingbot.command.CommandClassScanner;
import com.elorankingbot.command.annotations.AdminCommand;
import com.elorankingbot.command.annotations.GlobalCommand;
import com.elorankingbot.commands.SlashCommand;
import com.elorankingbot.service.Services;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

import java.util.Set;

@AdminCommand
@GlobalCommand
public class SetPermission extends SlashCommand {

    private final CommandClassScanner commandClassScanner;
    private Role role;

    public SetPermission(ChatInputInteractionEvent event, Services services) {
        super(event, services);
        commandClassScanner = services.commandClassScanner;
    }

    public static ApplicationCommandRequest getRequest() {
        return ApplicationCommandRequest.builder()
                .name(SetPermission.class.getSimpleName().toLowerCase())
                .description(getShortDescription())
                .defaultPermission(true)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("adminormod").description("Link admin or moderator permissions to a role?")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("admin").value("admin").build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name("moderator").value("moderator").build())
                        .required(true)
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("role").description("Link elo admin or moderator permissions to this role")
                        .type(ApplicationCommandOption.Type.ROLE.getValue())
                        .required(true).build())
                .build();
    }

    public static String getShortDescription() {
        return "Link elo permissions to a role.";
    }

    public static String getLongDescription() {
        return getShortDescription() + "\n" +
                "`Required:` `adminormod` Whether to set the admin role, or the moderator role.\n" +
                "`Required:` `role` The role to link permissions to.\n" +
                "Moderator commands are commands related to the day-to-day operation of the bot and include things like " +
                "dispute resolution, bans, and player rating manipulation.\n" +
                "Admin commands are those that alter the settings of the bot. Admin permissions include moderator permissions. " +
                "Deleting the admin role will make /setrole available to @everyone, so you cannot lock yourself out of " +
                "admin permissions.";
    }

    protected void execute() {
        role = event.getOption("role").get().getValue().get().asRole().block();
        if (role.isManaged()) {
            event.reply("This role is managed by an integration and cannot be chosen for permissions. " +
                    "Usually this means that this role is a bot role. Please choose a different role.").subscribe();
            return;
        }
        String adminOrMod = event.getOption("adminormod").get().getValue().get().asString();
        if (adminOrMod.equals("admin") && !hasUserAccessToRole()) {
            event.reply("Cannot set admin permissions to a role that you do not have access to." +
                    " This is so you cannot lock yourself of admin permissions." +
                    " Please select a role that you either hold, or that you have permission to assign.").subscribe();
            return;
        }

        Set<String> adminCommands = commandClassScanner.getAdminCommandHelpEntries();
        Set<String> modCommands = commandClassScanner.getModCommandHelpEntries();
        if (adminOrMod.equals("admin")) {
            server.setAdminRoleId(role.getId().asLong());
        }
        if (adminOrMod.equals("moderator")) {
            server.setModRoleId(role.getId().asLong());
        }
        String nameOfRole = role.getName();
        dbService.saveServer(server);

        event.reply(String.format("Linked %s permissions to %s. This may take a minute to update on the server.",
                adminOrMod, nameOfRole)).subscribe();
    }

    private boolean hasUserAccessToRole() {
        Member member = activeUser.asMember(guildSnowflake).block();
        PermissionSet permissions = member.getBasePermissions().block();
        if (permissions.contains(Permission.ADMINISTRATOR)) return true;
        if (member.getRoleIds().contains(role.getId())) return true;
        if (permissions.contains(Permission.MANAGE_ROLES)
                && member.getHighestRole().block().getRawPosition() >= role.getRawPosition())
            return true;

        return false;
    }
}
