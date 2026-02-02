# Code Quality and Warning Policy

## Policy Statement

**OpenRudder MUST enable compiler warnings and use linter tools to detect code quality errors and common mistakes. All warnings MUST be addressed.**

This policy ensures high code quality, prevents common bugs, and maintains consistency across the codebase.

## Compiler Warnings

### Enabled Warnings

OpenRudder enables **all recommended Java compiler warnings** with maximum strictness:

```xml
<compilerArgs>
    <arg>-Xlint:all</arg>          <!-- Enable all warnings -->
    <arg>-Werror</arg>              <!-- Treat warnings as errors -->
    <arg>-Xlint:deprecation</arg>   <!-- Deprecated API usage -->
    <arg>-Xlint:unchecked</arg>     <!-- Unchecked operations -->
    <arg>-Xlint:rawtypes</arg>      <!-- Raw type usage -->
    <arg>-Xlint:cast</arg>          <!-- Unnecessary casts -->
    <arg>-Xlint:serial</arg>        <!-- Serialization issues -->
    <arg>-Xlint:finally</arg>       <!-- Finally block issues -->
    <arg>-Xlint:fallthrough</arg>   <!-- Switch fallthrough -->
    <arg>-Xlint:empty</arg>         <!-- Empty statements -->
</compilerArgs>
```

### Configuration

See `pom.xml` lines 125-143 for complete compiler configuration.

### Checking for Warnings

```bash
# Compile and check for warnings
mvn clean compile

# All warnings will be displayed in console output
```

## Linter Tools (FLOSS)

OpenRudder uses multiple FLOSS linter tools to ensure code quality:

### 1. Checkstyle

**Purpose**: Enforce coding standards and style guidelines  
**License**: LGPL 2.1+  
**Configuration**: Google Java Style Guide

```bash
# Run Checkstyle
mvn checkstyle:check

# View report
open target/site/checkstyle.html
```

**Checks include**:
- Code formatting and indentation
- Naming conventions
- Javadoc requirements
- Import organization
- Code complexity

### 2. PMD

**Purpose**: Detect common programming flaws  
**License**: Apache 2.0  
**Configuration**: Default ruleset with Java 21 target

```bash
# Run PMD
mvn pmd:check

# Run copy-paste detection
mvn pmd:cpd-check

# View report
open target/site/pmd.html
```

**Checks include**:
- Unused variables and imports
- Empty catch blocks
- Unnecessary object creation
- Complex expressions
- Code duplication

### 3. SpotBugs (formerly FindBugs)

**Purpose**: Find bugs through static analysis  
**License**: LGPL 2.1  
**Configuration**: Maximum effort, low threshold

```bash
# Run SpotBugs
mvn spotbugs:check

# View GUI report
mvn spotbugs:gui

# View report
open target/spotbugsXml.xml
```

**Checks include**:
- Null pointer dereferences
- Resource leaks
- Concurrency issues
- Security vulnerabilities
- Performance problems

## Policy Requirements

### For All Code

1. **Zero Compiler Warnings**: Code MUST compile without warnings
2. **Address Linter Issues**: All linter warnings MUST be addressed
3. **Justify Suppressions**: Any warning suppressions MUST be justified

### For Pull Requests

PRs MUST:
- [ ] Compile without warnings
- [ ] Pass Checkstyle checks
- [ ] Pass PMD checks
- [ ] Pass SpotBugs checks
- [ ] Address all code quality issues

### Strictness Level

OpenRudder follows **maximum strictness** where practical:

- ✅ All compiler warnings enabled
- ✅ Warnings treated as errors during development
- ✅ Multiple linter tools for comprehensive coverage
- ✅ Automated checks in CI pipeline
- ⚠️ Some checks set to `failOnError=false` to allow gradual improvement

## Running All Quality Checks

### Local Development

```bash
# Run all quality checks
mvn clean verify

# This runs:
# 1. Compilation with warnings
# 2. Checkstyle
# 3. PMD
# 4. SpotBugs
# 5. Tests
# 6. Coverage
```

### Quick Check

```bash
# Just code quality (no tests)
mvn checkstyle:check pmd:check spotbugs:check
```

### CI Pipeline

GitHub Actions automatically runs all quality checks on every push and PR:

1. ✅ Compiler warnings check
2. ✅ Checkstyle validation
3. ✅ PMD analysis
4. ✅ SpotBugs analysis
5. ✅ Test execution
6. ✅ Coverage reporting

Reports are uploaded as artifacts for review.

## Addressing Warnings

### Compiler Warnings

**Fix the code** to eliminate the warning:

```java
// ❌ Bad - generates unchecked warning
List list = new ArrayList();

// ✅ Good - no warning
List<String> list = new ArrayList<>();
```

