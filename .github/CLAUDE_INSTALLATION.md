# 🎯 Quick Installation Instructions

## Prerequisites
- ✅ GitHub account with admin access to openrudder repo
- ✅ Anthropic API key from https://console.anthropic.com

## Step-by-Step Installation

### Step 1: Get Your API Key
1. Visit https://console.anthropic.com
2. Sign in or create account
3. Go to "API Keys"
4. Click "Create Key"
5. Copy the key (starts with `sk-ant-...`)

### Step 2: Add API Key to GitHub
1. Go to https://github.com/scalefirstai/openrudder/settings/secrets/actions
2. Click "New repository secret"
3. Name: `ANTHROPIC_API_KEY`
4. Value: Paste your API key
5. Click "Add secret"

### Step 3: Enable GitHub Actions
1. Go to https://github.com/scalefirstai/openrudder/settings/actions
2. Under "Actions permissions", select "Allow all actions"
3. Under "Workflow permissions", select "Read and write permissions"
4. Check "Allow GitHub Actions to create and approve pull requests"
5. Click "Save"

### Step 4: Install Workflow Files

#### Option A: Automated (Recommended)
```bash
cd /path/to/openrudder
./setup-refactoring.sh
```

#### Option B: Manual
```bash
cd /path/to/openrudder
mkdir -p .github/workflows

# Copy workflow files
cp claude-refactor-workflow.yml .github/workflows/
cp weekly-refactoring-workflow.yml .github/workflows/
cp claude-auto-refactor-advanced.yml .github/workflows/

# Copy documentation
cp REFACTORING_README.md .github/workflows/README.md
cp SETUP_GUIDE.md .github/CLAUDE_SETUP_GUIDE.md

# Commit and push
git add .github/
git commit -m "feat: add Claude AI continuous refactoring"
git push origin main
```

### Step 5: Verify Installation
1. Go to https://github.com/scalefirstai/openrudder/actions
2. You should see three new workflows:
   - Claude Continuous Refactoring
   - Weekly Comprehensive Refactoring  
   - Claude Auto-Refactor (Advanced)
3. Click "Run workflow" to test

## First Use

### Test the System
```bash
# Make a small change to test
cd openrudder
git checkout -b test-claude
echo "// Test comment" >> openrudder-core/src/main/java/io/openrudder/core/Source.java
git commit -am "test: verify Claude workflow"
git push origin test-claude

# Create PR and watch Claude comment!
```

### What to Expect

**Within 5 minutes:**
- ✅ Workflow runs automatically
- ✅ Claude analyzes your code
- ✅ Comments appear on your PR

**Every Monday at 9 AM UTC:**
- ✅ Comprehensive weekly analysis
- ✅ Issue created with full report

## Next Steps

1. **Read Documentation**
   - `.github/workflows/README.md` - Quick reference
   - `.github/CLAUDE_SETUP_GUIDE.md` - Full guide

2. **Customize Settings**
   - Adjust schedule in workflow files
   - Modify Claude's focus areas
   - Add custom refactoring types

3. **Monitor Results**
   - Check Actions tab regularly
   - Review Claude's suggestions
   - Track improvement over time

## Need Help?

- 📖 **Documentation**: See SETUP_GUIDE.md
- 🐛 **Issues**: https://github.com/scalefirstai/openrudder/issues
- 💬 **Discord**: https://discord.gg/openrudder
- 📧 **Email**: support@openrudder.io

## Estimated Time
- Installation: **10 minutes**
- First workflow run: **5 minutes**
- Total setup: **15 minutes**

**You're all set! 🎉**
