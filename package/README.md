# FAPI Conformance Suite

## Running locally

1. Unzip the archive
2. Run `docker-compose up`
3. After a few seconds the test server should be accessible at https://localhost:8443/

Note that the ingress uses a self-signed certificate. You will need to tell your browser to accept this.

## Using a custom certificate

1. Place the certificate and key in the `nginx/` directory

2. Modify `nginx/Dockerfile` to include your custom certificate and key in the image (see the comments in that file)

3. Modify the `ssl_certificate` and `ssl_certificate_key` lines in `nginx/nginx.conf` to refer to your custom certificate

4. Run `docker-compose build nginx` to update the image
