#!/bin/bash

# Run JUnit tests and display summary
# Usage: ./run-tests.sh

set -e

echo "========================================"
echo "Running Weld Instrumentation Unit Tests"
echo "========================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Run tests for weld-core-3.0
echo "Running tests for weld-core-3.0..."
./gradlew :weld-core-3.0:test --tests "com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest" --console=plain > /tmp/weld-test-3.0.log 2>&1
RESULT_3_0=$?

# Run tests for weld-core-4.0
echo "Running tests for weld-core-4.0..."
./gradlew :weld-core-4.0:test --tests "com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest" --console=plain > /tmp/weld-test-4.0.log 2>&1
RESULT_4_0=$?

echo ""
echo "========================================"
echo "Test Results Summary"
echo "========================================"
echo ""

# Parse weld-core-3.0 results
if [ -f "weld-core-3.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml" ]; then
    TESTS_3_0=$(grep -o 'tests="[0-9]*"' weld-core-3.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    FAILURES_3_0=$(grep -o 'failures="[0-9]*"' weld-core-3.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    ERRORS_3_0=$(grep -o 'errors="[0-9]*"' weld-core-3.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    TIME_3_0=$(grep -o 'time="[0-9.]*"' weld-core-3.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)

    echo "weld-core-3.0:"
    echo "  Tests:    $TESTS_3_0"
    echo "  Failures: $FAILURES_3_0"
    echo "  Errors:   $ERRORS_3_0"
    echo "  Time:     ${TIME_3_0}s"

    if [ "$FAILURES_3_0" -eq 0 ] && [ "$ERRORS_3_0" -eq 0 ]; then
        echo -e "  Status:   ${GREEN}✓ PASSED${NC}"
    else
        echo -e "  Status:   ${RED}✗ FAILED${NC}"
    fi
else
    echo -e "weld-core-3.0: ${RED}✗ NO RESULTS FOUND${NC}"
    RESULT_3_0=1
fi

echo ""

# Parse weld-core-4.0 results
if [ -f "weld-core-4.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml" ]; then
    TESTS_4_0=$(grep -o 'tests="[0-9]*"' weld-core-4.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    FAILURES_4_0=$(grep -o 'failures="[0-9]*"' weld-core-4.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    ERRORS_4_0=$(grep -o 'errors="[0-9]*"' weld-core-4.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)
    TIME_4_0=$(grep -o 'time="[0-9.]*"' weld-core-4.0/build/test-results/test/TEST-com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfigTest.xml | head -1 | cut -d'"' -f2)

    echo "weld-core-4.0:"
    echo "  Tests:    $TESTS_4_0"
    echo "  Failures: $FAILURES_4_0"
    echo "  Errors:   $ERRORS_4_0"
    echo "  Time:     ${TIME_4_0}s"

    if [ "$FAILURES_4_0" -eq 0 ] && [ "$ERRORS_4_0" -eq 0 ]; then
        echo -e "  Status:   ${GREEN}✓ PASSED${NC}"
    else
        echo -e "  Status:   ${RED}✗ FAILED${NC}"
    fi
else
    echo -e "weld-core-4.0: ${RED}✗ NO RESULTS FOUND${NC}"
    RESULT_4_0=1
fi

echo ""
echo "========================================"

# Overall result
if [ $RESULT_3_0 -eq 0 ] && [ $RESULT_4_0 -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    echo ""
    echo "Test details:"
    echo "  weld-core-3.0: $TESTS_3_0 tests, 0 failures"
    echo "  weld-core-4.0: $TESTS_4_0 tests, 0 failures"
    echo ""
    echo "View detailed reports:"
    echo "  weld-core-3.0: file://$(pwd)/weld-core-3.0/build/reports/tests/test/index.html"
    echo "  weld-core-4.0: file://$(pwd)/weld-core-4.0/build/reports/tests/test/index.html"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    echo ""
    echo "View logs:"
    echo "  weld-core-3.0: /tmp/weld-test-3.0.log"
    echo "  weld-core-4.0: /tmp/weld-test-4.0.log"
    exit 1
fi
