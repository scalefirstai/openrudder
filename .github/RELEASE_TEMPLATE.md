# Release Notes Template

## Release [VERSION] - [DATE]

**Git Tag**: `v[VERSION]`  
**Release Date**: [DATE]  
**Git Commit**: [COMMIT_HASH]

### Overview

[Brief description of this release]

### What's New

#### Added
- [New feature 1]
- [New feature 2]

#### Changed
- [Changed functionality 1]
- [Changed functionality 2]

#### Fixed
- [Bug fix 1]
- [Bug fix 2]

#### Deprecated
- [Deprecated feature 1]

#### Removed
- [Removed feature 1]

### Security Fixes

**REQUIRED**: List all publicly known vulnerabilities fixed in this release that had CVE assignments or similar when the release was created.

#### CVEs Fixed
- **CVE-YYYY-XXXXX**: [Description of vulnerability and fix]
- **CVE-YYYY-XXXXX**: [Description of vulnerability and fix]

If no CVEs were fixed, state:
- **CVEs Fixed**: None

#### Security Advisories
- Link to security advisories: https://github.com/scalefirstai/openrudder/security/advisories
- Link to this release's security notes: [URL]

### Breaking Changes

⚠️ **Important**: This release contains breaking changes.

- [Breaking change 1 with migration guide]
- [Breaking change 2 with migration guide]

### Installation

**Maven:**
```xml
<dependency>
    <groupId>io.openrudder</groupId>
    <artifactId>openrudder-spring-boot-starter</artifactId>
    <version>[VERSION]</version>
</dependency>
```

**Gradle:**
```gradle
implementation 'io.openrudder:openrudder-spring-boot-starter:[VERSION]'
```

### Upgrade Guide

[Instructions for upgrading from previous version]

### Contributors

Thank you to all contributors who made this release possible:
- @contributor1
- @contributor2

Full list: https://github.com/scalefirstai/openrudder/graphs/contributors

### Checksums

```
SHA256: [checksum]
```

---

## Version Control Information

- **Repository**: https://github.com/scalefirstai/openrudder
- **Full Changelog**: https://github.com/scalefirstai/openrudder/compare/v[PREV_VERSION]...v[VERSION]
- **All Commits**: https://github.com/scalefirstai/openrudder/commits/v[VERSION]
- **Release Tag**: https://github.com/scalefirstai/openrudder/releases/tag/v[VERSION]
