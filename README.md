# VersionBridge

This standalone Gradle project builds a shared NMS API and Paper/Spigot
adapters for Minecraft 1.17 through 26.3. It is a build dependency, not a
server plugin. Multiple consuming plugins can use the same adapter release.

Eclipse imports the API and each version adapter as a project, such as
`VersionBridge-paper-R26_3`. The `paper/` and `spigot/` directories only group
adapter source on disk; they are not Eclipse projects themselves.

Run `./gradlew build` to produce:

- `api/build/libs/api-1.0.0.jar` for build-time API use.
- `build/dist/adapters/*.jar` for runtime download assets.
- `build/dist/runtime-adapters.sha256` for consumers to embed and verify.

Upload every jar from `build/dist/adapters/` to a GitHub release tagged with
the hub version in `gradle.properties`, currently `v1.0.0`. Preserve asset
filenames. Release the hub before distributing a consuming plugin jar to new
servers. A consumer can also use another HTTPS host and point its bootstrap
loader there; the hashes in its embedded manifest still verify each adapter.

To use this project in another Gradle build, add `includeBuild('../VersionBridge')`
in its settings, depend on `com.swiftlicious.versionbridge:api:1.0.0`, and copy
the adapter manifest into that plugin jar. The consumer supplies its own small
bootstrap loader. No VersionBridge plugin is installed on the server.
