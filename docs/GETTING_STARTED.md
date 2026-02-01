# Getting Started with OpenRudder

## Overview

OpenRudder is a pure Java event-driven AI agent platform that enables real-time data change processing and intelligent reactions. This guide will help you get started quickly.

## Prerequisites

- **Java 21+** - Download from [OpenJDK](https://openjdk.java.net/)
- **Maven 3.8+** - Download from [Maven](https://maven.apache.org/)
- **Docker** (optional) - For running example databases
- **PostgreSQL 12+** (for CDC examples)

## Installation

### 1. Build the Project

```bash
cd /Volumes/D/Projects/OpenRudder
mvn clean install -DskipTests
```

### 2. Start Infrastructure (Optional)

```bash
# Start PostgreSQL, MongoDB, and Kafka
docker-compose up -d

# Wait for services to be ready
sleep 10

# Initialize PostgreSQL with sample data
docker exec -i openrudder-postgres psql -U postgres -d orders < scripts/init-db.sql
```

## Running the Example Application

### Order Monitoring Example

This example demonstrates:
- PostgreSQL CDC monitoring
- Continuous query evaluation  
- AI-powered order dispatching

```bash
cd openrudder-examples

# Set your OpenAI API key (or use demo mode)
export OPENAI_API_KEY=your-api-key-here

# Run the application
mvn spring-boot:run
```

### Test the System

In another terminal, insert a new order:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
INSERT INTO orders (customer, status, location, driver_assigned) 
VALUES ('Alice Brown', 'READY_FOR_PICKUP', '999 Test St', FALSE);
EOF
```

You should see:
1. CDC captures the change
2. Query evaluates the new order
3. AI agent analyzes and suggests driver assignment

## Creating Your Own Application

### Step 1: Add Dependency

```xml
<dependency>
    <groupId>io.openrudder</groupId>
    <artifactId>openrudder-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Step 2: Enable OpenRudder

```java
@SpringBootApplication
@EnableOpenRudder
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Step 3: Configure a Source

```java
@Bean
public PostgresSource mySource() {
    return PostgresSource.builder()
        .name("my-source")
        .host("localhost")
        .port(5432)
        .database("mydb")
        .username("user")
        .password("pass")
        .table("my_table")
        .cdcEnabled(true)
        .build();
}
```

### Step 4: Define a Query

```java
@Bean
public ContinuousQuery myQuery(PostgresSource source) {
    return ContinuousQuery.builder()
        .id("my-query")
        .name("My Query")
        .query("""
            MATCH (e:Entity)
            WHERE e.status = 'ACTIVE'
            RETURN e
            """)
        .sourceIds(Set.of(source.getId()))
        .build();
}
```

### Step 5: Create a Reaction

```java
@Bean
public LangChain4jReaction myReaction(ChatLanguageModel model) {
    return LangChain4jReaction.builder()
        .name("my-reaction")
        .queryId("my-query")
        .model(model)
        .systemPrompt("You are a helpful AI assistant")
        .onResponse(response -> {
            log.info("AI Response: {}", response.text());
            return Mono.empty();
        })
        .build();
}
```

## Configuration

### application.yml

```yaml
openrudder:
  enabled: true
  auto-start: true

postgres:
  host: localhost
  port: 5432
  database: mydb
  username: postgres
  password: postgres

openai:
  api:
    key: ${OPENAI_API_KEY}

logging:
  level:
    io.openrudder: DEBUG
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                  OpenRudder Platform                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────┐      ┌──────────────┐      ┌──────────┐  │
│  │ Sources  │─────▶│  Continuous  │─────▶│Reactions │  │
│  │ (CDC)    │      │  Query       │      │(AI Agents│  │
│  │          │      │  Engine      │      │)         │  │
│  └──────────┘      └──────────────┘      └──────────┘  │
│       │                    │                    │       │
│       │                    │                    │       │
│  ┌────▼────────────────────▼────────────────────▼────┐ │
│  │       Event Store & Change Stream                 │ │
│  │      (Reactive Streams / Kafka / Redis)           │ │
│  └───────────────────────────────────────────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Key Concepts

### Change Events
Events represent data changes (INSERT, UPDATE, DELETE) captured from sources.

### Continuous Queries
Queries that continuously evaluate against change streams and emit result updates.

### Reactions
Actions triggered by query result changes, including AI agent invocations.

### Incremental Processing
Only affected query results are re-evaluated, ensuring high performance.

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

## Troubleshooting

### PostgreSQL CDC Not Working

Ensure WAL level is set to logical:
```sql
ALTER SYSTEM SET wal_level = logical;
SELECT pg_reload_conf();
```

### Classpath Warnings

Run Maven install to download dependencies:
```bash
mvn clean install
```

### Port Already in Use

Change ports in `application.yml` or stop conflicting services.

## Next Steps

- Read the [Architecture Guide](Requirement/ARCHITECTURE.md)
- Explore [Platform Comparison](Requirement/COMPARISON.md)
- Check out more examples in `openrudder-examples/`
- Join our [Discord community](https://discord.gg/openrudder)

## Support

- **GitHub Issues**: Report bugs and request features
- **Documentation**: See `Requirement/` folder
- **Examples**: Check `openrudder-examples/` module

---

Happy coding with OpenRudder! 🚀
