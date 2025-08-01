#
# And awayyyy we go!
#

set dotenv-load
set quiet

# List all recipes (_ == hidden recipe)
_default:
    just --list

# Cat the Justfile
cat:
    just --dump

# Upgrade dependencies
deps:
    clojure -X:antq

# Checks (or formats) the source code
format action="check" files="":
    clojure -M:{{ action }} {{ files }}

# Build the application
build:
    bin/build

# Run the Docker services, e.g., PostgreSQL, Valkey...
up:
    docker compose -f scripts/docker/docker-compose-services.yml up

# Stop running the Docker services
down:
    docker compose -f scripts/docker/docker-compose-services.yml down

# Install pre-commit (https://pre-commit.com/)
pre-commit-install:
    pre-commit install

# Run pre-commit hooks (to verify at any point, not just on commit)
pre-commit-run hook-id="":
    pre-commit run --all-files {{ hook-id }}

# Drops and recreates the passkeys-demo database
recreate-passkeys-demo:
    psql -h localhost -d postgres -U postgres -f scripts/sql/200-drop-create-passkeys-demo-db.sql

# Run the UberJAR locally
run-local: build
    bin/run-local

# vim: expandtab:ts=4:sw=4:ft=just
