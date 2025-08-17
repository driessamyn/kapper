#!/bin/bash

# Create a release commit first:
# git commit --allow-empty -m "build: release <version>"
# Then tag
# git tag <version>
# remember to push the tag
# git push origin <version>

tag=$(git describe --tags --abbrev=0)
version=$(./gradlew -PnoDirtyCheck=true -q printVersion)

# Create the release
./gradlew -PnoDirtyCheck=true -q printChangeLog | gh release create $tag -d -t "Kapper $version" -F -

# Package and upload benchmarks
echo "Packaging benchmarks..."
./package-benchmarks.sh

# Upload benchmark zip to the release
benchmark_zip="benchmark/build/distributions/kapper-benchmarks-${version}.zip"
if [[ -f "$benchmark_zip" ]]; then
    echo "Uploading benchmark package to release..."
    gh release upload "$tag" "$benchmark_zip" --clobber
    echo "Benchmark package uploaded successfully"
else
    echo "Warning: Benchmark zip not found at $benchmark_zip"
fi