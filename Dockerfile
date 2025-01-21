FROM eclipse-temurin:17
COPY target/fapi-test-suite.jar /server/
ENV BASE_URL https://localhost:8443
ENV BASE_MTLS_URL https://localhost:8444
ENV MONGODB_HOST mongodb
ENV JAVA_EXTRA_ARGS=
EXPOSE 8080
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"fintechlabs.base_mtls_url=${BASE_MTLS_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  ${JWKS:+-D"fintechlabs.jwks=${JWKS}"} \
  ${SIGNING_KEY:+-D"fintechlabs.signingKey=${SIGNING_KEY}"} \
  -D"oidc.google.clientid=${OIDC_GOOGLE_CLIENTID}" \
  -D"oidc.google.secret=${OIDC_GOOGLE_SECRET}" \
  -D"oidc.gitlab.clientid=${OIDC_GITLAB_CLIENTID}" \
  -D"oidc.gitlab.secret=${OIDC_GITLAB_SECRET}" \
  $JAVA_EXTRA_ARGS \
 -jar /server/fapi-test-suite.jar \
 -Djdk.tls.maxHandshakeMessageSize=65536

