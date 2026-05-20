FROM gradle:8.14.4-jdk17@sha256:cd90533c9f7a5f28225af3076d58ea5ef569bfbc7eea4ae32ea7cdc58e642793 AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.6.2@sha256:e712055eab50036a06dda2921ee5158e4fdf8d362402ae3886cc0eb97012cffb AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
