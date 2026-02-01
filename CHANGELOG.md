# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Version Control & Release Information

- **Repository**: https://github.com/scalefirstai/openrudder
- **Version Format**: Semantic Versioning (SemVer) - `MAJOR.MINOR.PATCH`
- **Git Tags**: Each release is tagged in Git (e.g., `v1.0.0`)
- **Change Tracking**: All changes tracked via Git commits with author, timestamp, and description
- **Interim Versions**: Full Git history available for collaborative review

## [Unreleased]

### Added
- Initial release of OpenRudder
- Core event-driven architecture with RudderEngine
- PostgreSQL CDC support via Debezium
- MongoDB Change Streams support
- Kafka topic integration
- Continuous query engine with incremental processing
- LangChain4j AI agent integration
- Spring Boot auto-configuration and starter
- Reactive architecture built on Project Reactor
- Comprehensive documentation and examples
- Docker Compose setup for local development

### Features
- **Sources**: PostgreSQL CDC, MongoDB Change Streams, Kafka
- **Query Engine**: Pattern matching, incremental updates, temporal queries
- **Reactions**: AI agents, webhooks, custom reactions
- **Observability**: Metrics, health checks, structured logging
- **Spring Boot**: Auto-configuration, starter module

### Security
- No publicly known vulnerabilities fixed in this release
- No CVEs applicable to this initial release

## [1.0.0-SNAPSHOT] - 2024-02-01

### Initial Development Release

**Git Tag**: `v1.0.0-SNAPSHOT`  
**Release Date**: February 1, 2024  
**Git Commit**: See [commit history](https://github.com/scalefirstai/openrudder/commits/main)

This is the first development snapshot of OpenRudder, featuring:

#### Core Components
- Event-driven architecture for AI agents
- Change Data Capture (CDC) integration
- Continuous query processing
- Reactive streams with Project Reactor

#### Data Sources
- PostgreSQL CDC via Debezium
- MongoDB Change Streams
- Apache Kafka topics

#### Query Engine
- Cypher-like query language
- Incremental query evaluation
- Pattern matching on change streams
- Real-time result updates

#### AI Integration
- LangChain4j integration for AI agents
- OpenAI GPT support
- Custom AI model integration
- Prompt engineering support

#### Spring Boot Support
- Auto-configuration
- Starter module for easy integration
- Configuration properties
- Health indicators

#### Developer Experience
- Comprehensive documentation
- Example applications
- Docker Compose setup
- Maven multi-module structure

#### Security
- **CVEs Fixed**: None (initial release)
- **Known Vulnerabilities**: None at time of release
- **Security Advisories**: https://github.com/scalefirstai/openrudder/security/advisories

---

## Release Notes Format

### Added
- New features and capabilities

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security vulnerability fixes

---

[Unreleased]: https://github.com/scalefirstai/openrudder/compare/v1.0.0...HEAD
[1.0.0-SNAPSHOT]: https://github.com/scalefirstai/openrudder/releases/tag/v1.0.0-SNAPSHOT
