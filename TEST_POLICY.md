# Test Policy for OpenRudder

## Policy Statement

**As major new functionality is added to OpenRudder, tests of that functionality MUST be added to the automated test suite.**

This policy ensures that all new features are properly tested, maintaining code quality and preventing regressions.

## Scope

This policy applies to:
- ✅ All new features and major functionality
- ✅ Bug fixes that add new code paths
- ✅ API changes and enhancements
- ✅ Performance improvements
- ✅ Security fixes

## Requirements

### 1. Test Coverage for New Functionality

All pull requests that add major new functionality MUST include:

- **Unit Tests**: Test individual classes and methods in isolation
  - Minimum 80% line coverage for new code
  - Test all public APIs
  - Test edge cases and error conditions

- **Integration Tests**: Test component interactions (when applicable)
  - Test end-to-end workflows
  - Test integration points between modules

- **Documentation**: Update test documentation
  - Add examples to TESTING.md if introducing new test patterns
  - Document any test fixtures or utilities

### 2. Definition of "Major New Functionality"

Major new functionality includes:

- New modules or components
- New public APIs or interfaces
- New data sources or integrations
- New query capabilities
- New reaction types
- Significant algorithm changes
- New configuration options with behavioral impact

### 3. Test Requirements by Change Type

| Change Type | Test Requirement |
|-------------|------------------|
| New Feature | Unit + Integration tests, 80%+ coverage |
| Bug Fix | Test reproducing the bug + regression test |
| Refactoring | Maintain existing test coverage |
| Documentation | No test requirement |
| Configuration | Test configuration validation |
| Performance | Performance benchmark tests |

## Implementation

### For Contributors

When submitting a pull request with new functionality:

1. **Write Tests First** (TDD approach recommended)
   ```bash
   # Create test file
   touch src/test/java/io/openrudder/MyNewFeatureTest.java
   
   # Write failing tests
   # Implement feature
   # Verify tests pass
   mvn test
   ```

2. **Verify Coverage**
   ```bash
   mvn clean test jacoco:report
   open target/site/jacoco/index.html
   ```

3. **Document Tests**
   - Add test descriptions in PR
   - Update TESTING.md if needed

4. **PR Checklist**
   - [ ] Tests added for new functionality
   - [ ] All tests pass locally
   - [ ] Coverage meets 80% threshold for new code
   - [ ] Tests follow project conventions
   - [ ] Integration tests added (if applicable)

### For Reviewers

Reviewers MUST verify:

- [ ] Tests are present for all new functionality
- [ ] Tests are meaningful (not just for coverage)
- [ ] Tests follow naming conventions
- [ ] Coverage reports show adequate coverage
- [ ] Tests are maintainable and clear
- [ ] CI pipeline passes all tests

## Enforcement

### Automated Enforcement

1. **CI Pipeline**: GitHub Actions automatically:
   - Runs all tests on every PR
   - Generates coverage reports
   - Fails build if tests fail
   - Comments coverage on PR

2. **Coverage Checks**: JaCoCo enforces:
   - Minimum 60% overall coverage
   - Target 80% for new code
   - Build fails if coverage drops

3. **PR Checks**: Required before merge:
   - ✅ All tests pass
   - ✅ Coverage requirements met
   - ✅ No test failures
   - ✅ Code review approval

### Manual Review

Maintainers will:
- Review test quality during PR review
- Request additional tests if coverage is insufficient
- Ensure tests are meaningful, not just for metrics
- Verify integration tests for complex features

## Evidence of Adherence

### Recent Major Changes

Evidence that this policy has been followed:

1. **Test Files Created**: See `openrudder-core/src/test/java/`
   - `ChangeEventTest.java` - 10 test cases for domain models
   - `ContinuousQueryTest.java` - 9 test cases for query evaluation
   - `InMemoryResultSetCacheTest.java` - 13 test cases for caching

2. **Coverage Reports**: Generated automatically in CI
   - Available in GitHub Actions artifacts
   - Published to Codecov
   - Visible in PR comments

3. **CI Integration**: See `.github/workflows/build.yml`
   - Automated test execution
   - Coverage reporting
   - Test result publishing

4. **Documentation**: Test policy documented in:
   - This file (TEST_POLICY.md)
   - CONTRIBUTING.md
   - Pull request template
   - BUILD.md and TESTING.md

### Tracking Compliance

Compliance is tracked through:
- **PR Reviews**: All PRs reviewed for test coverage
- **CI Reports**: Automated coverage reports on every PR
- **Coverage Trends**: Codecov tracks coverage over time
- **Test Count**: Surefire reports show test execution

## Exceptions

Exceptions to this policy may be granted for:

1. **Emergency Hotfixes**: Security or critical bug fixes
   - Tests should be added in follow-up PR
   - Exception must be documented in PR

2. **Experimental Features**: Behind feature flags
   - Tests required before removing flag
   - Basic smoke tests still required

3. **Documentation-Only Changes**: No code changes
   - No test requirement

All exceptions must be:
- Approved by maintainers
- Documented in PR description
- Tracked for follow-up

## Test Quality Standards

Tests MUST:
- ✅ Be deterministic (no flaky tests)
- ✅ Run quickly (unit tests < 1s each)
- ✅ Be independent (no test dependencies)
- ✅ Have clear names describing what they test
- ✅ Follow Given-When-Then structure
- ✅ Use appropriate assertions
- ✅ Clean up resources properly

Tests SHOULD:
- Use test fixtures for common setup
- Mock external dependencies
- Test one thing per test method
- Include both positive and negative cases
- Test boundary conditions

## Tools and Frameworks

All testing tools are FLOSS:

- **JUnit 5** (EPL 2.0) - Test framework
- **Mockito** (MIT) - Mocking framework
- **Reactor Test** (Apache 2.0) - Reactive testing
- **JaCoCo** (EPL 2.0) - Code coverage
- **Maven Surefire** (Apache 2.0) - Test runner

## Resources

- **Testing Guide**: [TESTING.md](TESTING.md)
- **Build Guide**: [BUILD.md](BUILD.md)
- **Contributing Guide**: [CONTRIBUTING.md](CONTRIBUTING.md)
- **CI Configuration**: `.github/workflows/build.yml`

## Policy Updates

This policy may be updated by:
- Maintainer consensus
- Community discussion in GitHub Discussions
- PR to this document

Changes to this policy require:
- Documentation update
- Communication to contributors
- Update to PR template and CONTRIBUTING.md

## Contact

Questions about this policy:
- **GitHub Discussions**: https://github.com/scalefirstai/openrudder/discussions
- **Discord**: https://discord.gg/openrudder
- **Email**: dev@openrudder.io

---

**Policy Version**: 1.0  
**Last Updated**: 2026-02-01  
**Status**: Active  
**Applies To**: All contributions to OpenRudder
