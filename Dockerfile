# Use an official OpenJDK runtime as the base image
FROM eclipse-temurin:21 AS base

# Install Python and pip
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install required Python packages
RUN pip3 install yfinance>=0.2.54 CurrencyConverter --break-system-packages

# Install Leiningen (Clojure build tool)
RUN apt-get update && apt-get install -y curl
RUN curl -o /usr/local/bin/lein https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
    && chmod +x /usr/local/bin/lein

# Set the working directory
WORKDIR /app

# Copy the project files
COPY . /app

# Build the Clojure project
RUN lein deps
# RUN lein uberjar

# Expose the Gorilla REPL port (default is 8990)
EXPOSE 8990

# Run the Gorilla REPL
CMD ["lein", "gorilla", ":ip", "0.0.0.0", ":port", "8990"]