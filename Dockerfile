FROM openjdk:15-oracle

LABEL maintainer="JakobHeiden@gmx.de"

ADD backend/target/EloTracking.jar app.jar

CMD [ "sh", "-c", "java -jar /app.jar" ]