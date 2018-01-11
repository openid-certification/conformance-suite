FROM openjdk:9
COPY target/fapi-test-suite.jar /server/
ENV BASE_URL https://localhost:8443
ENV MONGODB_HOST mongodb
EXPOSE 8080
EXPOSE 9090
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  -D"oidc.google.clientid=${OIDC_GOOGLE_CLIENTID}" \
  -D"oidc.google.secret=${OIDC_GOOGLE_SECRET}" \
  -jar /server/fapi-test-suite.jar
