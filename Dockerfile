FROM openjdk
COPY target/fapi-test-suite.jar /server/
ENV BASE_URL http://localhost:8080
ENV MONGODB_HOST mongodb
EXPOSE 8080
ENTRYPOINT java \
  -D"fintechlabs.base_url=${BASE_URL}" \
  -D"spring.data.mongodb.uri=mongodb://${MONGODB_HOST}:27017/test_suite" \
  -jar /server/fapi-test-suite.jar
