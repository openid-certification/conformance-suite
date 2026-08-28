FROM eclipse-temurin:21
COPY target/fapi-test-suite.jar /server/
ENV BASE_URL https://localhost:8443
ENV BASE_MTLS_URL https://localhost:8444
ENV MONGODB_HOST mongodb
ENV JAVA_EXTRA_ARGS=
# The IdP that logins go through is configured with OIDC_IDP_ISS, OIDC_IDP_CLIENTID,
# OIDC_IDP_SECRET and OIDC_IDP_ADMIN_ROLE. They need no -D flags below: Spring's
# relaxed binding resolves oidc.idp.* from the matching environment variables. The
# retired -Doidc.google.* / -Doidc.gitlab.* flags were redundant for the same
# reason, and were removed with those providers.
EXPOSE 8080
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"fintechlabs.base_mtls_url=${BASE_MTLS_URL}" \
  -D"spring.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  ${SIGNING_KEY:+-D"fintechlabs.signingKey=${SIGNING_KEY}"} \
  ${DEPRECATED_SIGNING_KEY:+-D"fintechlabs.deprecatedSigningKey=${DEPRECATED_SIGNING_KEY}"} \
  ${PRIVATE_LINK_SIGNING_KEY:+-D"fintechlabs.privateLinkSigningKey=${PRIVATE_LINK_SIGNING_KEY}"} \
  $JAVA_EXTRA_ARGS \
 -jar /server/fapi-test-suite.jar \
 -Djdk.tls.maxHandshakeMessageSize=65536

