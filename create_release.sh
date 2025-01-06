#!/bin/bash

# tag first

tag=$(git describe --tags --abbrev=0)
version=$(./gradlew -q printVersion)
./gradlew -q printChangeLog | gh release create $tag -d -t "Kapper $version" -F -