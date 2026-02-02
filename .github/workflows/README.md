# 🤖 Claude AI Continuous Refactoring System

This directory contains GitHub Actions workflows that provide automated continuous refactoring assistance for the OpenRudder project using Claude AI.

## 📋 Overview

The system consists of two main workflows:

1. **Continuous Refactoring** (`claude-refactor-workflow.yml`) - Runs on every push/PR to provide immediate feedback
2. **Weekly Comprehensive Analysis** (`weekly-refactoring-workflow.yml`) - Deep architectural review every Monday

## 🚀 Setup Instructions

### 1. Add Anthropic API Key

Add your Anthropic API key to GitHub Secrets:

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Name: `ANTHROPIC_API_KEY`
5. Value: Your Anthropic API key (get one at https://console.anthropic.com)
6. Click **Add secret**

### 2. Copy Workflow Files

Copy both workflow files to your repository:

```bash
# From your local machine
mkdir -p .github/workflows
cp claude-refactor-workflow.yml .github/workflows/
cp weekly-refactoring-workflow.yml .github/workflows/
```

### 3. Enable GitHub Actions

Ensure GitHub Actions is enabled for your repository:

1. Go to **Settings** → **Actions** → **General**
2. Under **Actions permissions**, select **Allow all actions and reusable workflows**
3. Under **Workflow permissions**, select **Read and write permissions**
4. Check **Allow GitHub Actions to create and approve pull requests**
5. Click **Save**

### 4. Commit and Push

```bash
git add .github/workflows/
git commit -m "feat: add Claude AI continuous refactoring workflows"
git push origin main
```

## 🎯 How It Works

### Continuous Refactoring Workflow

**Triggers:**
- Push to `main` branch (Java files only)
- Pull Request to `main` (Java files only)
- Manual dispatch with options

**Process:**
1. Checks out code and builds the project
2. Runs static analysis tools (PMD, SpotBugs, Checkstyle)
3. Collects code metrics
4. Sends changed files to Claude AI for analysis
5. Claude provides specific refactoring suggestions with:
   - File and line numbers
   - Severity rating
   - Category (performance, security, etc.)
   - Before/after code examples
6. Creates a new branch with suggestions
7. Opens a PR for review (if changes are significant)
8. Comments on existing PRs with feedback

### Weekly Comprehensive Analysis

**Triggers:**
- Every Monday at 9 AM UTC
- Manual dispatch

**Process:**
1. Analyzes each module separately
2. Provides architectural recommendations
3. Checks dependency updates
4. Generates comprehensive report
5. Creates a tracking issue with all findings

## 📊 What Claude Analyzes

### Code Quality
- Complexity reduction
- Readability improvements
- Code duplication
- Naming conventions
- Documentation (JavaDoc)

### Performance
- Reactive stream optimization
- Memory allocation patterns
- Database query efficiency
- Caching opportunities

### Security
- Input validation
- SQL injection risks
- Authentication/authorization
- Sensitive data handling

### Architecture
- Module cohesion/coupling
- Interface design
- Dependency management
- Design pattern usage

### Best Practices
- Spring Boot conventions
- Reactive programming patterns
- Exception handling
- Logging strategies

## 🎮 Manual Usage

### Analyze Specific Module

```bash
# Via GitHub UI
Go to Actions → Claude Continuous Refactoring → Run workflow
Select module: openrudder-core
Select type: performance
Click "Run workflow"
```

### Review Suggestions

1. **Check PRs**: Look for PRs titled "🤖 Claude AI: Refactoring Suggestions"
2. **Check Issues**: Look for issues with label `claude-ai`
3. **Review Comments**: Claude will comment on your PRs with suggestions

### Download Reports

```bash
# Download from GitHub Actions artifacts
Go to Actions → Select workflow run → Artifacts section
Download "refactoring-analysis" or "weekly-refactoring-report"
```

## 🔧 Customization

### Adjust Refactoring Focus

Edit `claude-refactor-workflow.yml`:

```yaml
# Line ~15: Add/modify refactor types
options:
  - code_quality
  - performance
  - security
  - architecture
  - documentation
  - your_custom_type
```

### Change Trigger Patterns

```yaml
# Only trigger on specific modules
paths:
  - 'openrudder-core/**/*.java'
  - 'openrudder-sources/**/*.java'
```

### Modify Claude's Instructions

Edit the prompt in the "Call Claude API" step to focus on specific aspects:

```yaml
# Line ~200: Customize the analysis prompt
"content": "Focus on reactive programming patterns and Spring Boot best practices..."
```

### Adjust Schedule

```yaml
# Run weekly report on different day/time
schedule:
  - cron: '0 14 * * 3'  # Wednesday at 2 PM UTC
```

## 📈 Monitoring

### Check Workflow Status

```bash
# View all workflow runs
https://github.com/scalefirstai/openrudder/actions

# Check specific workflow
https://github.com/scalefirstai/openrudder/actions/workflows/claude-refactor-workflow.yml
```

### Review Metrics

The workflows collect and display:
- Total Java files
- Lines of code
- Complex classes (>300 lines)
- Static analysis findings
- Refactoring suggestions count

### Artifacts

All analysis results are saved as artifacts for 30 days (continuous) or 90 days (weekly):
- `refactoring-analysis/` - Individual run analysis
- `weekly-refactoring-report/` - Comprehensive weekly reports

## 🛠️ Troubleshooting

### Workflow Not Running

**Issue**: Workflow doesn't trigger on push
- Check that paths match your file changes
- Verify GitHub Actions is enabled
- Check workflow permissions

### API Key Errors

**Issue**: "Authentication failed" or "Invalid API key"
- Verify `ANTHROPIC_API_KEY` secret is set correctly
- Check API key is valid at https://console.anthropic.com
- Ensure key has not expired

### No Suggestions Generated

**Issue**: Workflow runs but no PR/issue created
- Check if there were actual Java file changes
- Review workflow logs for API errors
- Verify Claude's response in artifacts

### Rate Limiting

**Issue**: "Rate limit exceeded"
- Anthropic API has rate limits
- Reduce frequency of workflow runs
- Consider upgrading API plan

## 🔐 Security Considerations

### API Key Protection
- ✅ Stored as GitHub Secret (encrypted)
- ✅ Never logged or exposed in workflow
- ✅ Only accessible during workflow execution

### Code Access
- ✅ Claude only sees code you explicitly send
- ✅ No persistent storage of code on Anthropic servers
- ✅ Analyze public repository code only

### Recommendations
- Regularly rotate API keys
- Review PRs before merging
- Don't auto-merge Claude's suggestions
- Use branch protection rules

## 📝 Example Output

### Pull Request Comment
```markdown
## 🤖 Claude AI Code Review

I've analyzed the changes in this PR and have some refactoring suggestions:

### HIGH Priority

**File**: `openrudder-core/src/main/java/io/openrudder/core/Source.java`
**Lines**: 45-60
**Category**: Performance
**Issue**: Blocking I/O in reactive context

Current code creates unnecessary blocking:
```java
public Mono<Void> stop() {
    this.running.set(false);
    executorService.shutdown(); // Blocking!
    return Mono.empty();
}
```

Suggested improvement:
```java
public Mono<Void> stop() {
    return Mono.fromRunnable(() -> {
        this.running.set(false);
    }).then(Mono.fromRunnable(() -> 
        executorService.shutdown()
    )).subscribeOn(Schedulers.boundedElastic());
}
```
```

### Issue Report
```markdown
📊 Claude Refactoring Report - 2025-02-02

## Summary
Analyzed 5 modules, identified 23 refactoring opportunities

## High Priority (5)
1. Performance: Blocking calls in reactive streams
2. Security: Unvalidated external input
3. Architecture: Tight coupling between modules

## Medium Priority (12)
...

## Recommendations
1. Introduce CompletableFuture for async operations
2. Add input validation layer
3. Use Spring events for module communication
```

## 🤝 Contributing

To improve the refactoring system:

1. Test changes locally first
2. Update documentation
3. Submit PR with clear description
4. Tag with `refactoring-automation` label

## 📚 Resources

- [Anthropic API Documentation](https://docs.anthropic.com)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [OpenRudder Contributing Guide](../CONTRIBUTING.md)
- [Claude Prompting Guide](https://docs.anthropic.com/claude/docs/guide-to-anthropics-prompt-engineering-resources)

## 🎓 Best Practices

### Do's ✅
- Review all suggestions before applying
- Use suggestions as learning opportunities
- Combine with manual code review
- Track patterns over time

### Don'ts ❌
- Don't auto-merge without review
- Don't ignore security suggestions
- Don't disable without team discussion
- Don't commit API keys to repository

## 📞 Support

Issues with the refactoring system?

1. Check workflow logs in Actions tab
2. Review artifacts for detailed output
3. Open issue with `refactoring-automation` label
4. Contact maintainers in Discord

---

**Built with ❤️ using Claude AI by Anthropic**
