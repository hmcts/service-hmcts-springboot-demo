# PMD needs custom rules defining in build.gradle

Since this is a build quality / pipeline it is best at the end of the build.gradle file
```
pmd {
  ruleSets = []
  ruleSetFiles = files(".github/pmd-ruleset.xml")
  ignoreFailures = false
}
```
