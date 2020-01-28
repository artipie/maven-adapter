# Spec

The current scope of the project does not include advanced Maven features -
like version range resolving, parent POMs, plugins and snapshots.


## Maven project

[Modular architecture](https://maven.apache.org/ref/3.6.3/)

### Modules

[Core](https://maven.apache.org/ref/3.6.3/maven-core/index.html)

[Model Builder](https://maven.apache.org/ref/3.6.3/maven-model-builder/index.html)

[Repository Metadata](https://maven.apache.org/ref/3.6.3/maven-repository-metadata/index.html)

and others

### Projects

[Artifact Resolver](https://maven.apache.org/resolver/index.html)

[Wagon](https://maven.apache.org/wagon/index.html)

and others

### Glossary

Read [Glossary](https://maven.apache.org/glossary.html)

**Artifact coordinates** - `org.group:artifact[:extension][:classifier]:version`
where classifier may be `sources` or `javadoc`

## Uploading artifacts

Scenario:
1. Identify the artifact by parsing artifact coordinates from URL path
2. Save the binary payload to `org/group/artifact/version/artifact-version[-classifier][.extension]`
3. Generate checksum files (files with extensions md5 and sha1)
4. Generate (and merge) metadata `maven-metadata-*.xml` which includes appending current version,
updating release (and latest) refs, lastUpdated properties

API `com.artipie.maven.Repository.upload(com.artipie.maven.UploadRequest)`

`UploadRequest` includes parsed artifact coordinates (`group:artifact:extension[:classifier]:version`)
and actual file.
