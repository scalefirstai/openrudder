# Contributing to OpenRudder

Thank you for your interest in contributing to OpenRudder! We welcome contributions from the community and are grateful for your support. This document provides guidelines and instructions for contributing.

## 🌟 Ways to Contribute

There are many ways to contribute to OpenRudder:

- 🐛 **Report bugs** - Help us identify and fix issues
- 💡 **Suggest features** - Share ideas for new functionality
- 📝 **Improve documentation** - Help others understand OpenRudder better
- 🔧 **Submit code** - Fix bugs or implement new features
- 💬 **Help others** - Answer questions in issues and discussions
- 🎨 **Design** - Improve UI/UX for documentation or examples
- 🧪 **Write tests** - Improve code coverage and reliability

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- Git
- Docker (for integration tests)
- PostgreSQL (for CDC testing)
- IDE with Java support (IntelliJ IDEA, Eclipse, or VS Code)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/openrudder.git
   cd openrudder
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/scalefirstai/openrudder.git
   ```
4. Create a feature branch:
   ```bash
   git checkout -b feature/my-awesome-feature
   ```

## 🔨 Development Workflow

### Building the Project

```bash
# Build all modules
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build specific module
cd openrudder-core
mvn clean install
```

### Running Tests

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify

# Run tests for specific module
cd openrudder-query-engine
mvn test

# Run specific test class
mvn test -Dtest=RudderEngineTest

# Run with coverage
mvn clean test jacoco:report
```

### Running Examples

```bash
cd openrudder-examples
mvn spring-boot:run
```

## 📋 Code Style Guidelines

We follow standard Java conventions with some specific preferences:

### General Guidelines

- **Java Version**: Use Java 21 features appropriately
- **Formatting**: Use 4 spaces for indentation (no tabs)
- **Line Length**: Maximum 120 characters per line
- **Naming**: Use descriptive names for variables, methods, and classes
- **Lombok**: Use Lombok annotations to reduce boilerplate (`@Data`, `@Builder`, etc.)

### Code Structure

```java
// ✅ Good
public class OrderProcessor {
    private final OrderRepository repository;
    
    public Mono<Order> processOrder(String orderId) {
        return repository.findById(orderId)
            .flatMap(this::validateOrder)
            .flatMap(this::enrichOrder)
            .flatMap(repository::save);
    }
}

// ❌ Bad - too much in one method
public class OrderProcessor {
    public Mono<Order> processOrder(String orderId) {
        // 100 lines of code...
    }
}
```

### Documentation

- Add Javadoc for all public APIs
- Include `@param`, `@return`, and `@throws` tags
- Provide usage examples for complex APIs

```java
/**
 * Processes an order through the fulfillment pipeline.
 *
 * @param orderId the unique identifier of the order
 * @return a Mono emitting the processed order
 * @throws OrderNotFoundException if the order does not exist
 */
public Mono<Order> processOrder(String orderId) {
    // implementation
}
```

## 🧪 Testing Guidelines

### Test Coverage

- Maintain minimum 80% code coverage
- Write unit tests for all business logic
- Add integration tests for complex workflows
- Include edge cases and error scenarios

### Test Structure

```java
@Test
void shouldProcessOrderSuccessfully() {
    // Given
    Order order = createTestOrder();
    when(repository.findById(any())).thenReturn(Mono.just(order));
    
    // When
    Mono<Order> result = processor.processOrder("123");
    
    // Then
    StepVerifier.create(result)
        .expectNextMatches(o -> o.getStatus() == OrderStatus.PROCESSED)
        .verifyComplete();
}
```

## 📝 Commit Message Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples

```bash
feat(query-engine): add support for temporal queries

Implement temporal query operators (BEFORE, AFTER, DURING) to enable
time-based pattern matching in continuous queries.

Closes #123

fix(sources): handle connection timeout in PostgreSQL CDC

Add retry logic with exponential backoff when PostgreSQL connection
times out during CDC streaming.

Fixes #456
```

## 🔄 Pull Request Process

### Before Submitting

1. **Sync with upstream**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Run tests locally**:
   ```bash
   mvn clean verify
   ```

3. **Check code style**:
   ```bash
   mvn checkstyle:check
   ```

### PR Guidelines

- **Title**: Use a clear, descriptive title
- **Description**: Explain what changes you made and why
- **Link Issues**: Reference related issues using `Fixes #123` or `Closes #456`
- **Screenshots**: Include screenshots for UI changes
- **Breaking Changes**: Clearly mark any breaking changes
- **Documentation**: Update relevant documentation

### PR Template

Your PR should include:

- [ ] Description of changes
- [ ] Related issue(s)
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] Changelog entry added
- [ ] All tests passing
- [ ] Code review requested

## 🐛 Reporting Issues

### Bug Reports

When reporting bugs, please include:

- **Description**: Clear description of the issue
- **Steps to Reproduce**: Detailed steps to reproduce the behavior
- **Expected Behavior**: What you expected to happen
- **Actual Behavior**: What actually happened
- **Environment**:
  - OpenRudder version
  - Java version
  - Operating System
  - Database version (if applicable)
- **Logs**: Relevant log output or stack traces
- **Screenshots**: If applicable

### Feature Requests

When requesting features, please include:

- **Use Case**: Describe the problem you're trying to solve
- **Proposed Solution**: Your idea for how to solve it
- **Alternatives**: Other solutions you've considered
- **Additional Context**: Any other relevant information

## 💬 Community Guidelines

- Be respectful and inclusive
- Follow our [Code of Conduct](CODE_OF_CONDUCT.md)
- Help others in discussions and issues
- Share your experiences and use cases
- Provide constructive feedback

## 🎯 Good First Issues

Looking for a place to start? Check out issues labeled:
- `good first issue` - Great for newcomers
- `help wanted` - We need community help
- `documentation` - Improve our docs

## 📚 Resources

- [Documentation](https://openrudder.io/docs)
- [Architecture Guide](Requirement/ARCHITECTURE.md)
- [API Reference](https://openrudder.io/api)
- [Discord Community](https://discord.gg/openrudder)

## 📄 License

By contributing to OpenRudder, you agree that your contributions will be licensed under the Apache License 2.0.

## 🙏 Recognition

All contributors will be recognized in our:
- [Contributors page](https://github.com/scalefirstai/openrudder/graphs/contributors)
- Release notes
- Project documentation

Thank you for contributing to OpenRudder! 🚀
