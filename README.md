# Dependency DL

Downloads dependencies with its transitive ones

## How to use

Create dependencies.txt with following content:

example:

```
org.jfrog.buildinfo:build-info-extractor-gradle:4.20.0
junit:junit:4.13.0

```

you should enumerate each dependency one by one

Also you need to set up initial repositories in repositories.txt like this:

```
central https://repo.maven.apache.org/maven2/
gradle-plugins https://plugins.gradle.org/m2/
```

You should place these files in the same folder where jar placed (or just run on already precreated files (its were
committed) from IDE)