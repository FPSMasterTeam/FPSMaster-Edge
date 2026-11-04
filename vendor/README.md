# Vendored dependencies

`maven/top/fpsmaster/prism/0.3.0` contains the Prism 0.3.0 Java 8 jar, source jar, POM and Gradle module metadata, built from the FPSMaster Prism repository by the final-release workstream. Gradle resolves this module from this directory before any external repository; no local Maven installation or private credentials are required.

- Upstream: https://github.com/FPSMasterTeam/fpsmaster-prism
- License: MIT (see the accompanying source repository and notices).
- Jar SHA-256: `dbd6c0ac5570deaee8a5ffca6006de1ce7b46d296ec2b85c2cc62778134b32fc`
- Source jar SHA-256: `55eb8b99d73fe9ba6599ece2d7f161f31ecc73cfdefe1a3acda0aadc44ce7188`

Other public Minecraft, Forge and Maven dependencies still require an initial online resolution. `--offline` works only after those dependencies and the Gradle distribution have been cached.
