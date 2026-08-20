FROM node:20-alpine

# Self-signed TLS material for the adapter's HTTPS listener, generated while the
# image is built so that no certificate or private key has to live in the
# repository. Regenerate simply by rebuilding the image:
#
#   docker compose -f docker-compose-localtest.yml build cerbos-authzen-adapter
#
# SANs cover both names the PDP is reached on: the compose service name
# (https://cerbos-authzen-adapter:9090, used by authzen-cerbos-test-config.json)
# and localhost/127.0.0.1 via the published port. The cert is self-signed, so
# clients still have to trust it explicitly or skip verification.
RUN apk add --no-cache openssl \
	&& mkdir -p /certs \
	&& openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
		-keyout /certs/key.pem -out /certs/cert.pem \
		-subj "/CN=cerbos-authzen-adapter/O=Cerbos Local" \
		-addext "subjectAltName=DNS:cerbos-authzen-adapter,DNS:localhost,IP:127.0.0.1" \
	&& chmod 600 /certs/key.pem

WORKDIR /app
COPY cerbos-authzen-adapter.js /app/cerbos-authzen-adapter.js

# Point the adapter at the baked-in pair; overriding either one in
# docker-compose (or unsetting it) makes the adapter serve plain HTTP instead.
ENV TLS_CERT=/certs/cert.pem \
	TLS_KEY=/certs/key.pem

EXPOSE 9090
CMD ["node", "/app/cerbos-authzen-adapter.js"]
