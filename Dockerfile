FROM openjdk:15-oracle

ENV DISCORD_BOT_TOKEN=NzczOTMzMzMyMzk2MjQ1MDAz.X6QblQ.2EEhXKCUX-41VocHsAxD1tFlKsQ

LABEL maintainer="JakobHeiden@gmx.de"

ADD backend/target/EloTracking.jar app.jar
COPY backend/src/main/resources/config.txt config.txt

CMD [ "sh", "-c", "java -jar /app.jar" ]