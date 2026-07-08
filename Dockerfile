# Use an official OpenJDK runtime as the base image
FROM eclipse-temurin:21 AS base

# Install Leiningen (Clojure build tool)
RUN apt-get update && apt-get install -y curl
RUN curl -o /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
    && chmod +x /usr/local/bin/lein

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . /app

# These directories will be replaced by bind mounts to support writing from docker host
RUN rm -rf /app/examples
RUN rm -rf /app/src

# Build the Clojure project
RUN lein deps
# RUN lein uberjar

# Expose Clerk's webserver port (currently set to 8990, can set in user.clj)
EXPOSE 8990

# Run the Lein REPL
CMD ["lein", "repl"]

