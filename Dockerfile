FROM openjdk:9-jre-slim
COPY target/fapi-test-suite.jar /server/
ENV BASE_URL https://localhost:8443
ENV MONGODB_HOST mongodb
ENV JAVA_EXTRA_ARGS=
EXPOSE 8080
EXPOSE 9090
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  -D"oidc.google.clientid=${OIDC_GOOGLE_CLIENTID}" \
  -D"oidc.google.secret=${OIDC_GOOGLE_SECRET}" \
  -D"oidc.gitlab.clientid=${OIDC_GITLAB_CLIENTID}" \
  -D"oidc.gitlab.secret=${OIDC_GITLAB_SECRET}" \
  -D"oauth.introspection_url=http://${MICROAUTH_HOST}:9001/introspect" \
  $JAVA_EXTRA_ARGS \
 -jar /server/fapi-test-suite.jar
