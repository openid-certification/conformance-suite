# This dockerfile is used when running development builds of the server
#
# It is necessary to make selenium work when the server is running on
# localhost - selenium runs on the jvm, so localhost points at the
# spring server, whereas selenium needs to talk to the ingress httpd
FROM openjdk:9-jdk-slim

RUN apt-get update && apt-get install redir
