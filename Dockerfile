FROM maven AS build-image

WORKDIR /app
COPY src ./src
COPY pom.xml .

RUN mvn package -Dmaven.test.skip=true

FROM openjdk AS runtime-image

COPY --from=build-image /app/target/photon-*.jar /app/photon.jar

EXPOSE 2322

ENTRYPOINT ["java", "-jar", "/app/photon.jar"]