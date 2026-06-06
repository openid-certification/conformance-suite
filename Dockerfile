FROM eclipse-temurin:21
COPY target/fapi-test-suite.jar /server/
# Run the app "unpacked": explode the Spring Boot jar so classes load from real
# files via the standard (parallel-capable) system class loader instead of
# Spring Boot's nested-jar LaunchedClassLoader. JFR profiling of CI cold start
# measured that nested-jar loader (JarUrlClassLoader / NestedJarFile /
# UrlJarFiles$Cache) serialising concurrent class loads as a top lock-contention
# source. Launched below via -cp + main class rather than -jar.
RUN cd /server && jar xf fapi-test-suite.jar && rm fapi-test-suite.jar
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
  $JAVA_EXTRA_ARGS \
 -cp "/server/BOOT-INF/classes:/server/BOOT-INF/lib/*" \
 net.openid.conformance.Application \
 -Djdk.tls.maxHandshakeMessageSize=65536

