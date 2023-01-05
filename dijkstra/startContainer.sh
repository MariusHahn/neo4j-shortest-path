#!/bin/sh
docker run \
  --rm \
  --user "$(id -u):$(id -g)"\
  --name neo4j-shortest-path\
  --env=NEO4J_AUTH=none\
  --publish=7474:7474 \
  --publish=7687:7687 \
  --volume="$(pwd)"/build/libs:/var/lib/neo4j/plugins \
  neo4j