FROM gradle:8.14.5-jdk17@sha256:d0c053f2371d09d9ace3ecd823ac7dac07e33280a4ab010fea23ea79107e8b17 AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.7.0@sha256:2eb3cd316835c990e69e26ade292ffa78f6fb0db7d5fc6377463c162e1979ac0 AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
