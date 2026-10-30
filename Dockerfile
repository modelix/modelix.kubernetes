FROM gradle:9.8.2-jdk21@sha256:85f8f572045bfb31874d66d7d8a2bd0c17d8cb9ad34b6a4d02f5d552b4b92c04 AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.8.1@sha256:79c623a22abacf3fe6c00a5fb30f5b7843d717b234033b80995891c06116d244 AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
