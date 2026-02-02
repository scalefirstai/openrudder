# Testing Guide for OpenRudder

This document provides a quick reference for testing OpenRudder. For comprehensive build instructions, see [BUILD.md](BUILD.md).

## Quick Start

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open openrudder-core/target/site/jacoco/index.html
```

## Test Suite Overview

OpenRudder uses **100% FLOSS (Free/Libre and Open Source Software)** testing tools:

| Tool | Purpose | License |
|------|---------|---------|
| JUnit 5 | Test framework | EPL 2.0 |
| Mockito | Mocking | MIT |
| Reactor Test | Reactive testing | Apache 2.0 |
| JaCoCo | Code coverage | EPL 2.0 |
| Maven Surefire | Test runner | Apache 2.0 |

## Running Tests

### Standard Way (Maven)

The test suite is invocable in the standard Maven way:

```bash
mvn test
```

This is the standard command for running tests in Maven-based Java projects.

### With Coverage

```bash
mvn clean test jacoco:report
```

### Specific Module

```bash
cd openrudder-core
mvn test
```

### Specific Test Class

```bash
mvn test -Dtest=ChangeEventTest
```

### Specific Test Method

```bash
mvn test -Dtest=ChangeEventTest#shouldCreateInsertEvent
```

## Test Categories

### Unit Tests
- **Location**: `src/test/java/**/*Test.java`
- **Purpose**: Test individual classes in isolation
- **Run**: `mvn test`
- **Coverage**: 80%+ target

### Integration Tests
- **Location**: `src/test/java/**/*IntegrationTest.java`
- **Purpose**: Test component interactions
- **Run**: `mvn verify`
- **Coverage**: Best effort

## Code Coverage

### View Coverage Reports

After running tests with coverage:

```bash
# HTML report (human-readable)
open openrudder-core/target/site/jacoco/index.html

# XML report (for CI tools)
cat openrudder-core/target/site/jacoco/jacoco.xml

# CSV report (for analysis)
cat openrudder-core/target/site/jacoco/jacoco.csv
```

### Coverage Requirements

- **Minimum**: 60% line coverage (enforced)
- **Target**: 80% line coverage (recommended)
- **New Code**: 80%+ coverage required for PRs

### Check Coverage Threshold

```bash
mvn clean verify
```

Build fails if coverage < 60%.

## Continuous Integration

### Automated Testing

Every push and PR triggers automated tests:

1. ✅ Build verification
2. ✅ Unit tests with coverage
3. ✅ Integration tests
4. ✅ Coverage report generation
5. ✅ Test result publishing

### View CI Results

- **Actions**: https://github.com/scalefirstai/openrudder/actions
- **Coverage**: Uploaded to Codecov
- **Reports**: Available as workflow artifacts

### CI Configuration

See `.github/workflows/build.yml` for the complete CI pipeline.

## Writing Tests

### Test Structure

Follow the Given-When-Then pattern:

```java
@Test
void shouldProcessOrderSuccessfully() {
    // Given - Setup
    Order order = createTestOrder();
    when(repository.findById(any())).thenReturn(Mono.just(order));
    
    // When - Execute
    Mono<Order> result = processor.processOrder("123");
    
    // Then - Verify
    StepVerifier.create(result)
        .expectNextMatches(o -> o.getStatus() == OrderStatus.PROCESSED)
        .verifyComplete();
}
```

### Naming Conventions

- Test classes: `*Test.java`
- Test methods: `should[DoSomething]When[Condition]`
- Example: `shouldReturnEmptyWhenOrderNotFound`

### Best Practices

- ✅ Test one thing per test method
- ✅ Use descriptive test names
- ✅ Keep tests independent
- ✅ Use test fixtures for common setup
- ✅ Mock external dependencies
- ✅ Test edge cases and error conditions

## Test Examples

### Testing Domain Models

```java
@Test
void shouldCreateInsertEvent() {
    ChangeEvent event = ChangeEvent.builder()
        .type(ChangeEvent.ChangeType.INSERT)
        .entityType("Order")
        .after(Map.of("id", 1))
        .build();
    
    assertTrue(event.isInsert());
    assertEquals("Order", event.getEntityType());
}
```

### Testing Reactive Streams

```java
@Test
void shouldFilterEventsBySourceId() {
    Flux<ChangeEvent> events = Flux.just(event1, event2);
    Flux<ChangeEvent> filtered = query.evaluate(events);
    
    StepVerifier.create(filtered)
        .expectNextCount(1)
        .verifyComplete();
}
```

### Testing with Mocks

```java
@Test
void shouldCallRepositoryWhenProcessing() {
    when(repository.save(any())).thenReturn(Mono.just(order));
    
    processor.processOrder("123").block();
    
    verify(repository).save(any());
}
```

## Troubleshooting

### Tests Fail Locally

```bash
# Clean and rebuild
mvn clean install

# Run with verbose output
mvn test -X

# Run single test
mvn test -Dtest=FailingTest
```

### Coverage Report Not Generated

```bash
# Ensure tests run first
mvn clean test jacoco:report
```

### Out of Memory

```bash
export MAVEN_OPTS="-Xmx2048m"
mvn test
```

## Additional Resources

- **Complete Build Guide**: [BUILD.md](BUILD.md)
- **Contributing Guide**: [CONTRIBUTING.md](CONTRIBUTING.md)
- **CI Workflows**: `.github/workflows/`
- **Test Examples**: `openrudder-core/src/test/java/`

## Getting Help

- **Issues**: https://github.com/scalefirstai/openrudder/issues
- **Discussions**: https://github.com/scalefirstai/openrudder/discussions
- **Discord**: https://discord.gg/openrudder

---

**All testing tools are FLOSS** - OpenRudder is buildable and testable using only Free/Libre and Open Source Software.
