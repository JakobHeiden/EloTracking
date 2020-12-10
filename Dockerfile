FROM openjdk:15-oracle

LABEL maintainer="JakobHeiden@gmx.de"

ADD backend/target/elotracking.jar app.jar

CMD [ "sh", "-c", "java -Xms256m -Xmx256m -jar -Dserver.port=$PORT /app.jar" ]