#!/bin/bash

# OpenRudder Claude AI Refactoring Setup Script
# This script sets up continuous refactoring workflows for your repository

set -e

echo "🤖 OpenRudder Claude AI Refactoring Setup"
echo "=========================================="
echo ""

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository"
    exit 1
fi

# Check if this is the openrudder repo
if ! git remote get-url origin | grep -q "openrudder"; then
    echo "⚠️  Warning: This doesn't appear to be the openrudder repository"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "📁 Creating .github/workflows directory..."
mkdir -p .github/workflows

echo ""
echo "📝 Which workflows would you like to install?"
echo "  1. Continuous Refactoring (recommended)"
echo "  2. Weekly Comprehensive Analysis"
echo "  3. Advanced Auto-Refactoring"
echo "  4. All of the above"
echo ""
read -p "Enter choice (1-4): " choice

case $choice in
    1)
        workflows=("claude-refactor-workflow.yml")
        ;;
    2)
        workflows=("weekly-refactoring-workflow.yml")
        ;;
    3)
        workflows=("claude-auto-refactor-advanced.yml")
        ;;
    4)
        workflows=("claude-refactor-workflow.yml" "weekly-refactoring-workflow.yml" "claude-auto-refactor-advanced.yml")
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

# Download workflow files
echo ""
echo "📥 Downloading workflow files..."

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for workflow in "${workflows[@]}"; do
    if [ -f "$SCRIPT_DIR/$workflow" ]; then
        cp "$SCRIPT_DIR/$workflow" .github/workflows/
        echo "  ✓ Installed $workflow"
    else
        echo "  ⚠️  Warning: $workflow not found in current directory"
    fi
done

# Create README
echo ""
echo "📄 Creating documentation..."
if [ -f "$SCRIPT_DIR/REFACTORING_README.md" ]; then
    cp "$SCRIPT_DIR/REFACTORING_README.md" .github/workflows/README.md
    echo "  ✓ Created .github/workflows/README.md"
fi

# Check for API key
echo ""
echo "🔑 Checking for Anthropic API key..."
echo ""
echo "You need to add your Anthropic API key to GitHub Secrets:"
echo "  1. Go to: https://github.com/$(git remote get-url origin | sed 's/.*://;s/.git$//')/settings/secrets/actions"
echo "  2. Click 'New repository secret'"
echo "  3. Name: ANTHROPIC_API_KEY"
echo "  4. Value: Your API key from https://console.anthropic.com"
echo ""
read -p "Have you already added the ANTHROPIC_API_KEY secret? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "⚠️  Please add the API key before the workflows will work"
    echo "   The workflows are installed but won't run without it"
fi

# Configure git
echo ""
echo "📋 Committing changes..."
git add .github/workflows/

if [ -n "$(git status --porcelain .github/workflows/)" ]; then
    git commit -m "feat: add Claude AI continuous refactoring workflows

- Automated code quality analysis
- Weekly comprehensive reviews
- Integration with pull requests

Workflow features:
- Static analysis integration
- AI-powered refactoring suggestions
- Automatic PR creation for safe changes
- Comprehensive reporting"
    
    echo "  ✓ Changes committed"
    echo ""
    echo "📤 Push changes to GitHub:"
    echo "  git push origin main"
else
    echo "  ℹ️  No changes to commit (workflows already exist)"
fi

echo ""
echo "✅ Setup complete!"
echo ""
echo "📚 Next steps:"
echo "  1. Push changes to GitHub: git push origin main"
echo "  2. Add ANTHROPIC_API_KEY secret (if not done)"
echo "  3. Go to Actions tab to verify workflows are enabled"
echo "  4. Read .github/workflows/README.md for usage instructions"
echo ""
echo "🎉 Your repository now has continuous refactoring assistance!"
echo ""
echo "📖 Documentation: .github/workflows/README.md"
echo "🔧 Workflows: .github/workflows/"
echo "🐛 Issues: https://github.com/scalefirstai/openrudder/issues"
echo ""
