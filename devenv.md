NixOS / devenv based Setup for Conformance Testsuite Development
---

This document describes how to setup a development environment for conformance testsuite development
based on [NixOS](https://nixos.org/) and [devenv](https://devenv.sh/).

The devenv will create a containerized environment based on the [devenv.nix](./devenv.nix) configuration file.

It will setup the following services:
- MongoDB
- Nginx
- Setup 127.0.0.1 host aliases for localhost.emobix.co.uk
- Generate and install TLS certificates into the local Keychain

# Setup

## Install NixOS Tooling

See: https://nixos.org/download/#download-nix

```
sh <(curl -L https://nixos.org/nix/install)
```

## Install devenv

See: https://devenv.sh/getting-started/#2-install-devenv

```
nix-env -iA devenv -f https://github.com/NixOS/nixpkgs/tarball/nixpkgs-unstable
```

# Run

Once the installation of nix and devenv has been completed, one can start the devenv.

## Start devenv

See: https://devenv.sh/getting-started/#commands

```
devenv up
```

Once up, one can launch the Conformance Testsuite in the IDE.

## Stop devenv

To stop the environment press `ctrl+c` and confirm the message via with enter.
