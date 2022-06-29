package com.elorankingbot.backend.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// TODO das hier weg. die generalisierung fuer Help und MessageCommand auch weg. stattdessen help-eintraege generalisiert
// nur fuer slashcommands, fuer alle anderen nach bedarf mit @HelpEntry
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHelpEntry {
}
