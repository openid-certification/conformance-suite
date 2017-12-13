### Build instructions

1. Check out the code

2. Build the server image

        $ mvn clean package
        $ docker build --pull -f Dockerfile -t server:1.0 .
        $ docker save -o server-1.0.tar server:1.0

3. Build the ingress image

        $ docker build --pull -t ingress:1.0 httpd
        $ docker save -o ingress-1.0.tar ingress:1.0

4. Package up the two images, deployment instructions, release notes etc.
