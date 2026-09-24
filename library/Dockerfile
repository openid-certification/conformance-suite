# Tooling for scripts/spec_library.py: fetch and render the specification texts under
# library/specs. The repository is mounted at /work; nothing from it is copied in.
#
#   docker build -t cts-spec-library library/
#   docker run --rm -v "$PWD:/work" cts-spec-library check
#   docker run --rm -v "$PWD:/work" cts-spec-library sync --refresh-linked authorization-api-1_0
#
# Versions are pinned to what the manifest records (`generator`), so a re-render of an
# unchanged source reproduces the stored sha256. Bump them together with a full
# `sync --refresh-linked` of every rendered document.

FROM golang:1.24-alpine AS mmark
# mmark renders the markdown sources (`md`, `github-md`) to xml2rfc XML.
RUN CGO_ENABLED=0 go install github.com/mmarkdown/mmark/v2@v2.2.48

FROM python:3.12-slim
# Python 3.12 or later: the script extracts tarballs with tarfile's `filter="data"`.
# xml2rfc renders every non-txt source; it fetches the bibliography from xml2rfc.ietf.org at
# render time, so the container needs network access.
RUN pip install --no-cache-dir xml2rfc==3.34.0
COPY --from=mmark /go/bin/mmark /usr/local/bin/mmark
WORKDIR /work
ENTRYPOINT ["python3", "scripts/spec_library.py"]
CMD ["check"]
