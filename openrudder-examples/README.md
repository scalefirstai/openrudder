# OpenRudder Examples

This module contains example applications demonstrating various OpenRudder features.

## Examples

### 1. OrderMonitoringApplication (Java Configuration)

Traditional Java-based configuration using Spring beans.

**Location:** `src/main/java/io/openrudder/examples/OrderMonitoringApplication.java`

**Features:**
- Postgres CDC source
- Continuous query for ready orders
- AI-powered dispatcher using LangChain4j

**Run:**
```bash
mvn spring-boot:run -Dspring-boot.run.main-class=io.openrudder.examples.OrderMonitoringApplication
```

### 2. YamlConfiguredApplication (YAML Configuration)

Modern declarative configuration using YAML files.

**Location:** `src/main/java/io/openrudder/examples/YamlConfiguredApplication.java`

**Features:**
- YAML-based source, query, and reaction configuration
- Environment variable substitution
- Multiple configuration examples

**Run:**
```bash
mvn spring-boot:run \
  -Dspring-boot.run.main-class=io.openrudder.examples.YamlConfiguredApplication \
  -Dspring-boot.run.profiles=yaml
```

## YAML Configuration Examples

### Basic Webhook Example
**File:** `src/main/resources/openrudder.yml`

Simple webhook reaction triggered by database changes.

### AI-Powered Example
**File:** `src/main/resources/openrudder-ai.yml`

AI agent that analyzes orders using OpenAI GPT models.

### Multi-Source Example
**File:** `src/main/resources/openrudder-multi-source.yml`

Complex setup with multiple data sources (Postgres, Kafka) and reactions.

## Configuration

### Using YAML Configuration

1. Set the active profile:
```yaml
spring:
  profiles:
    active: yaml
```

2. Configure the YAML path:
```yaml
openrudder:
  config:
    yaml:
      enabled: true
      path: classpath:openrudder.yml
```

3. Set environment variables:
```bash
export POSTGRES_PASSWORD=your_password
export OPENAI_API_KEY=your_api_key
```

### Using Java Configuration

Use the traditional Spring Boot approach with `@Bean` methods.

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker (for running Postgres)

## Running with Docker Compose

Start the required services:

```bash
docker-compose up -d
```

This starts:
- PostgreSQL with CDC enabled
- Kafka (optional, for multi-source example)

## Documentation

For detailed YAML configuration documentation, see:
- [YAML Configuration Guide](../../YAML_CONFIGURATION.md)
- [API Documentation](../../API.md)

## Future: UI-Based Configuration

The YAML configuration system is designed to support future UI-based configuration tools, allowing visual configuration of sources, queries, and reactions without editing YAML files directly.
