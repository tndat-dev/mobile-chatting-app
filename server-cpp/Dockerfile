# Build stage
FROM alpine:3.19 AS builder

# Install necessary build dependencies
RUN apk add --no-cache \
    build-base \
    cmake \
    make \
    linux-headers \
    postgresql-dev

WORKDIR /app

# Copy CMake files and source code
COPY CMakeLists.txt .
COPY include/ include/
COPY src/ src/
COPY tools/ tools/

# Configure and build
RUN mkdir build && cd build && \
    cmake .. && \
    make -j$(nproc)

# Runtime stage
FROM alpine:3.19

# Install runtime dependencies
RUN apk add --no-cache \
    libpq \
    libstdc++ \
    libgcc

WORKDIR /app

# Copy the built binary from the builder stage
COPY --from=builder /app/build/chat_server .

# Default port
ENV PORT=8080
EXPOSE 8080

# Entrypoint
ENTRYPOINT ["./chat_server"]
