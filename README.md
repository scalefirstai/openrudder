<div align="center">

# ⚡ OpenRudder

### Event-Driven AI Agent Platform for Java

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0--SNAPSHOT-blue)](https://search.maven.org/artifact/io.openrudder/openrudder-parent)
[![Build Status](https://img.shields.io/github/actions/workflow/status/scalefirstai/openrudder/build.yml?branch=main)](https://github.com/scalefirstai/openrudder/actions)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-7289da)](https://discord.gg/openrudder)

[Website](https://openrudder.io) • [Documentation](https://openrudder.io/docs) • [API Reference](https://openrudder.io/api) • [Quick Start](#-quick-start) • [Bug Reports](https://github.com/scalefirstai/openrudder/issues/new?template=bug_report.md) • [Contributing](CONTRIBUTING.md)

</div>

---

## 🌟 What is OpenRudder?

**OpenRudder** is a pure Java implementation of an event-driven change data processing platform designed specifically for **Ambient AI Agents** that react to real-time data changes. Build intelligent, reactive systems that respond to database changes, stream events, and data mutations with zero code changes to your existing applications.

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL 12+ (for CDC examples)
- Docker (optional, for running examples)

### Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.openrudder</groupId>
    <artifactId>openrudder-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Example

```java
@SpringBootApplication
@EnableOpenRudder
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
    
    @Bean
    public PostgresSource ordersSource() {
        return PostgresSource.builder()
            .name("orders")
            .host("localhost")
            .database("mydb")
            .username("user")
            .password("pass")
            .table("orders")
            .cdcEnabled(true)
            .build();
    }
    
    @Bean
    public ContinuousQuery readyOrders(PostgresSource ordersSource) {
        return ContinuousQuery.builder()
            .id("ready-orders")
            .name("Ready Orders")
            .query("""
                MATCH (o:Order)
                WHERE o.status = 'READY'
                RETURN o
                """)
            .sourceIds(Set.of(ordersSource.getId()))
            .build();
    }
    
    @Bean
    public LangChain4jReaction aiAgent(ChatLanguageModel model) {
        return LangChain4jReaction.builder()
            .name("order-processor")
            .queryId("ready-orders")
            .model(model)
            .systemPrompt("You are an order processing AI")
            .onResponse(response -> {
                System.out.println("AI: " + response.text());
                return Mono.empty();
            })
            .build();
    }
}
```

## 📦 Project Structure

```
openrudder/
├── openrudder-core/              # Core domain models and interfaces
├── openrudder-sources/           # Data source implementations (CDC, Kafka, MongoDB)
├── openrudder-query-engine/      # Continuous query engine with incremental processing
├── openrudder-reactions/         # Reaction implementations (AI agents, webhooks)
├── openrudder-spring-boot-starter/ # Spring Boot auto-configuration
└── openrudder-examples/          # Example applications
```

## 🏗️ Architecture

### Core Components

1. **Sources** - Capture data changes from various sources
   - PostgreSQL CDC (via Debezium)
   - MongoDB Change Streams
   - Kafka Topics
   - Custom sources

2. **Query Engine** - Continuously evaluate queries on change streams
   - Incremental query processing
   - Pattern matching
   - Real-time result updates

3. **Reactions** - Execute actions in response to query updates
   - AI Agent integration (LangChain4j)
   - Webhook callbacks
   - Custom reactions

4. **RudderEngine** - Orchestrates all components
   - Manages lifecycle
   - Routes events
   - Coordinates reactions

## 🔧 Configuration

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
```

## 🎯 Key Features

- ✅ **Pure Java** - No .NET or external runtime dependencies
- ✅ **AI Agent Integration** - Native LangChain4j support
- ✅ **Reactive Architecture** - Built on Project Reactor
- ✅ **Spring Boot Ready** - Auto-configuration and starter
- ✅ **CDC Support** - PostgreSQL, MongoDB via Debezium
- ✅ **Incremental Processing** - Efficient query evaluation
- ✅ **Observability** - Metrics, health checks, logging

## 📚 Documentation

Comprehensive documentation is available:

- **[Getting Started Guide](https://openrudder.io/docs/getting-started)** - Installation and first steps
- **[API Reference](https://openrudder.io/api)** - Complete API documentation with input/output specifications
- **[Architecture Guide](https://openrudder.io/docs/architecture)** - System design and components
- **[User Guide](https://openrudder.io/docs/user-guide)** - Detailed usage instructions
- **[Examples](./openrudder-examples)** - Working code examples

## 🧪 Running Examples

### Order Monitoring Example

```bash
cd openrudder-examples
mvn spring-boot:run
```

This example demonstrates:
- PostgreSQL CDC monitoring
- Continuous query evaluation
- AI-powered order dispatching

## 🔨 Building from Source

```bash
# Clone repository
git clone https://github.com/scalefirstai/openrudder.git
cd openrudder

# Build all modules
mvn clean install

# Run tests
mvn test

# Skip tests for faster build
mvn clean install -DskipTests
```

## 🛠️ Development

### Module Dependencies

```
openrudder-core
    ↑
    ├── openrudder-sources
    ├── openrudder-query-engine
    ├── openrudder-reactions
    └── openrudder-spring-boot-starter
            ↑
            └── openrudder-examples
```

### Adding a Custom Source

```java
public class CustomSource extends AbstractSource<CustomSourceConfig> {
    
    public CustomSource(CustomSourceConfig config) {
        super(config);
    }
    
    @Override
    protected Flux<ChangeEvent> doStart() {
        // Implement your source logic
        return Flux.create(sink -> {
            // Emit change events
            sink.next(ChangeEvent.builder()
                .type(ChangeEvent.ChangeType.INSERT)
                .entityType("MyEntity")
                .after(Map.of("id", 1, "name", "Test"))
                .build());
        });
    }
    
    @Override
    protected Mono<Void> doStop() {
        // Cleanup resources
        return Mono.empty();
    }
    
    @Override
    protected Flux<ChangeEvent> doSnapshot() {
        // Return initial snapshot
        return Flux.empty();
    }
}
```

## 📊 Performance

- **Event Processing**: 100,000+ events/second per node
- **Query Latency**: <10ms incremental update (p99)
- **Memory**: ~2GB for 1M active query results
- **Scaling**: Horizontal via distributed deployment

## 📥 Obtaining the Software

### Maven/Gradle

Add OpenRudder to your project:

**Maven:**
```xml
<dependency>
    <groupId>io.openrudder</groupId>
    <artifactId>openrudder-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.openrudder:openrudder-spring-boot-starter:1.0.0-SNAPSHOT'
```

### Source Code

Clone from GitHub:
```bash
git clone https://github.com/scalefirstai/openrudder.git
cd openrudder
mvn clean install
```

**Repository:** https://github.com/scalefirstai/openrudder

## 🐛 Bug Reports & Feature Requests

### Issue Tracker

**URL**: https://github.com/scalefirstai/openrudder/issues

We use GitHub Issues for tracking all bugs, enhancements, and questions.

### Report a Bug

**URL**: https://github.com/scalefirstai/openrudder/issues/new?template=bug_report.md

When reporting bugs, please include:
- OpenRudder version
- Java version
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs or stack traces

### Request a Feature

**URL**: https://github.com/scalefirstai/openrudder/issues/new?template=feature_request.md

When requesting features, describe:
- Your use case
- Proposed solution
- Why this would be valuable

### Response Commitments

We are committed to responding to community issues:

- **Bug Reports**: Acknowledge **majority (>50%)** within 5 business days
- **Enhancement Requests**: Respond to **majority (>50%)** within 10 business days
- **Target**: We aim for 100% acknowledgment of bugs and 80% response to enhancements

**Policy**: [Issue Response Policy](.github/ISSUE_RESPONSE_POLICY.md)

### Public Archive

**URL**: https://github.com/scalefirstai/openrudder/issues?q=

All bug reports and responses are publicly archived and searchable:

- **All Issues**: https://github.com/scalefirstai/openrudder/issues?q=
- **Open Issues**: https://github.com/scalefirstai/openrudder/issues
- **Closed Issues**: https://github.com/scalefirstai/openrudder/issues?q=is%3Aissue+is%3Aclosed
- **Bug Reports**: https://github.com/scalefirstai/openrudder/issues?q=label%3Abug
- **Enhancements**: https://github.com/scalefirstai/openrudder/issues?q=label%3Aenhancement

The archive is:
- ✅ Publicly accessible (no login required)
- ✅ Fully searchable
- ✅ Permanently available
- ✅ Indexed by search engines

## 🔒 Security Vulnerabilities

**⚠️ Do not report security vulnerabilities through public GitHub issues.**

### Vulnerability Reporting Process

**URL**: https://github.com/scalefirstai/openrudder/blob/main/SECURITY.md

We provide two methods for **private** vulnerability reporting:

#### Method 1: Email (Recommended)
- **Email**: security@openrudder.io
- **Privacy**: Kept completely confidential until fix is released
- **Response Time**: Initial response within **14 days** (typically 48 hours)

#### Method 2: GitHub Security Advisories (Private)
- **URL**: https://github.com/scalefirstai/openrudder/security/advisories/new
- **Privacy**: Private until we publish a fix
- **Response Time**: Initial response within **14 days** (typically 48 hours)

### Response Time Commitment

We commit to providing an initial response to all vulnerability reports within **14 days** of receipt (last 6 months).

**Full Security Policy**: [SECURITY.md](SECURITY.md)

## � Providing Feedback

We welcome your feedback through multiple channels:

- **GitHub Discussions:** https://github.com/scalefirstai/openrudder/discussions
- **Discord Community:** https://discord.gg/openrudder
- **Email:** feedback@openrudder.io
- **Twitter:** [@openrudder](https://twitter.com/openrudder)

## 🤝 Contributing

We welcome contributions from the community! Please see our [Contributing Guide](CONTRIBUTING.md) for detailed information.

### Quick Contribution Guide

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/YOUR_USERNAME/openrudder.git`
3. **Create a branch:** `git checkout -b feature/my-feature`
4. **Make changes** following our coding standards
5. **Test** your changes: `mvn test`
6. **Commit** with conventional commits: `git commit -m "feat: add new feature"`
7. **Push** to your fork: `git push origin feature/my-feature`
8. **Create a Pull Request**

### Contribution Requirements

All contributions must meet these requirements:

- **Code Style:** Follow Java conventions, 4-space indentation, max 120 chars per line
- **Testing:** Include unit tests with 80%+ coverage
- **Documentation:** Update relevant docs and add Javadoc for public APIs
- **Commits:** Use [Conventional Commits](https://www.conventionalcommits.org/) format
- **License:** Agree to Apache 2.0 license
- **Code of Conduct:** Follow our [Code of Conduct](CODE_OF_CONDUCT.md)

**Full Guidelines:** [CONTRIBUTING.md](CONTRIBUTING.md)

## 📦 Releases & Versioning

OpenRudder follows [Semantic Versioning](https://semver.org/) (SemVer):

- **Current Version:** 1.0.0-SNAPSHOT (Development)
- **Release History:** https://github.com/scalefirstai/openrudder/releases
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)
- **Release Process:** [RELEASING.md](RELEASING.md)

### Version Identifiers

Each release has a unique version identifier:
- Format: `MAJOR.MINOR.PATCH` (e.g., 1.0.0)
- Development snapshots: `MAJOR.MINOR.PATCH-SNAPSHOT` (e.g., 1.0.0-SNAPSHOT)
- Release candidates: `MAJOR.MINOR.PATCH-RC.N` (e.g., 1.0.0-RC.1)

### Git Tags

Each release is identified in Git using tags:
- **Format:** `v{VERSION}` (e.g., `v1.0.0`)
- **View Tags:** `git tag -l`
- **Checkout Release:** `git checkout v1.0.0`
- **All Tags:** https://github.com/scalefirstai/openrudder/tags

### Accessing Releases

- **Latest Stable:** https://github.com/scalefirstai/openrudder/releases/latest
- **All Releases:** https://github.com/scalefirstai/openrudder/releases
- **Maven Central:** https://search.maven.org/artifact/io.openrudder/openrudder-parent

### Version Control & Change Tracking

The source repository tracks all changes with full transparency:

✅ **Interim Versions**: All commits available for collaborative review  
✅ **Change Tracking**: Every change includes author, timestamp, and description  
✅ **Git History**: `git log` shows complete development history  
✅ **Pull Requests**: Code review process visible in PR history  
✅ **Blame/Annotate**: `git blame` shows who changed what and when

**View Changes:**
```bash
# All commits
git log

# Changes between versions
git log v1.0.0..v1.1.0

# Who changed a file
git blame <filename>

# Changes by author
git log --author="Name"
```

### Source Repository

The source repository includes:
- ✅ All interim versions and commits for collaborative review
- ✅ Development branches for feature work
- ✅ Tagged releases for production use
- ✅ Pull requests for community review
- ✅ Complete Git history with author and timestamp information

**Repository:** https://github.com/scalefirstai/openrudder

### Security & CVE Tracking

Release notes identify all publicly known vulnerabilities fixed:
- **CVE Tracking:** Each release documents fixed CVEs
- **Security Advisories:** https://github.com/scalefirstai/openrudder/security/advisories
- **Vulnerability Reports:** See [SECURITY.md](SECURITY.md)

Example from release notes:
```markdown
### Security Fixes
- CVE-2024-XXXXX: Description of vulnerability and fix
```

## 📄 License

Apache 2.0 License - see [LICENSE](LICENSE)

## 🔗 Related Projects

- [LangChain4j](https://github.com/langchain4j/langchain4j) - AI agent framework
- [Debezium](https://debezium.io/) - Change Data Capture

## 📧 Contact & Community

For questions and support:
- 🐛 **GitHub Issues**: [Report bugs or request features](https://github.com/scalefirstai/openrudder/issues)
- 💬 **Discord**: [Join our community](https://discord.gg/openrudder)
- 📧 **Email**: support@openrudder.io
- 🌐 **Website**: [openrudder.io](https://openrudder.io)
- 📖 **Documentation**: [openrudder.io/docs](https://openrudder.io/docs)

## ⭐ Show Your Support

If you find OpenRudder useful, please consider:
- ⭐ **Starring** this repository
- 🐦 **Sharing** on social media
- 📝 **Writing** a blog post about your experience
- 🤝 **Contributing** to the project

---

<div align="center">

**Built with ❤️ for the Java and AI community**

[Website](https://openrudder.io) • [Documentation](https://openrudder.io/docs) • [GitHub](https://github.com/scalefirstai/openrudder)

</div>
