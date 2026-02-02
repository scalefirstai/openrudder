## 📝 Description

<!-- Provide a clear and concise description of your changes -->

## 🔗 Related Issues

<!-- Link related issues using "Fixes #123" or "Closes #456" -->

Fixes #

## 🎯 Type of Change

<!-- Mark the relevant option with an "x" -->

- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📝 Documentation update
- [ ] 🔧 Refactoring (no functional changes)
- [ ] ⚡ Performance improvement
- [ ] 🧪 Test update

## ✅ Checklist

<!-- Mark completed items with an "x" -->

### Code Quality
- [ ] My code follows the project's code style guidelines
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] My changes generate no new warnings or compiler errors
- [ ] I have addressed all linter warnings
- [ ] Code passes `mvn checkstyle:check`

### Testing (Required - See [TEST_POLICY.md](../TEST_POLICY.md))
- [ ] I have added tests for all new functionality (unit + integration)
- [ ] Tests achieve 80%+ coverage for new code
- [ ] All new and existing tests pass locally: `mvn test`
- [ ] Coverage report generated: `mvn clean test jacoco:report`
- [ ] Tests follow naming conventions and Given-When-Then structure
- [ ] Tests are deterministic and independent

### Documentation
- [ ] I have made corresponding changes to the documentation
- [ ] I have updated the CHANGELOG.md file
- [ ] Any dependent changes have been merged and published

## 🧪 Testing

<!-- Describe the tests you ran and how to reproduce them -->

### Test Policy Compliance

**For major new functionality, tests MUST be added per [TEST_POLICY.md](../TEST_POLICY.md)**

- [ ] This PR adds major new functionality
- [ ] Tests have been added for new functionality
- [ ] Coverage meets 80% threshold for new code

### Test Configuration

- **Java Version**: 
- **Maven Version**: 
- **OS**: 

### Test Results

```bash
# Run tests and paste output
mvn clean test

# Generate coverage report
mvn jacoco:report

# Coverage for new code: XX%
```

### Test Files Added/Modified

<!-- List test files added or modified -->

- `src/test/java/...`

## 📸 Screenshots (if applicable)

<!-- Add screenshots to help explain your changes -->

## 📚 Documentation

<!-- List any documentation that needs to be updated -->

- [ ] README.md
- [ ] API documentation
- [ ] Architecture documentation
- [ ] Example code

## 🔍 Additional Notes

<!-- Add any additional notes for reviewers -->

## 🤝 Reviewer Checklist

<!-- For maintainers -->

### Code Quality
- [ ] Code quality and style verified
- [ ] No compiler warnings introduced
- [ ] Linter checks pass
- [ ] Checkstyle passes

### Test Policy Compliance
- [ ] Tests added for all new functionality (per [TEST_POLICY.md](../TEST_POLICY.md))
- [ ] Test coverage meets 80% for new code
- [ ] Tests are meaningful and maintainable
- [ ] All tests pass in CI

### General
- [ ] Documentation completeness
- [ ] Breaking changes identified and documented
- [ ] Performance impact assessed
- [ ] Security implications reviewed
