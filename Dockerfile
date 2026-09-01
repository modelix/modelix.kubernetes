FROM gradle:8.14.5-jdk17@sha256:d0c053f2371d09d9ace3ecd823ac7dac07e33280a4ab010fea23ea79107e8b17 AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.7.3@sha256:ff4257d0d64efbe99ed1ddfaf07765cc3c36dc7518bf8324d41961327f441c54 AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
