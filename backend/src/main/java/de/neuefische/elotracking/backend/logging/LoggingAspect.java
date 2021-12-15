package de.neuefische.elotracking.backend.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(public * *(..)) && within(de.neuefische..*)")
    public void onFunctionCall(JoinPoint joinPoint) {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());
        log.trace(String.format("call: %s(%s)",
                joinPoint.getSignature().getName(),
                formatParameters(joinPoint)));
    }

    @AfterReturning(pointcut = "execution(public * *(..)) && within(de.neuefische..*)", returning = "returnValue")
    public void onFunctionReturn(JoinPoint joinPoint, Object returnValue) {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());
        log.trace(String.format("return: %s(%s) => %s",
                joinPoint.getSignature().getName(),
                formatParameters(joinPoint),
                getStringRepresentation(returnValue)));
    }

    private static String formatParameters(JoinPoint joinPoint) {
        if (joinPoint.getSignature().toString().contains("Function")) {
            return "";// hack to circumvent crashes in relation to CommandFactoryConfiguration.commandFactory
        }             // I have no idea why or how.

        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        StringJoiner joiner = new StringJoiner(", ");
        for (int n = 0; n < paramValues.length; n++) {
            joiner.add(paramNames[n] + ": " + getStringRepresentation(paramValues[n]));
        }
        return joiner.toString();
    }

    private static String getStringRepresentation(Object value) {
        if (value == null)// TODO void
            return "NULL";

        if (value instanceof Optional) {
            if (((Optional) value).isPresent()) {
                return "Optional:" + getStringRepresentation(((Optional) value).get());
            } else {
                return "Optional:empty";
            }
        }

        if (value.getClass().isAnnotationPresent(UseToStringForLogging.class)) {
            return value.toString();
        }

        if (Collection.class.isAssignableFrom(value.getClass())) {
            return "Collection with size " + ((Collection<?>) value).size();
        }

        if (hasSimpleStringRepresentation(value.getClass())) {
            return value.toString();
        }

        return buildStringFromClassName(value);
    }

    private static String buildStringFromClassName(Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(value.getClass().getSimpleName());
        if (value.getClass().isArray()) {
            sb.deleteCharAt(sb.length() - 1);
            sb.append(((Object[]) value).length).append("]");
        }
        sb.append(">");
        return sb.toString();
    }

    private static boolean hasSimpleStringRepresentation(Class<?> paramClass) {
        return paramClass.isPrimitive() || String.class.isAssignableFrom(paramClass)
                || Boolean.class.isAssignableFrom(paramClass) || Number.class.isAssignableFrom(paramClass);
    }
}
