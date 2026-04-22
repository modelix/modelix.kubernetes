FROM gradle:9.4.1-jdk17@sha256:549ab76a04fc532f37945d689207a3fb2642350b1235901eb652c93dae218dd0 AS builder

COPY ./ /project
RUN cd /project && gradle :keycloak-extensions:assemble

FROM quay.io/keycloak/keycloak:26.6.0@sha256:b0e5dbced1775de4d629f103c0a9cfc057decc62ce8d3cb1c54f8849a6c6eb62 AS keycloak

WORKDIR /opt/keycloak
COPY --from=builder /project/keycloak-extensions/build/libs/keycloak-extensions.jar /opt/keycloak/providers/org.modelix.keycloak.extensions.jar

# These variables are required here, because keycloak is started with the --optimized option
ENV KC_HEALTH_ENABLED="true"
ENV KC_DB="postgres"

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
