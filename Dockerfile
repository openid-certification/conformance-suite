# Extract the Spring Boot jar so classes load from regular filesystem jars
# instead of Boot's nested-jar handler, which JFR showed as a major source of
# lock contention during cold start. Multi-stage so the fat jar isn't in the
# final image.
FROM eclipse-temurin:21 AS extract
COPY target/fapi-test-suite.jar /tmp/
RUN java -Djarmode=tools -jar /tmp/fapi-test-suite.jar extract --destination /server

FROM eclipse-temurin:21
COPY --from=extract /server /server
ENV BASE_URL https://localhost:8443
ENV BASE_MTLS_URL https://localhost:8444
ENV MONGODB_HOST mongodb
ENV JAVA_EXTRA_ARGS=
EXPOSE 8080
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"fintechlabs.base_mtls_url=${BASE_MTLS_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  ${SIGNING_KEY:+-D"fintechlabs.signingKey=${SIGNING_KEY}"} \
  ${DEPRECATED_SIGNING_KEY:+-D"fintechlabs.deprecatedSigningKey=${DEPRECATED_SIGNING_KEY}"} \
  ${PRIVATE_LINK_SIGNING_KEY:+-D"fintechlabs.privateLinkSigningKey=${PRIVATE_LINK_SIGNING_KEY}"} \
  -D"oidc.google.clientid=${OIDC_GOOGLE_CLIENTID}" \
  -D"oidc.google.secret=${OIDC_GOOGLE_SECRET}" \
  -D"oidc.gitlab.clientid=${OIDC_GITLAB_CLIENTID}" \
  -D"oidc.gitlab.secret=${OIDC_GITLAB_SECRET}" \
  -Djdk.tls.maxHandshakeMessageSize=65536 \
  $JAVA_EXTRA_ARGS \
 -jar /server/fapi-test-suite.jar

