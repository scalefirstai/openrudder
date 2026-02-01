# Release Process

This document describes the release process for OpenRudder.

## Version Control Requirements (OpenSSF)

OpenRudder follows OpenSSF Best Practices for version control and releases:

✅ **Interim Versions**: Full Git history is available for collaborative review  
✅ **Change Tracking**: All changes tracked with author, timestamp, and description via Git  
✅ **Version Identifiers**: Each release has a unique SemVer identifier  
✅ **Git Tags**: Each release is tagged in Git  
✅ **CVE Tracking**: Release notes identify all publicly known vulnerabilities fixed

## Versioning

OpenRudder uses [Semantic Versioning](https://semver.org/) (SemVer):

- **Format**: `MAJOR.MINOR.PATCH`
- **Example**: `1.2.3`

### Version Components

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

### Pre-release Versions

- **Snapshot**: `1.0.0-SNAPSHOT` (development)
- **Release Candidate**: `1.0.0-RC.1` (testing)
- **Beta**: `1.0.0-beta.1` (early access)
- **Alpha**: `1.0.0-alpha.1` (experimental)

## Release Checklist

### 1. Pre-Release

- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG.md updated with all changes
- [ ] Security vulnerabilities reviewed
- [ ] CVEs documented (if any)
- [ ] Breaking changes documented
- [ ] Migration guide written (if needed)
- [ ] Version number decided

### 2. Version Update

Update version in all `pom.xml` files:

```bash
mvn versions:set -DnewVersion=1.0.0
mvn versions:commit
```

Update `VERSION` file:
```bash
echo "1.0.0" > VERSION
```

### 3. Update CHANGELOG

Add release section to `CHANGELOG.md`:

```markdown
## [1.0.0] - 2024-02-01

### Git Tag
v1.0.0

### Security
- **CVEs Fixed**: CVE-2024-XXXXX - [Description]
- **Known Vulnerabilities**: None

### Added
- Feature 1
- Feature 2

### Changed
- Change 1

### Fixed
- Bug fix 1
```

### 4. Commit and Tag

```bash
# Commit version changes
git add .
git commit -m "chore: release version 1.0.0"

# Create annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0

- Feature 1
- Feature 2
- Security: Fixed CVE-2024-XXXXX"

# Push commits and tags
git push origin main
git push origin v1.0.0
```

### 5. Build and Test

```bash
# Clean build
mvn clean install

# Run all tests
mvn verify

# Build distribution
mvn package
```

### 6. Create GitHub Release

1. Go to https://github.com/scalefirstai/openrudder/releases/new
2. Select tag: `v1.0.0`
3. Release title: `OpenRudder v1.0.0`
4. Copy content from CHANGELOG.md
5. **IMPORTANT**: Add Security section with CVEs
6. Attach artifacts (if any)
7. Publish release

### 7. Deploy to Maven Central

```bash
# Deploy to Maven Central
mvn clean deploy -P release
```

### 8. Post-Release

- [ ] Announce on Discord
- [ ] Tweet announcement
- [ ] Update website
- [ ] Update documentation site
- [ ] Send newsletter
- [ ] Update VERSION to next SNAPSHOT

```bash
mvn versions:set -DnewVersion=1.1.0-SNAPSHOT
mvn versions:commit
echo "1.1.0-SNAPSHOT" > VERSION
git add .
git commit -m "chore: prepare for next development iteration"
git push origin main
```

## Security Release Process

For releases that fix security vulnerabilities:

### 1. Private Disclosure

- Receive vulnerability report at security@openrudder.io
- Confirm and validate vulnerability
- Assign CVE if needed
- Develop fix in private branch

### 2. Security Advisory

Create GitHub Security Advisory:
1. Go to https://github.com/scalefirstai/openrudder/security/advisories
2. Click "New draft security advisory"
3. Fill in CVE details
4. Set severity
5. Add affected versions
6. Add patched versions

### 3. Release

- Follow normal release process
- **MUST** include CVE in release notes
- Publish security advisory
- Notify users via security mailing list

### 4. Release Notes Format for Security Fixes

```markdown
## [1.0.1] - 2024-02-15

### Security Fixes

**CRITICAL**: This release fixes publicly known vulnerabilities.

#### CVEs Fixed
- **CVE-2024-12345**: SQL Injection in query parser
  - **Severity**: High (CVSS 7.5)
  - **Impact**: Allows unauthorized database access
  - **Fix**: Input validation added to query parser
  - **Credit**: @security-researcher

#### Security Advisories
- https://github.com/scalefirstai/openrudder/security/advisories/GHSA-xxxx-xxxx-xxxx
```

## Git Workflow

### Branches

- `main`: Stable, production-ready code
- `develop`: Integration branch for features
- `feature/*`: Feature branches
- `hotfix/*`: Emergency fixes
- `release/*`: Release preparation

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### Change Tracking

Every commit includes:
- **Author**: Git author name and email
- **Timestamp**: Commit date and time
- **Description**: Commit message
- **Changes**: File diffs

View history:
```bash
# All commits
git log

# Commits for specific release
git log v1.0.0..v1.1.0

# Changes by author
git log --author="John Doe"

# Changes in date range
git log --since="2024-01-01" --until="2024-02-01"
```

## Interim Versions

All development work is tracked in Git:

- **Commits**: Every change is a commit
- **Pull Requests**: Code review before merge
- **Branches**: Feature development visible
- **Tags**: Milestones and releases marked

View interim versions:
```bash
# All commits between releases
git log v1.0.0..v1.1.0

# Browse code at any point
git checkout <commit-hash>

# Compare versions
git diff v1.0.0 v1.1.0
```

## Release Artifacts

Each release includes:

- **Source code** (zip, tar.gz)
- **JAR files** (Maven Central)
- **Documentation** (website)
- **CHANGELOG** (GitHub)
- **Git tag** (repository)
- **Release notes** (GitHub Releases)

## Verification

Verify a release:

```bash
# Check tag exists
git tag -l v1.0.0

# View tag details
git show v1.0.0

# Verify tag signature (if signed)
git tag -v v1.0.0

# Check Maven Central
curl https://search.maven.org/artifact/io.openrudder/openrudder-parent/1.0.0/pom
```

---

**Questions?** Contact: release@openrudder.io
