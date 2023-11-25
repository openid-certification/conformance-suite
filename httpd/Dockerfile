FROM debian:buster
RUN apt-get update \
	&& apt-get install -y apache2 ssl-cert \
	&& apt-get clean
RUN \
	echo 'Listen 8443' > /etc/apache2/ports.conf \
	&& a2enmod headers proxy proxy_http rewrite ssl \
	&& a2dissite 000-default.conf
RUN echo 'application/javascript js es mjs' > /etc/mime.types
COPY server.conf /etc/apache2/sites-enabled
ENTRYPOINT ["apachectl", "-DFOREGROUND"]
