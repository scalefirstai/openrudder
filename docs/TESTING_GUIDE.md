# OpenRudder Testing Guide

This guide shows you how to test the OpenRudder platform with real database changes and AI agent responses.

## Prerequisites

- Docker and Docker Compose installed
- Java 21+ installed
- Maven 3.8+ installed
- OpenAI API key set in environment: `export OPENAI_API_KEY=your-key-here`

## Step 1: Start the Infrastructure

Start PostgreSQL with CDC enabled:

```bash
cd /Volumes/D/Projects/OpenRudder
docker-compose up -d postgres
```

Wait for PostgreSQL to be ready (about 10 seconds):

```bash
docker logs openrudder-postgres
```

You should see: `database system is ready to accept connections`

## Step 2: Verify Database Setup

Check that the database is initialized with sample data:

```bash
docker exec -it openrudder-postgres psql -U postgres -d orders -c "SELECT * FROM orders;"
```

You should see 3 orders, including one with status `READY_FOR_PICKUP`.

## Step 3: Start the OpenRudder Application

In a new terminal, start the application:

```bash
cd /Volumes/D/Projects/OpenRudder/openrudder-examples
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export OPENAI_API_KEY=your-key-here
mvn spring-boot:run
```

The application will:
1. Connect to PostgreSQL
2. Initialize the OpenRudder engine
3. Start CDC monitoring on the `orders` table
4. Evaluate the continuous query for orders with status `READY_FOR_PICKUP`
5. Send matching orders to the AI agent for analysis

## Step 4: Test with Database Changes

### Test Case 1: Insert a New Ready Order

In another terminal, insert a new order that matches the query:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
INSERT INTO orders (customer, status, location, driver_assigned) 
VALUES ('Alice Brown', 'READY_FOR_PICKUP', '999 Test St', FALSE);
EOF
```

**Expected Result:**
- CDC captures the INSERT event
- Query evaluates and matches the new order (status = 'READY_FOR_PICKUP', driver_assigned = FALSE)
- AI agent receives the order details and suggests driver assignment
- You'll see logs like:
  ```
  Query 'Ready Orders Query' matched event: ...
  AI Dispatcher Response: Based on the order details...
  ```

### Test Case 2: Update an Existing Order to Ready

Update an order to trigger the query:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
UPDATE orders 
SET status = 'READY_FOR_PICKUP' 
WHERE customer = 'John Doe';
EOF
```

**Expected Result:**
- CDC captures the UPDATE event
- Query matches the updated order
- AI agent analyzes and suggests driver assignment

### Test Case 3: Assign a Driver (Should Not Match)

Update an order to assign a driver:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
UPDATE orders 
SET driver_assigned = TRUE 
WHERE customer = 'Alice Brown';
EOF
```

**Expected Result:**
- CDC captures the UPDATE event
- Query does NOT match (driver_assigned = TRUE)
- No AI agent invocation

### Test Case 4: Delete an Order

Delete an order:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
DELETE FROM orders WHERE customer = 'Bob Johnson';
EOF
```

**Expected Result:**
- CDC captures the DELETE event
- Query evaluates the deletion
- Result update type is REMOVED

## Understanding the Query

The example application uses this continuous query:

```cypher
MATCH (o:Order)
WHERE o.status = 'READY_FOR_PICKUP'
  AND NOT EXISTS(o.driver_assigned)
RETURN o.id, o.customer, o.location
```

This matches orders that are:
- Status is exactly `READY_FOR_PICKUP`
- `driver_assigned` is FALSE or NULL

## Monitoring the System

### View Application Logs

The application logs show:
- CDC events captured
- Query evaluation results
- AI agent responses

Look for these log patterns:
```
DEBUG io.openrudder.core.query.ContinuousQuery : Query 'Ready Orders Query' matched event: ...
INFO  io.openrudder.examples.OrderMonitoringApplication : AI Dispatcher Response: ...
```

### Check Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

### View Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

## Troubleshooting

### PostgreSQL CDC Not Working

Ensure WAL level is logical:

```bash
docker exec -it openrudder-postgres psql -U postgres -c "SHOW wal_level;"
```

Should return `logical`. If not, restart the container:

```bash
docker-compose restart postgres
```

### No AI Responses

Check that your OpenAI API key is set:

```bash
echo $OPENAI_API_KEY
```

If using demo mode, the AI model won't actually call OpenAI but will still process the events.

### Application Won't Start

Check if port 8080 is already in use:

```bash
lsof -i :8080
```

Kill the process or change the port in `application.yml`.

## Advanced Testing

### Bulk Insert Test

Test with multiple orders at once:

```bash
docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
INSERT INTO orders (customer, status, location, driver_assigned) VALUES
    ('Customer 1', 'READY_FOR_PICKUP', 'Location 1', FALSE),
    ('Customer 2', 'READY_FOR_PICKUP', 'Location 2', FALSE),
    ('Customer 3', 'READY_FOR_PICKUP', 'Location 3', FALSE),
    ('Customer 4', 'PREPARING', 'Location 4', FALSE);
EOF
```

You should see 3 AI agent invocations (Customer 4 is filtered out).

### Stress Test

Run a script to continuously insert orders:

```bash
for i in {1..10}; do
  docker exec -i openrudder-postgres psql -U postgres -d orders << EOF
  INSERT INTO orders (customer, status, location, driver_assigned) 
  VALUES ('Stress Test $i', 'READY_FOR_PICKUP', 'Test Location $i', FALSE);
EOF
  sleep 2
done
```

Monitor the application logs to see real-time processing.

## Cleanup

Stop the application (Ctrl+C in the terminal running it).

Stop and remove containers:

```bash
docker-compose down
```

To also remove data volumes:

```bash
docker-compose down -v
```

## Next Steps

- Modify the query in `OrderMonitoringApplication.java` to match different conditions
- Create additional reactions to handle different event types
- Add more data sources (MongoDB, Kafka)
- Implement custom query evaluators for complex logic
