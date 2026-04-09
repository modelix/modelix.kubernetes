FROM gradle:8.14.4-jdk17@sha256:d2fd8171c6bd2740b32f397257d0822d88bb833fa4d2401feb3fe6cd3ddbd3dc AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.5.7@sha256:45ae20191531eb608ddb0b775d012b40d3e4f942697f3214694887dd7c108d13 AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