### Checkstyle Violations

Follow the Google Java Style Guide:

```java
// ❌ Bad - naming violation
private String MyVariable;

// ✅ Good - follows convention
private String myVariable;
```

### PMD Issues

Simplify and improve code:

```java
// ❌ Bad - unused variable
public void process() {
    String unused = "test";
    // ...
}

// ✅ Good - remove unused code
public void process() {
    // ...
}
```

### SpotBugs Issues

Fix potential bugs:

```java
// ❌ Bad - potential null pointer
public void process(String value) {
    int length = value.length(); // NPE if value is null
}

// ✅ Good - null check
public void process(String value) {
    if (value != null) {
        int length = value.length();
    }
}
```

## Suppressing Warnings

### When to Suppress

Suppress warnings ONLY when:
- The warning is a false positive
- The code is intentionally written that way
- Fixing would make code less readable/maintainable

### How to Suppress

Use `@SuppressWarnings` with justification:

```java
// Suppress with clear reason
@SuppressWarnings("unchecked") // Safe: checked at runtime
public <T> T cast(Object obj) {
    return (T) obj;
}
```

### Documentation Required

All suppressions MUST include:
- Reason for suppression
- Why it's safe
- Alternative approaches considered

## Tool Configuration Files

### Checkstyle

Location: Uses built-in `google_checks.xml`

To customize, create `checkstyle.xml` in project root.

### PMD

Location: Uses default ruleset

To customize, create `pmd-ruleset.xml`:

```xml
<?xml version="1.0"?>
<ruleset name="OpenRudder PMD Rules">
    <rule ref="category/java/bestpractices.xml"/>
    <rule ref="category/java/codestyle.xml"/>
    <rule ref="category/java/design.xml"/>
    <rule ref="category/java/errorprone.xml"/>
    <rule ref="category/java/performance.xml"/>
</ruleset>
```

### SpotBugs

Location: Uses default configuration

To customize, create `spotbugs-exclude.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Exclude specific patterns if needed -->
</FindBugsFilter>
```

## IDE Integration

### IntelliJ IDEA

1. **Checkstyle Plugin**: Install "Checkstyle-IDEA" plugin
2. **PMD Plugin**: Install "PMDPlugin" plugin
3. **SpotBugs Plugin**: Install "SpotBugs" plugin
4. **Configure**: Point to project's configuration files

### Eclipse

1. **Checkstyle Plugin**: Install from Eclipse Marketplace
2. **PMD Plugin**: Install from Eclipse Marketplace
3. **SpotBugs Plugin**: Install from Eclipse Marketplace

### VS Code

1. **Java Extension Pack**: Includes basic linting
2. **Checkstyle for Java**: Install extension
3. **SonarLint**: Additional quality checks

## Metrics and Reporting

### Quality Metrics Tracked

- Compiler warning count (target: 0)
- Checkstyle violations (target: 0)
- PMD issues (target: minimize)
- SpotBugs bugs (target: 0)
- Code coverage (target: 80%+)

### Reports Location

After running checks:

```
target/
├── checkstyle-result.xml
├── site/
│   ├── checkstyle.html
│   ├── pmd.html
│   └── spotbugs.html
├── pmd.xml
└── spotbugsXml.xml
```

### CI Artifacts

All reports uploaded to GitHub Actions artifacts:
- `code-quality-reports` - Checkstyle, PMD, SpotBugs
- `test-results` - Test execution results
- `coverage-reports` - JaCoCo coverage

## Continuous Improvement

### Regular Reviews

- Weekly: Review quality metrics
- Monthly: Update tool configurations
- Quarterly: Evaluate new tools and practices

### Tool Updates

Keep tools up to date:

```bash
# Check for plugin updates
mvn versions:display-plugin-updates

# Update versions in pom.xml
```

## Resources

### Documentation

- **Checkstyle**: https://checkstyle.org/
- **PMD**: https://pmd.github.io/
- **SpotBugs**: https://spotbugs.github.io/
- **Google Java Style**: https://google.github.io/styleguide/javaguide.html

### Internal Documentation

- [BUILD.md](BUILD.md) - Build instructions
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines
- [TEST_POLICY.md](TEST_POLICY.md) - Testing requirements

## Getting Help

Questions about code quality:
- **GitHub Discussions**: https://github.com/scalefirstai/openrudder/discussions
- **Discord**: https://discord.gg/openrudder
- **Email**: dev@openrudder.io

---

**Policy Version**: 1.0  
**Last Updated**: 2026-02-01  
**Status**: Active  
**All Tools**: 100% FLOSS (Free/Libre and Open Source Software)
