package com.elorankingbot.logging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface UseToStringForLogging {
	// used in LoggingAspect::getStringRepresentation
}
