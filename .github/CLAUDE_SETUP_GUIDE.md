# 🚀 Complete Setup Guide: Claude AI Continuous Refactoring for OpenRudder

## 📖 Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Detailed Setup](#detailed-setup)
4. [How It Works](#how-it-works)
5. [Usage Examples](#usage-examples)
6. [Customization](#customization)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Overview

This system provides **automated continuous refactoring assistance** for the OpenRudder project using Claude AI. It analyzes your Java code on every push, pull request, and weekly schedule, providing specific, actionable refactoring suggestions.

### What You Get

✅ **Automatic Code Review**: Claude analyzes every PR  
✅ **Weekly Reports**: Comprehensive architectural analysis  
✅ **Auto-Refactoring**: Safe, automated code improvements  
✅ **Integration**: Works with your existing CI/CD  
✅ **Transparency**: All suggestions in PRs and issues  

---

## Quick Start

### Prerequisites

- ✅ Admin access to github.com/scalefirstai/openrudder
- ✅ Anthropic API key ([Get one here](https://console.anthropic.com))
- ✅ Git and GitHub CLI installed

### 5-Minute Setup

```bash
# 1. Clone the repo (if not already)
git clone https://github.com/scalefirstai/openrudder.git
cd openrudder

# 2. Download setup files
# (Copy the workflow files you created to this directory)

# 3. Run setup script
chmod +x setup-refactoring.sh
./setup-refactoring.sh

# 4. Follow prompts and push to GitHub
git push origin main
```

### Add API Key to GitHub

1. Go to: https://github.com/scalefirstai/openrudder/settings/secrets/actions
2. Click **"New repository secret"**
3. Name: `ANTHROPIC_API_KEY`
4. Value: Your Anthropic API key
5. Click **"Add secret"**

**✅ Done!** Your workflows are now active.

---

## Detailed Setup

### Step 1: Get Anthropic API Key

1. Visit https://console.anthropic.com
2. Sign up or log in
3. Navigate to **API Keys**
4. Click **"Create Key"**
5. Copy your key (starts with `sk-ant-...`)

**Note**: You'll need a paid plan for production use. The free tier has limits.

### Step 2: Install Workflow Files

#### Option A: Manual Installation

```bash
cd openrudder
mkdir -p .github/workflows

# Copy workflow files
cp /path/to/claude-refactor-workflow.yml .github/workflows/
cp /path/to/weekly-refactoring-workflow.yml .github/workflows/
cp /path/to/claude-auto-refactor-advanced.yml .github/workflows/

# Copy documentation
cp /path/to/REFACTORING_README.md .github/workflows/README.md
```

#### Option B: Using Setup Script

```bash
./setup-refactoring.sh
# Follow the interactive prompts
```

### Step 3: Configure GitHub Repository

#### Enable Actions

1. Go to **Settings** → **Actions** → **General**
2. Under "Actions permissions":
   - Select ✅ **"Allow all actions and reusable workflows"**
3. Under "Workflow permissions":
   - Select ✅ **"Read and write permissions"**
   - Check ✅ **"Allow GitHub Actions to create and approve pull requests"**
4. Click **"Save"**

#### Add API Key Secret

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"**
3. Add:
   - **Name**: `ANTHROPIC_API_KEY`
   - **Secret**: Your Anthropic API key
4. Click **"Add secret"**

### Step 4: Commit and Push

```bash
git add .github/workflows/
git commit -m "feat: add Claude AI continuous refactoring workflows"
git push origin main
```

### Step 5: Verify Installation

1. Go to the **Actions** tab on GitHub
2. You should see three new workflows:
   - ✅ Claude Continuous Refactoring
   - ✅ Weekly Comprehensive Refactoring
   - ✅ Claude Auto-Refactor (Advanced)
3. Click on any workflow to see its status

---

## How It Works

### Workflow 1: Continuous Refactoring

**Triggers**: Push to `main`, Pull Requests

**Process**:
```
1. Code Change Detected
   ↓
2. Build & Static Analysis (PMD, SpotBugs, Checkstyle)
   ↓
3. Extract Changed Files
   ↓
4. Send to Claude AI for Analysis
   ↓
5. Claude Returns Specific Suggestions
   ↓
6. Create Branch + PR (if significant)
   OR
   Comment on Existing PR
```

**Output**:
- Pull Request with refactoring suggestions
- Issue with detailed analysis
- Comments on your PRs

### Workflow 2: Weekly Comprehensive Analysis

**Triggers**: Every Monday at 9 AM UTC

**Process**:
```
1. Analyze Each Module Separately
   ↓
2. Comprehensive Architectural Review
   ↓
3. Check Dependency Updates
   ↓
4. Generate Report
   ↓
5. Create Tracking Issue
```

**Output**:
- Weekly issue with full report
- Downloadable artifacts (90 days)

### Workflow 3: Advanced Auto-Refactoring

**Triggers**: Manual dispatch only

**Process**:
```
1. Analyze Code with JavaParser
   ↓
2. Claude Identifies Safe Refactorings
   ↓
3. Apply Changes Automatically
   ↓
4. Run Tests to Verify
   ↓
5. Create PR if Tests Pass
```

**Output**:
- PR with automatically applied refactorings
- All tests passing

---

## Usage Examples

### Example 1: Get Feedback on Your PR

```bash
# Create your feature branch
git checkout -b feature/new-source

# Make changes
vim openrudder-sources/src/main/java/io/openrudder/sources/MyNewSource.java

# Commit and push
git add .
git commit -m "feat: add new source implementation"
git push origin feature/new-source

# Create PR on GitHub
# Claude will automatically comment with suggestions!
```

**Claude's comment might include**:
```markdown
## 🤖 Claude AI Code Review

### HIGH Priority

**File**: `openrudder-sources/.../MyNewSource.java`
**Line**: 45
**Issue**: Potential resource leak

The `InputStream` is not closed properly:
```java
// Current
InputStream is = connection.getInputStream();
return parseData(is);

// Suggested
try (InputStream is = connection.getInputStream()) {
    return parseData(is);
}
```
```

### Example 2: Run Weekly Analysis

The weekly workflow runs automatically, but you can trigger it manually:

1. Go to **Actions** → **Weekly Comprehensive Refactoring**
2. Click **"Run workflow"**
3. Click **"Run workflow"** button
4. Wait 5-10 minutes
5. Check **Issues** tab for the report

### Example 3: Auto-Refactor Specific Files

```bash
# Via GitHub UI:
# 1. Go to Actions → Claude Auto-Refactor (Advanced)
# 2. Click "Run workflow"
# 3. Enter files: "openrudder-core/src/main/java/io/openrudder/core/Source.java"
# 4. Set auto_apply: true
# 5. Click "Run workflow"

# Wait for PR to be created with automated fixes
```

### Example 4: Focus on Performance

```bash
# Via GitHub UI:
# 1. Go to Actions → Claude Continuous Refactoring
# 2. Click "Run workflow"
# 3. Select module: "openrudder-query-engine"
# 4. Select type: "performance"
# 5. Click "Run workflow"
```

---

## Customization

### Adjust Analysis Frequency

Edit `.github/workflows/weekly-refactoring-workflow.yml`:

```yaml
on:
  schedule:
    - cron: '0 9 * * 1'  # Monday at 9 AM UTC
    # Change to: '0 14 * * 3' for Wednesday at 2 PM UTC
```

### Focus on Specific Modules

Edit `.github/workflows/claude-refactor-workflow.yml`:

```yaml
paths:
  - 'openrudder-core/**/*.java'  # Only core module
  - 'openrudder-sources/**/*.java'  # Only sources
```

### Change Claude's Focus

Modify the prompt in the "Call Claude API" step:

```yaml
"content": "Analyze this code focusing ONLY on:\n1. Security vulnerabilities\n2. Performance bottlenecks\n\n..."
```

### Add Custom Refactoring Types

Edit the workflow inputs:

```yaml
refactor_type:
  type: choice
  options:
    - code_quality
    - performance
    - security
    - my_custom_type  # Add your type
```

---

## Best Practices

### Do's ✅

1. **Review Before Merging**
   - Always review Claude's suggestions
   - Don't auto-merge without human review
   - Test changes locally if possible

2. **Use as Learning Tool**
   - Read Claude's explanations
   - Understand *why* changes are suggested
   - Share patterns with team

3. **Combine with Manual Review**
   - Claude complements, doesn't replace humans
   - Use for catching issues humans miss
   - Verify suggestions make sense for your context

4. **Track Patterns**
   - Note recurring issues
   - Update coding standards
   - Share insights in team meetings

5. **Customize for Your Needs**
   - Adjust prompts for your priorities
   - Focus on your pain points
   - Iterate on what works

### Don'ts ❌

1. **Don't Auto-Merge**
   - Never set up auto-merge for Claude PRs
   - Always have human approval
   - Verify tests pass

2. **Don't Ignore Security Issues**
   - Take security suggestions seriously
   - Investigate and fix promptly
   - Don't dismiss as false positives without verification

3. **Don't Disable Without Discussion**
   - Talk to team before turning off
   - Understand why it's not working
   - Try adjusting first

4. **Don't Commit API Keys**
   - Always use GitHub Secrets
   - Never commit keys to repo
   - Rotate keys regularly

5. **Don't Treat as Source of Truth**
   - Claude can be wrong
   - Use judgment
   - Consult documentation when unsure

---

## Troubleshooting

### Issue: Workflow Not Triggering

**Symptoms**: Push to main, but no workflow run

**Solutions**:
```bash
# 1. Check workflow file is in correct location
ls -la .github/workflows/

# 2. Verify file is valid YAML
cat .github/workflows/claude-refactor-workflow.yml | yamllint -

# 3. Check GitHub Actions is enabled
# Go to Settings → Actions → General

# 4. Verify paths match your changes
git log --name-only -1
# Should include Java files in specified paths
```

### Issue: API Authentication Failed

**Symptoms**: "Invalid API key" or "Authentication failed"

**Solutions**:
```bash
# 1. Verify secret exists
# Go to Settings → Secrets → Actions
# Confirm ANTHROPIC_API_KEY is listed

# 2. Check key validity
curl https://api.anthropic.com/v1/messages \
  -H "x-api-key: YOUR_KEY_HERE" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 10,
    "messages": [{"role": "user", "content": "Hi"}]
  }'

# 3. Regenerate key if needed
# Visit https://console.anthropic.com/settings/keys
```

### Issue: No Suggestions Generated

**Symptoms**: Workflow runs but no PR/issue created

**Solutions**:
```bash
# 1. Check workflow logs
# Go to Actions → Select run → View logs

# 2. Download artifacts
# Actions → Select run → Artifacts section
# Download and review claude_suggestions.txt

# 3. Verify files were changed
git diff HEAD~1 HEAD --name-only | grep "\.java$"

# 4. Check if changes were too minor
# Claude may not have suggestions for trivial changes
```

### Issue: Tests Fail After Auto-Refactoring

**Symptoms**: Auto-refactor applied changes but tests fail

**Solutions**:
```bash
# The workflow should automatically revert, but if not:

# 1. Checkout the PR branch
git fetch origin
git checkout claude-auto-refactor-XXXXX

# 2. Review changes
git diff main

# 3. Identify failing tests
mvn test

# 4. Either fix or close PR
# Option A: Fix tests and push
# Option B: Close PR and report issue
```

### Issue: Rate Limiting

**Symptoms**: "Rate limit exceeded" errors

**Solutions**:
```yaml
# 1. Reduce workflow frequency
on:
  push:
    branches:
      - main
  # Remove pull_request trigger temporarily

# 2. Upgrade Anthropic plan
# Visit https://console.anthropic.com/settings/plans

# 3. Implement request batching
# Modify workflow to combine multiple files per request
```

### Issue: Large File Analysis Timeout

**Symptoms**: Workflow times out on large files

**Solutions**:
```yaml
# 1. Limit files analyzed
# In workflow, change:
head -20 /tmp/changed_files.txt
# to:
head -10 /tmp/changed_files.txt

# 2. Increase timeout
jobs:
  analyze-and-refactor:
    timeout-minutes: 60  # Increase from default

# 3. Split into multiple runs
# Analyze modules separately
```

### Getting Help

If you can't resolve an issue:

1. **Check Logs**
   - Actions tab → Failed run → View logs
   - Download artifacts for detailed output

2. **Review Documentation**
   - `.github/workflows/README.md`
   - This guide

3. **Create Issue**
   ```bash
   # Include:
   # - Workflow run link
   # - Error messages
   # - What you've tried
   # - Expected vs actual behavior
   ```

4. **Community Support**
   - OpenRudder Discord
   - GitHub Discussions
   - Email: support@openrudder.io

---

## Advanced Configuration

### Custom Claude Instructions

Create a file `.github/claude-instructions.md`:

```markdown
# Custom Refactoring Instructions for OpenRudder

## Priority Areas
1. Reactive programming correctness
2. Thread-safety in concurrent scenarios
3. Resource management (proper cleanup)

## Coding Standards
- Max method length: 50 lines
- Max class length: 300 lines
- Always use try-with-resources

## Ignore
- Generated code in target/
- Third-party code in lib/
```

Reference in workflow:

```yaml
- name: Call Claude API
  run: |
    INSTRUCTIONS=$(cat .github/claude-instructions.md)
    # Include in API call
```

### Integration with Other Tools

#### SonarQube Integration

```yaml
- name: Run SonarQube
  run: mvn sonar:sonar

- name: Get SonarQube Results
  run: |
    # Fetch issues from SonarQube
    # Include in Claude's context
```

#### Slack Notifications

```yaml
- name: Notify Slack
  if: steps.claude.outputs.has_suggestions == 'true'
  uses: slackapi/slack-github-action@v1
  with:
    payload: |
      {
        "text": "Claude found refactoring opportunities!"
      }
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Cost Estimation

### Typical Costs (Anthropic API)

**Small commit** (1-3 files):
- Input: ~2,000 tokens
- Output: ~1,000 tokens
- Cost: ~$0.05

**Medium PR** (10-20 files):
- Input: ~10,000 tokens
- Output: ~5,000 tokens
- Cost: ~$0.25

**Weekly comprehensive** (all modules):
- Input: ~50,000 tokens
- Output: ~15,000 tokens
- Cost: ~$1.25

**Monthly estimate**: $30-50 for active repo

### Cost Optimization

1. **Limit file count**: Analyze only changed files
2. **Use batching**: Combine multiple files per request
3. **Selective triggers**: Don't run on docs-only changes
4. **Cache results**: Store and reuse analysis

---

## Success Metrics

Track these to measure impact:

### Code Quality
- Decrease in PMD/SpotBugs findings
- Improved code coverage
- Reduced cyclomatic complexity

### Team Productivity
- Faster PR reviews
- Fewer bugs in production
- Time saved on manual reviews

### Learning
- Team members learning from suggestions
- Adoption of new patterns
- Improved coding standards

---

## Conclusion

You now have a comprehensive understanding of the Claude AI Continuous Refactoring system for OpenRudder. The system will help maintain code quality, catch issues early, and continuously improve your codebase.

### Quick Reference

- **Setup**: Run `./setup-refactoring.sh`
- **API Key**: Settings → Secrets → Actions
- **Workflows**: .github/workflows/
- **Docs**: .github/workflows/README.md
- **Support**: GitHub Issues or Discord

Happy refactoring! 🚀

---

**Built with ❤️ by the OpenRudder Community**
