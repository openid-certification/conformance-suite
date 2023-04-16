FROM debian:bullseye
RUN apt-get update \
	&& apt-get install -y apache2 ssl-cert \
	&& apt-get clean
RUN \
	echo 'Listen 8443' > /etc/apache2/ports.conf \
	&& a2enmod headers proxy proxy_http rewrite ssl http2 \
	&& a2dissite 000-default.conf
COPY server.conf /etc/apache2/sites-enabled
ENTRYPOINT ["apachectl", "-DFOREGROUND"]
