FROM openjdk:11-jdk-slim
COPY fapi-test-suite.jar /server/
#ENV BASE_URL https://localhost:8443
ENV BASE_URL https://web.conformance.directory.openbankingbrasil.org.br
ENV MONGODB_HOST mongodb
EXPOSE 8080
EXPOSE 9090
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  -D"oidc.google.clientid=${OIDC_GOOGLE_CLIENTID}" \
  -D"oidc.google.secret=${OIDC_GOOGLE_SECRET}" \
  -D"oidc.gitlab.clientid=${OIDC_GITLAB_CLIENTID}" \
  -D"oidc.gitlab.secret=${OIDC_GITLAB_SECRET}" \
  -D"oidc.obbrazil.clientid=${OIDC_OBBRAZIL_CLIENTID}" \
  -D"oidc.obbrazil.secret=${OIDC_OBBRAZIL_SECRET}" \
  -jar /server/fapi-test-suite.jar
