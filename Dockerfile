# Use an official OpenJDK runtime as the base image
FROM eclipse-temurin:21 AS base

# Install Leiningen (Clojure build tool)
RUN apt-get update && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

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
# RUN lein uberjar

# Expose the web server port
EXPOSE 3000

# Run the application
CMD ["lein", "run"]

