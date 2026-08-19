#!/bin/bash

# Run all JUnit tests and display a per-class summary
# Usage: ./run-tests.sh

set -e

echo "========================================"
echo "Running Weld Instrumentation Unit Tests"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Test classes to run for each module (WeldFilteringIntegrationTest excluded — known Weld SE classloader issue)
TEST_CLASSES=(
    "com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest"
    "com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfigTest"
    "com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtilsTest"
)
TEST_CLASSES_4=(
    "com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest"
    "com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfigTest"
    "com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtilsTest"
)

summarise_module() {
    local MODULE="$1"
    local RESULT_DIR="${MODULE}/build/test-results/test"
    local TOTAL=0 FAILURES=0 ERRORS=0

    echo "${MODULE}:"
    if [ ! -d "${RESULT_DIR}" ]; then
        echo -e "  ${RED}No test results found${NC}"
        return 1
    fi

    for xml in "${RESULT_DIR}"/*.xml; do
        [ -f "$xml" ] || continue
        cls=$(grep -o 'name="[^"]*"' "$xml" | head -1 | cut -d'"' -f2)
        t=$(grep -o 'tests="[0-9]*"' "$xml" | head -1 | cut -d'"' -f2)
        f=$(grep -o 'failures="[0-9]*"' "$xml" | head -1 | cut -d'"' -f2)
        e=$(grep -o 'errors="[0-9]*"' "$xml" | head -1 | cut -d'"' -f2)
        TOTAL=$((TOTAL + ${t:-0}))
        FAILURES=$((FAILURES + ${f:-0}))
        ERRORS=$((ERRORS + ${e:-0}))
        if [ "${f:-0}" -eq 0 ] && [ "${e:-0}" -eq 0 ]; then
            echo -e "  ${GREEN}✓ PASS${NC}  ${cls} (${t:-0} tests)"
        else
            echo -e "  ${RED}✗ FAIL${NC}  ${cls} (${t:-0} tests, ${f:-0} failures, ${e:-0} errors)"
        fi
    done

    echo "  ─────────────────────────────"
    if [ "$FAILURES" -eq 0 ] && [ "$ERRORS" -eq 0 ]; then
        echo -e "  ${GREEN}Total: ${TOTAL} tests, all passed${NC}"
    else
        echo -e "  ${RED}Total: ${TOTAL} tests, ${FAILURES} failures, ${ERRORS} errors${NC}"
    fi
    echo ""

    [ "$FAILURES" -eq 0 ] && [ "$ERRORS" -eq 0 ]
}

# Build test classes args for each module
TESTS_3=""
for c in "${TEST_CLASSES[@]}"; do TESTS_3="${TESTS_3} --tests \"${c}\""; done
TESTS_4=""
for c in "${TEST_CLASSES_4[@]}"; do TESTS_4="${TESTS_4} --tests \"${c}\""; done

echo "Running tests for weld-core-3.0..."
eval "./gradlew :weld-core-3.0:cleanTest :weld-core-3.0:test ${TESTS_3} --console=plain" > /tmp/weld-test-3.0.log 2>&1
R3=$?

echo "Running tests for weld-core-4.0..."
eval "./gradlew :weld-core-4.0:cleanTest :weld-core-4.0:test ${TESTS_4} --console=plain" > /tmp/weld-test-4.0.log 2>&1
R4=$?

echo ""
echo "========================================"
echo "Test Results Summary"
echo "========================================"
echo ""

summarise_module "weld-core-3.0" && S3=0 || S3=1
summarise_module "weld-core-4.0" && S4=0 || S4=1

echo "========================================"
echo ""
if [ $S3 -eq 0 ] && [ $S4 -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    echo ""
    echo "HTML reports:"
    echo "  file://$(pwd)/weld-core-3.0/build/reports/tests/test/index.html"
    echo "  file://$(pwd)/weld-core-4.0/build/reports/tests/test/index.html"
    exit 0
else
    echo -e "${RED}Some tests failed! See logs:${NC}"
    echo "  /tmp/weld-test-3.0.log"
    echo "  /tmp/weld-test-4.0.log"
    exit 1
fi
