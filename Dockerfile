FROM openjdk:17-jdk-alpine3.14 as photon-builder

# Install pbzip2 for parallel extraction
RUN apk -U upgrade --update && \
    	apk --no-cache add maven && \
        rm -rf /var/cache/apk/*

WORKDIR /photon
COPY ./es /photon/es
COPY ./src /photon/src
COPY ./pom.xml /photon/pom.xml

RUN mvn -T 15 --no-transfer-progress package -Dmaven.test.skip=true

RUN ls -sahlS target/photon-*.jar

FROM openjdk:17-jdk-alpine3.14 as photon

RUN apk -U upgrade --update && \
    	apk --no-cache add sudo bzip2 bash && \
        rm -rf /var/cache/apk/*

RUN	adduser -D -s /bin/bash -h /photon photon && \
	echo 'photon ALL=(ALL) NOPASSWD:ALL' >>/etc/sudoers

USER photon:photon
WORKDIR /photon

COPY --from=photon-builder /photon/target/photon-*.jar /photon/
RUN sudo chown -R "$USER":root /photon

# Inspired by https://github.com/thomasnordquist/photon-docker
COPY --chown=photon:photon entrypoint.sh /photon/entrypoint.sh

VOLUME /photon/photon_data
EXPOSE 2322

RUN chmod +x /photon/entrypoint.sh

ENTRYPOINT /photon/entrypoint.sh