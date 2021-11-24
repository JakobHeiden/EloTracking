FROM openjdk:17-oracle

LABEL maintainer="JakobHeiden@gmx.de"

ADD backend/target/elotracking.jar app.jar

CMD [ "sh", "-c", "java -XX:MaxRAM=200m -jar -Dserver.port=$PORT /app.jar" ]
