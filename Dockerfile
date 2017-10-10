FROM gliderlabs/herokuish
COPY . /tmp/app/
RUN /bin/herokuish buildpack build

FROM openjdk
COPY --from=0 /app/target/fapi-test-suite.jar /server/
EXPOSE 8080
ENTRYPOINT java -jar /server/fapi-test-suite.jar
