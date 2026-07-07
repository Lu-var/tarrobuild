#!/bin/sh
URI=$DATABASE_URL
STRIPPED=$(echo "$URI" | sed 's|^postgres://||;s|^postgresql://||')
export SPRING_DATASOURCE_URL="jdbc:postgresql://$(echo "$STRIPPED" | cut -d@ -f2-)"
export SPRING_DATASOURCE_USERNAME="$(echo "$STRIPPED" | cut -d: -f1)"
export SPRING_DATASOURCE_PASSWORD="$(echo "$STRIPPED" | cut -d: -f2- | sed 's|@.*||')"
exec java -jar app.jar
