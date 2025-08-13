#!/bin/bash

set -eu

# Configuration
PACKAGE_NAME="kapper-benchmarks"
BENCHMARK_DIR="benchmark/build/distributions"

# Default values
VERBOSE=true

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}INFO:${NC} $1"
    fi
}

print_success() {
    echo -e "${GREEN}SUCCESS:${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}WARNING:${NC} $1"
}

print_error() {
    echo -e "${RED}ERROR:${NC} $1" >&2
}

# Function to show usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Package Kapper benchmark zip for distribution

OPTIONS:
    --verbose       Enable verbose output (default: true)
    --quiet         Disable verbose output
    -h, --help      Show this help message

EXAMPLES:
    $0                          # Package benchmarks
    $0 --quiet                 # Package with minimal output
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose)
            VERBOSE=true
            shift
            ;;
        --quiet)
            VERBOSE=false
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done


# Check if benchmark zip exists, build if missing
ZIP_PATTERN="$BENCHMARK_DIR/$PACKAGE_NAME-*.zip"
if ! ls $ZIP_PATTERN 1> /dev/null 2>&1; then
    print_info "Benchmark zip not found. Building it now..."
    ./gradlew :kapper-benchmark:benchmarkZip
    
    if ! ls $ZIP_PATTERN 1> /dev/null 2>&1; then
        print_error "Failed to build benchmark zip"
        exit 1
    fi
fi

# Find the most recent zip file
ZIP_FILE=$(ls -t $ZIP_PATTERN | head -n 1)
ZIP_BASENAME=$(basename "$ZIP_FILE")

print_info "Found benchmark zip: $ZIP_BASENAME"

# Extract version from filename
if [[ "$ZIP_BASENAME" =~ $PACKAGE_NAME-(.+)\.zip ]]; then
    VERSION="${BASH_REMATCH[1]}"
    print_info "Detected version: $VERSION"
else
    print_error "Could not extract version from filename: $ZIP_BASENAME"
    exit 1
fi

print_success "Benchmark package created successfully: $ZIP_FILE"
print_info "Package details:"
print_info "  File: $ZIP_BASENAME"
print_info "  Version: $VERSION"
print_info "  Size: $(du -h "$ZIP_FILE" | cut -f1)"