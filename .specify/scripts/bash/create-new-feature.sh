#!/bin/bash
#
# Create New Feature Scaffolding
#
# Usage: ./create-new-feature.sh <feature-id> <feature-name>
# Example: ./create-new-feature.sh 004 batch-processing
#
# This script creates a new feature directory with spec, plan, and tasks templates.

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SPECIFY_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
TEMPLATES_DIR="$SPECIFY_DIR/templates"
SPECS_DIR="$SPECIFY_DIR/specs"

# Validate arguments
if [ $# -lt 2 ]; then
    echo -e "${RED}Error: Missing arguments${NC}"
    echo "Usage: $0 <feature-id> <feature-name>"
    echo "Example: $0 004 batch-processing"
    exit 1
fi

FEATURE_ID="$1"
FEATURE_NAME="$2"
FEATURE_DIR="$SPECS_DIR/${FEATURE_ID}-${FEATURE_NAME}"

# Validate feature ID format (3 digits)
if ! [[ "$FEATURE_ID" =~ ^[0-9]{3}$ ]]; then
    echo -e "${RED}Error: Feature ID must be exactly 3 digits (e.g., 004)${NC}"
    exit 1
fi

# Validate feature name format (lowercase, hyphens)
if ! [[ "$FEATURE_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
    echo -e "${RED}Error: Feature name must be lowercase with hyphens (e.g., batch-processing)${NC}"
    exit 1
fi

# Check if directory already exists
if [ -d "$FEATURE_DIR" ]; then
    echo -e "${RED}Error: Feature directory already exists: $FEATURE_DIR${NC}"
    exit 1
fi

# Check if templates exist
if [ ! -f "$TEMPLATES_DIR/spec-template.md" ]; then
    echo -e "${RED}Error: Template not found: $TEMPLATES_DIR/spec-template.md${NC}"
    exit 1
fi

# Create feature directory
echo -e "${YELLOW}Creating feature directory: ${FEATURE_ID}-${FEATURE_NAME}${NC}"
mkdir -p "$FEATURE_DIR/contracts"

# Convert feature name to title case for display
FEATURE_TITLE=$(echo "$FEATURE_NAME" | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++)$i=toupper(substr($i,1,1))substr($i,2)}1')

# Get current date
CURRENT_DATE=$(date +%Y-%m-%d)

# Create spec.md from template
echo "Creating spec.md..."
sed -e "s/\[Feature Name\]/$FEATURE_TITLE/g" \
    -e "s/XXX-feature-name/${FEATURE_ID}-${FEATURE_NAME}/g" \
    -e "s/YYYY-MM-DD/$CURRENT_DATE/g" \
    "$TEMPLATES_DIR/spec-template.md" > "$FEATURE_DIR/spec.md"

# Create plan.md from template
echo "Creating plan.md..."
sed -e "s/\[Feature Name\]/$FEATURE_TITLE/g" \
    -e "s/XXX-feature-name/${FEATURE_ID}-${FEATURE_NAME}/g" \
    -e "s/YYYY-MM-DD/$CURRENT_DATE/g" \
    "$TEMPLATES_DIR/plan-template.md" > "$FEATURE_DIR/plan.md"

# Create tasks.md from template
echo "Creating tasks.md..."
sed -e "s/\[Feature Name\]/$FEATURE_TITLE/g" \
    -e "s/XXX-feature-name/${FEATURE_ID}-${FEATURE_NAME}/g" \
    -e "s/YYYY-MM-DD/$CURRENT_DATE/g" \
    "$TEMPLATES_DIR/tasks-template.md" > "$FEATURE_DIR/tasks.md"

# Create contracts/api-spec.md placeholder
echo "Creating contracts/api-spec.md placeholder..."
cat > "$FEATURE_DIR/contracts/api-spec.md" << EOF
# API Contracts: $FEATURE_TITLE

**Spec Reference**: ${FEATURE_ID}-${FEATURE_NAME}
**Version**: 1.0
**Last Updated**: $CURRENT_DATE

---

## 1. REST API Endpoints

<!-- Define REST endpoints here -->

---

## 2. WebSocket API

<!-- Define WebSocket messages here if applicable -->

---

## 3. Error Responses

<!-- Define error codes and responses -->

---

## 4. Data Models

<!-- Define request/response schemas -->
EOF

echo ""
echo -e "${GREEN}Feature scaffolding created successfully!${NC}"
echo ""
echo "Created files:"
echo "  $FEATURE_DIR/spec.md"
echo "  $FEATURE_DIR/plan.md"
echo "  $FEATURE_DIR/tasks.md"
echo "  $FEATURE_DIR/contracts/api-spec.md"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "  1. Edit spec.md to define requirements and user stories"
echo "  2. Review constitution.md and complete the Constitution Check"
echo "  3. Edit plan.md to define implementation phases"
echo "  4. Edit contracts/api-spec.md if feature has API changes"
echo "  5. Break down tasks in tasks.md when ready to implement"
echo ""
echo -e "${GREEN}Remember to follow the constitution principles in .specify/memory/constitution.md${NC}"
