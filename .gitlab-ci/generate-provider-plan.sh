#!/bin/bash
docker run --rm \
  -v $(pwd)/local-provider-oidcc.json:/home/node/app/config.json \
  panvafs/oidc-provider-oidc-core-sample plan \
  > local-provider-oidcc-conformance-config.json
