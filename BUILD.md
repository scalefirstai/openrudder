# Building and Testing OpenRudder

This document provides comprehensive instructions for building, testing, and verifying OpenRudder using only Free/Libre and Open Source Software (FLOSS) tools.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Building from Source](#building-from-source)
- [Running Tests](#running-tests)
- [Code Coverage](#code-coverage)
- [Continuous Integration](#continuous-integration)
- [Test Suite Details](#test-suite-details)
- [Development Tools](#development-tools)
- [Troubleshooting](#troubleshooting)

## Prerequisites

All tools required to build and test OpenRudder are FLOSS:

### Required Tools

- **Java Development Kit (JDK) 21+**
  - OpenJDK (recommended): https://openjdk.org/
  - Eclipse Temurin: https://adoptium.net/
  - License: GPL v2 with Classpath Exception
  
- **Apache Maven 3.8+**
  - Download: https://maven.apache.org/
  - License: Apache License 2.0
  
- **Git**
  - Download: https://git-scm.com/
  - License: GPL v2

### Optional Tools

- **Docker** (for integration tests)
  - Download: https://www.docker.com/
  - License: Apache License 2.0
  
- **PostgreSQL 12+** (for CDC examples)
  - Download: https://www.postgresql.org/
  - License: PostgreSQL License (BSD-style)

### Verify Installation

```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Git version
git --version
```

## Building from Source

### 1. Clone the Repository

```bash
git clone https://github.com/scalefirstai/openrudder.git
cd openrudder
```

### 2. Build All Modules

Build the entire project including all modules:

```bash
mvn clean install
```

This command will:
- Compile all source code
- Run all unit tests
- Generate test coverage reports
- Package all modules
- Install artifacts to local Maven repository

### 3. Build Without Tests (Faster)

For a faster build that skips tests:

```bash
mvn clean install -DskipTests
```

### 4. Build Specific Module

To build only a specific module:

```bash
cd openrudder-core
mvn clean install
```

### 5. Build with Specific Java Version

If you have multiple Java versions installed:

```bash
export JAVA_HOME=/path/to/jdk-21
mvn clean install
```

## Running Tests

OpenRudder includes a comprehensive automated test suite using JUnit 5, Mockito, and Reactor Test.

### Run All Tests

```bash
mvn test
```

### Run Tests for Specific Module

```bash
cd openrudder-core
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=ChangeEventTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=ChangeEventTest#shouldCreateInsertEvent
```

### Run Tests with Verbose Output

```bash
mvn test -X
```

### Run Integration Tests

```bash
mvn verify
```

This runs both unit tests and integration tests.

### Skip Unit Tests, Run Only Integration Tests

```bash
mvn verify -DskipUnitTests
```

## Code Coverage

OpenRudder uses JaCoCo for code coverage analysis.

### Generate Coverage Report

```bash
mvn clean test jacoco:report
```

### View Coverage Report

After running the above command, open the HTML report:

```bash
# On macOS
open openrudder-core/target/site/jacoco/index.html

# On Linux
xdg-open openrudder-core/target/site/jacoco/index.html

# On Windows
start openrudder-core/target/site/jacoco/index.html
```

### Coverage Reports Location

Coverage reports are generated in:
- HTML: `<module>/target/site/jacoco/index.html`
- XML: `<module>/target/site/jacoco/jacoco.xml`
- CSV: `<module>/target/site/jacoco/jacoco.csv`

### Aggregate Coverage Report

To generate a combined coverage report for all modules:

```bash
mvn clean test jacoco:report jacoco:report-aggregate
```

### Coverage Requirements

OpenRudder maintains the following coverage targets:
- **Minimum Line Coverage**: 60% (enforced by build)
- **Target Line Coverage**: 80% (recommended for contributions)
- **Branch Coverage**: Best effort

### Check Coverage Thresholds

The build will fail if coverage falls below minimum thresholds:

```bash
mvn clean verify
```

## Continuous Integration

OpenRudder uses GitHub Actions for automated testing on every push and pull request.

### CI Workflow

The CI pipeline automatically:
1. Builds the project on Ubuntu with Java 21
2. Runs all unit tests
3. Generates code coverage reports
4. Runs integration tests (when available)
5. Uploads test results and coverage reports as artifacts
6. Publishes test reports in PR checks

### View CI Results

- **GitHub Actions**: https://github.com/scalefirstai/openrudder/actions
- **Build Status Badge**: Displayed in README.md
- **Test Reports**: Available as artifacts in each workflow run

### Running CI Locally

To simulate the CI environment locally:

```bash
# Run the same commands as CI
mvn clean install
mvn test jacoco:report
mvn verify -DskipUnitTests
```

### Manual CI Trigger

You can manually trigger the CI workflow from the GitHub Actions tab using the "workflow_dispatch" event.

## Test Suite Details

### Test Framework Stack

OpenRudder uses the following FLOSS testing frameworks:

- **JUnit 5** (Jupiter) - Test framework
  - License: Eclipse Public License 2.0
  - Website: https://junit.org/junit5/
  
- **Mockito** - Mocking framework
  - License: MIT License
  - Website: https://site.mockito.org/
  
- **Reactor Test** - Reactive streams testing
  - License: Apache License 2.0
  - Website: https://projectreactor.io/
  
- **Spring Boot Test** - Spring testing utilities
  - License: Apache License 2.0
  - Website: https://spring.io/projects/spring-boot

### Test Categories

#### Unit Tests
- Located in: `src/test/java`
- Naming convention: `*Test.java`
- Run with: `mvn test`
- Purpose: Test individual classes and methods in isolation

#### Integration Tests
- Located in: `src/test/java`
- Naming convention: `*IntegrationTest.java`
- Run with: `mvn verify`
- Purpose: Test interactions between components

### Test Structure

Tests follow the Given-When-Then pattern:

```java
@Test
void shouldProcessOrderSuccessfully() {
    // Given - Setup test data and mocks
    Order order = createTestOrder();
    
    // When - Execute the code under test
    Mono<Order> result = processor.processOrder("123");
    
    // Then - Verify the results
    StepVerifier.create(result)
        .expectNextMatches(o -> o.getStatus() == OrderStatus.PROCESSED)
        .verifyComplete();
}
```

### Current Test Coverage

The project includes tests for:
- ✅ Core domain models (`ChangeEvent`, `QueryResult`)
- ✅ Continuous query evaluation
- ✅ Result set caching with indexing
- 🚧 Query engine (in progress)
- 🚧 Source implementations (in progress)
- 🚧 Reaction handlers (in progress)

## Development Tools

### Code Quality Checks

OpenRudder uses multiple FLOSS linter tools to ensure code quality. See [CODE_QUALITY.md](CODE_QUALITY.md) for complete policy.

#### Run All Quality Checks

```bash
# Run all quality checks
mvn clean verify

# This includes:
# - Compiler warnings (all enabled)
# - Checkstyle (Google Java Style)
# - PMD (bug detection)
# - SpotBugs (static analysis)
# - Tests with coverage
```

#### Individual Tools

```bash
# Checkstyle - code style verification
mvn checkstyle:check

# PMD - programming flaw detection
mvn pmd:check

# SpotBugs - bug detection
mvn spotbugs:check

# All linters at once
mvn checkstyle:check pmd:check spotbugs:check
```

#### View Reports

```bash
# Checkstyle report
open target/site/checkstyle.html

# PMD report
open target/site/pmd.html

# SpotBugs report
mvn spotbugs:gui
```

### Compiler Warnings

All compiler warnings are enabled and must be addressed:

```bash
# Compile with warnings displayed
mvn clean compile

# Warnings include:
# - Deprecation warnings
# - Unchecked operations
# - Raw types
# - Unnecessary casts
# - And more...
```

**Policy**: All code must compile without warnings. See [CODE_QUALITY.md](CODE_QUALITY.md).

### Format Code

OpenRudder follows Google Java Style Guide. Use your IDE's formatter or:

```bash
# Using Maven with google-java-format plugin (if configured)
mvn fmt:format
```

### Dependency Analysis

Check for dependency updates:

```bash
mvn versions:display-dependency-updates
```

Check for plugin updates:

```bash
mvn versions:display-plugin-updates
```

### Security Scanning

Run OWASP dependency check:

```bash
mvn org.owasp:dependency-check-maven:check
```

## Troubleshooting

### Common Issues

#### Tests Fail with "Out of Memory"

Increase Maven memory:

```bash
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"
mvn clean test
```

#### Tests Fail Intermittently

Run tests in isolation:

```bash
mvn test -Dtest=FailingTest -DforkCount=1 -DreuseForks=false
```

#### Build Fails with "Package Does Not Exist"

Clean and rebuild:

```bash
mvn clean install -U
```

The `-U` flag forces update of dependencies.

#### JaCoCo Report Not Generated

Ensure tests are run before generating report:

```bash
mvn clean test jacoco:report
```

#### Integration Tests Require Docker

Start required services:

```bash
docker-compose up -d
mvn verify
docker-compose down
```

### Getting Help

If you encounter issues:

1. Check existing issues: https://github.com/scalefirstai/openrudder/issues
2. Review CI logs: https://github.com/scalefirstai/openrudder/actions
3. Ask in discussions: https://github.com/scalefirstai/openrudder/discussions
4. Join Discord: https://discord.gg/openrudder

## Standard Build Commands

Quick reference for common build tasks:

```bash
# Full build with tests
mvn clean install

# Fast build (skip tests)
mvn clean install -DskipTests

# Run tests only
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# Run integration tests
mvn verify

# Check code style
mvn checkstyle:check

# Update dependencies
mvn clean install -U

# Generate all reports
mvn clean verify site
```

## Build Artifacts

After a successful build, artifacts are located in:

- **JAR files**: `<module>/target/*.jar`
- **Test reports**: `<module>/target/surefire-reports/`
- **Coverage reports**: `<module>/target/site/jacoco/`
- **Javadoc**: `<module>/target/site/apidocs/`

## Contributing

When contributing code:

1. ✅ Ensure all tests pass: `mvn clean verify`
2. ✅ Maintain 80%+ code coverage for new code
3. ✅ Follow code style guidelines: `mvn checkstyle:check`
4. ✅ Add tests for new functionality
5. ✅ Update documentation as needed

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines.

## License

All build tools and testing frameworks used by OpenRudder are Free/Libre and Open Source Software (FLOSS):

- **OpenRudder**: Apache License 2.0
- **Maven**: Apache License 2.0
- **JUnit 5**: Eclipse Public License 2.0
- **Mockito**: MIT License
- **JaCoCo**: Eclipse Public License 2.0
- **Spring Framework**: Apache License 2.0

---

**Last Updated**: 2026-02-01

For more information, see:
- [README.md](README.md) - Project overview
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [Documentation](https://openrudder.io/docs) - Full documentation
