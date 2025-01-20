# This dockerfile is used when running development builds of the server
#
# It is necessary to make selenium work when the server is running on
# within docker on localhost - selenium runs on the jvm container, so
# localhost points at the spring server, whereas selenium needs to talk
# to the ingress httpd.
#
# We install redir, and then the --fintechlabs.startredir=true option
# to java is used to launch it
FROM eclipse-temurin:17

RUN apt-get update && apt-get install redir
