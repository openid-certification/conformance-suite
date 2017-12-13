# Deploying the test suite

## Docker Compose

Recommended for running the test suite locally.

1. You should have the two images `server-1.0.tar` and `ingress-1.0.tar` from the release package

2. Import the images into your local docker instance

        docker load -i server-1.0.tar
        docker load -i ingress-1.0.tar

3. Create a file `docker-compose.yml` as follows:

        version: '3'
        services:
          mongodb:
            image: mongo
            volumes:
            - ./mongo/data:/data/db
          server:
            image: server:1.0
            links:
            - mongodb:mongodb
            depends_on:
            - mongodb
          ingress:
            image: ingress:1.0
            ports:
            - "8443:8443"
            depends_on:
            - server

4. Run `docker-compose up`

5. After a few seconds the test server should be accessible at https://localhost:8443/

Note that the ingress uses a self-signed certificate. You will need to tell your browser to accept this.

## Kubernetes

**These are examples only. You will need to modify the commands for your particular environment.**

1. You should have the two images `server-1.0.tar` and `ingress-1.0.tar` from the release package

2. Import the images into your local docker instance

        docker load -i server-1.0.tar
        docker load -i ingress-1.0.tar

3. Tag and push the images to your remote repository

        docker tag server:1.0 repo.example.com/conformance-suite/server:1.0
        docker push repo.example.com/conformance-suite/server:1.0
Repeat for `ingress:1.0`.

4. Start the services running (including mongodb)

        kubectl run mongodb --image=mongo --expose=true
        kubectl run server --image=repo.example.com/conformance-suite/server:1.0 --env="BASE_URL=https://test-suite.example.com" --expose=true
        kubectl run ingress --image=repo.example.com/conformance-suite/server:1.0
        kubectl expose pod ingress --port=443 --target-port=8443 --type=LoadBalancer
For a permanent installation, you should create a deployment instead (see the Kubernetes manual).

5. Create a DNS entry for `test-suite.example.com` pointing to the new load balancer.

Note that the ingress uses a self-signed certificate. You will need to tell your browser to accept this.
