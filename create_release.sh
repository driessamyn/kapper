#!/bin/bash

# Create a release commit first:
# git commit --allow-empty -m "build: release <version>"
# Then tag
# git tag <version>
# remember to push the tag
# git push origin <version>

tag=$(git describe --tags --abbrev=0)
version=$(./gradlew -q printVersion)
./gradlew -q printChangeLog | gh release create $tag -d -t "Kapper $version" -F -