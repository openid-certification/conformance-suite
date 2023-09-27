FROM debian:buster
RUN apt-get update \
	&& apt-get install -y apache2 ssl-cert \
	&& apt-get clean
RUN \
	a2enmod headers proxy proxy_http rewrite ssl\
	&& a2dissite 000-default.conf
COPY ports.conf /etc/apache2/ports.conf
COPY server.conf /etc/apache2/sites-enabled
ENTRYPOINT ["apachectl", "-DFOREGROUND"]
