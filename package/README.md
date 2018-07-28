# FAPI Conformance Suite

## Running locally

1. Unzip the archive
2. Run `docker-compose up`
3. After a few seconds the test server should be accessible at https://localhost:8443/

Note that the ingress uses a self-signed certificate. You will need to tell your browser to accept this.

## Using a custom certificate

1. Place the certificate and key in the `httpd/` directory

2. Modify `httpd/Dockerfile` to include your custom certificate and key in the image (see the comments in that file)

3. Modify the `SSLCertificateFile` and `SSLCertificateKeyFile` lines in `httpd/server.conf` to refer to your custom certificate

4. Run `docker-compose build http` to update the image
