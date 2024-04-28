FROM eclipse-temurin:17 as builder
WORKDIR workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} vitale-1.jar

RUN java -Djarmode=layertools -jar vitale-1.jar extract

FROM eclipse-temurin:17
RUN useradd spring
USER spring
WORKDIR workspace

COPY --from=builder workspace/dependencies/ ./
COPY --from=builder workspace/spring-boot-loader/ ./
COPY --from=builder workspace/snapshot-dependencies/ ./
COPY --from=builder workspace/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]











#FROM ubuntu:22.04
#
#RUN apt-get update && apt-get install -y default-jre
#
#ENTRYPOINT ["java", "--version"]