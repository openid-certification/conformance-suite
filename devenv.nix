{ pkgs, lib, config, inputs, ... }:

{
  env.GREET = "OIDF - Conformance - Local dev env";

  packages = [
    pkgs.git
    pkgs.ngrok
  ];

  scripts.hello.exec = "echo $GREET";

  enterShell = ''
    hello

    export EXTERNAL_URL=`curl -s localhost:4040/api/tunnels | jq -r ".tunnels[0].public_url"`

    echo "In order to run CIBA tests, please make sure ngrok account is created"

  '';

  dotenv.enable = true;
  certificates = [
    "localhost.emobix.co.uk"
  ];

  hosts."localhost.emobix.co.uk" = "127.0.0.1";

  # todo integrate the building, starting conformance and running the CI tests
  enterTest = ''
  '';

  processes = {
    ngrok = {
      exec = "${pkgs.ngrok}/bin/ngrok http https://localhost.emobix.co.uk:8443 --log stdout";
    };
  };

  services.mongodb.enable = true;
  services.nginx = {
    enable = true;
        httpConfig = ''
            ssl_protocols       TLSv1.3;
            ssl_prefer_server_ciphers on;

            ssl_certificate     ${config.env.DEVENV_STATE}/mkcert/localhost.emobix.co.uk.pem;
            ssl_certificate_key ${config.env.DEVENV_STATE}/mkcert/localhost.emobix.co.uk-key.pem;

            server {
                listen 8443 ssl;
                server_name localhost.emobix.co.uk;
                ssl_verify_client   off;

                location / {
                    proxy_pass http://127.0.0.1:8080;
                    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-Proto $scheme;
                    proxy_set_header X-Forwarded-Host $host;
                    proxy_set_header X-Forwarded-Port 8443;
                    proxy_set_header X-Forwarded-Uri $request_uri;
                    proxy_set_header X-Ssl-Cipher $ssl_cipher;
                    proxy_set_header X-Ssl-Protocol $ssl_protocol;

                    add_header 'Access-Control-Allow-Origin' '*';
                    add_header 'Access-Control-Allow-Methods' 'GET, POST, HEAD, OPTIONS';

                    proxy_pass_request_headers on;
                }
            }
            server {
                listen 8444 ssl;
                server_name localhost.emobix.co.uk;
                ssl_verify_client   optional_no_ca;
                location / {
                    proxy_pass http://127.0.0.1:8080;
                    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                    proxy_set_header X-Forwarded-Proto $scheme;
                    proxy_set_header X-Forwarded-Host $host;
                    proxy_set_header X-Forwarded-Port 8444;
                    proxy_set_header X-Forwarded-Uri $request_uri;
                    proxy_set_header X-Ssl-Cipher $ssl_cipher;
                    proxy_set_header X-Ssl-Protocol $ssl_protocol;
                    proxy_set_header X-Ssl-Cert $ssl_client_cert;

                    add_header 'Access-Control-Allow-Origin' '*';
                    add_header 'Access-Control-Allow-Methods' 'GET, POST, HEAD, OPTIONS';
                }
            }

        '';
  };

  # https://devenv.sh/languages/
  languages.java = {
    enable = true;
    maven.enable = true;
  };

  languages.python = {
    enable = true;
    package = pkgs.python312;
    venv.enable = true;
  };

  languages.javascript = {
    enable = true;
    npm = {
      enable = true;
      install.enable = true;
    };
  };

  pre-commit.hooks.fix-whitespace = {
      enable = true;
      name = "Check Whitespace";
      entry = "python3 scripts/checkwhitespace.py --fix";
      pass_filenames = false;
    };
  pre-commit.hooks.mvn-check = {
      enable = true;
      name = "PMD and Checkstyle";
      entry = "mvn pmd:check checkstyle:check";
      pass_filenames = false;
    };
  # See full reference at https://devenv.sh/reference/options/
}
