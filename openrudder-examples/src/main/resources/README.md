# OpenRudder YAML Configuration Examples

This directory contains example YAML configuration files for OpenRudder.

## Files

### openrudder.yml
Basic example with a single Postgres source and webhook reaction.

**Use case:** Simple order monitoring with webhook notifications

**Run:**
```bash
mvn spring-boot:run -Dspring.profiles.active=yaml
```

### openrudder-ai.yml
AI-powered reaction using OpenAI GPT models.

**Use case:** Intelligent order analysis with AI agents

**Prerequisites:**
- Set `OPENAI_API_KEY` environment variable

**Run:**
```bash
export OPENAI_API_KEY=your_key
mvn spring-boot:run -Dspring.profiles.active=yaml \
  -Dopenrudder.config.yaml.path=classpath:openrudder-ai.yml
```

### openrudder-multi-source.yml
Complex setup with multiple data sources and reactions.

**Use case:** Multi-database monitoring with Kafka integration

**Prerequisites:**
- PostgreSQL running on localhost:5432
- Kafka running on localhost:9092

**Run:**
```bash
mvn spring-boot:run -Dspring.profiles.active=yaml \
  -Dopenrudder.config.yaml.path=classpath:openrudder-multi-source.yml
```

## Configuration Structure

All YAML files follow this structure:

```yaml
version: "1.0"

sources:
  - # Source definitions

queries:
  - # Query definitions

reactions:
  - # Reaction definitions
```

## Environment Variables

Use `${VAR_NAME}` syntax for environment variable substitution:

```yaml
postgres:
  password: ${POSTGRES_PASSWORD}
  host: ${POSTGRES_HOST:localhost}  # With default value
```

## Supported Types

### Sources
- `postgres` - PostgreSQL with CDC
- `kafka` - Kafka topics
- `mongodb` - MongoDB change streams

### Reactions
- `webhook` - HTTP webhooks
- `ai` - AI agents (OpenAI, etc.)
- `kafka` - Kafka publishers
- `http` - HTTP requests

## Documentation

For complete documentation, see:
- [YAML Configuration Guide](../../../YAML_CONFIGURATION.md)
- [Examples README](../README.md)
