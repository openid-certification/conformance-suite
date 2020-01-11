#!/bin/bash
#openssl genrsa -out local-provider.key 4096
openssl req -x509 -nodes -days 3650 -key local-provider.key -out local-provider-oidcc.crt -subj '/CN=oidcc-provider'
