#!/bin/bash

set -eu

# Default values
benchmark_mode="avgt"
forks=3
iterations=5
time_on_iteration="5s"
warmup_time="2s"
databases="SQLITE,POSTGRESQL"
libraries="JDBC,KAPPER,KAPPER-NO-AUTOMAP,HIBERNATE,KTORM"
rows=1000

# Parse options
while getopts "b:f:i:r:w:d:l:j:h" opt; do
  case $opt in
    b) benchmark_mode="$OPTARG"
    ;;
    f) forks="$OPTARG"
    ;;
    i) iterations="$OPTARG"
    ;;
    r) time_on_iteration="$OPTARG"
    ;;
    w) warmup_time="$OPTARG"
    ;;
    d) databases="$OPTARG"
    ;;
    l) libraries="$OPTARG"
    ;;
    j) rows="$OPTARG"
    ;;
    h) cat benchmark/run-usage.txt
           exit 0
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
        exit 1
    ;;
  esac
done

script_dir=$(dirname "$(realpath "$0")")

# Create JMH jar
cd "$script_dir/.." || exit
./gradlew jmhJar

cd "$script_dir/build/libs" || exit

# Find the latest version of the JAR file
latest_jar=$(ls kapper-benchmark-*-jmh.jar | sort -V | tail -n 1)

# Print system details
echo "------------------------------------------------------------------------------------------"
echo "JMH benchmark in: $latest_jar (SHA-256: $(shasum -a 256 "$latest_jar" | awk '{print $1}') )"
echo "Kapper source: $(git rev-parse --short HEAD)"
echo "Benchmark created at: $(date)"
java -version
echo "------------------------------------------------------------------------------------------"
if command -v lscpu &> /dev/null; then
    echo "CPU: $(lscpu)"
else
    echo "CPU: $(sysctl -n machdep.cpu.brand_string) $(sysctl -n hw.physicalcpu) $(sysctl -n hw.logicalcpu)"
fi

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    grep MemTotal /proc/meminfo | awk '{print "RAM: " $2 " kB"}'
elif [[ "$OSTYPE" == "darwin"* ]]; then
    sysctl -n hw.memsize | awk '{print "RAM: " $1 / 1024 / 1024 " MB"}'
fi
echo "------------------------------------------------------------------------------------------"

set -x
java -jar "$latest_jar" -bm "$benchmark_mode" -f "$forks" -i "$iterations" \
  -r "$time_on_iteration" -w "$warmup_time" \
  -p databaseType="$databases" -p library="$libraries" -p rows="$rows" \
  -opi 1 -t 1 \
  -foe true -gc true -rf json -rff "results-${benchmark_mode}.json"
set +x
