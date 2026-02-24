# ---- build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- runtime stage (Java + Node + Copilot CLI)
FROM eclipse-temurin:21-jre

# instalacja node + npm (Debian-based)
RUN apt-get update \
  && apt-get install -y --no-install-recommends nodejs npm ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# Copilot CLI (wymagane przez copilot-sdk-java)
RUN npm i -g @github/copilot@latest

WORKDIR /app
COPY --from=build /src/target/*.jar /app/app.jar

ENV JAVA_OPTS=""
EXPOSE 8788
ENTRYPOINT ["sh","-lc","java $JAVA_OPTS -jar /app/app.jar"]