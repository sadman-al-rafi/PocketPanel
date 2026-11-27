# PocketPanel Release Process

This document defines how release builds are signed, versioned, verified and
published. No release has been published yet, and the release workflow is
never started automatically.

## 1. Fixed release identity

- `applicationId` and `namespace` are both `dev.pocketpanel` in
  `app/build.gradle.kts`. They must not change; changing the application ID
  makes Android and stores treat the app as a different application.
- The permanent release signing key is generated and stored by the project
  owner. This repository never generates, replaces or contains a key.
- Every future release must be signed with the **same** key and alias. The
  key validity should exceed 25 years and, for Google Play, must not expire
  before 22 October 2033.
- The signing certificate fingerprint is the identity check. Store its
  SHA-256 digest as a GitHub secret so CI can detect a wrong keystore.

## 2. Supplying signing credentials

Credentials are read from environment variables first, then from an
untracked `keystore.properties` at the project root. Nothing sensitive is
stored in Gradle files or in Git.

| Environment variable | `keystore.properties` key | Meaning |
|---|---|---|
| `POCKETPANEL_RELEASE_STORE_FILE` | `storeFile` | Path to the `.jks`/`.keystore` file |
| `POCKETPANEL_RELEASE_STORE_PASSWORD` | `storePassword` | Keystore password |
| `POCKETPANEL_RELEASE_KEY_ALIAS` | `keyAlias` | Key alias inside the keystore |
| `POCKETPANEL_RELEASE_KEY_PASSWORD` | `keyPassword` | Private-key password |

Example `keystore.properties` (never commit this file):

```properties
storeFile=/absolute/path/to/pocketpanel-release.jks
storePassword=<keystore password>
keyAlias=pocketpanel-release
keyPassword=<key password>
```

`keystore.properties`, `local.properties`, `*.jks` and `*.keystore` are
ignored by `.gitignore`. If no credentials are present, only release tasks
fail; `assembleDebug` and debug signing keep working with the SDK debug
keystore.

## 3. Backup requirements

Back up these four items together, encrypted and offline:

1. the permanent keystore file;
2. the keystore password;
3. the key alias;
4. the key password.

Also record the certificate SHA-256 fingerprint. Losing the keystore or its
passwords means future versions cannot be published as updates to the same
app; the key cannot be regenerated. If the key is ever compromised, stop
publishing and plan a key upgrade through the store rather than quietly
reusing a different local key.

## 4. Versioning policy

Versions are kept in the tracked `release/version.properties` file and read
by Gradle:

```properties
versionCode=1
versionName=0.1.0
```

- `versionCode` is a positive integer that **must increase** for every
  published release. Never reuse or decrease it. The current value `1` is
  valid because no release has been published.
- `versionName` is the user-visible `MAJOR.MINOR.PATCH` value. Update it with
  the code version in the same commit.
- The GitHub tag is always `v<versionName>` (for example `v0.1.0`). The
  release workflow rejects a tag that does not match this rule.

Before each release: increment `versionCode`, set `versionName`, review the
diff, and commit both together.

## 5. Local release build and verification

With credentials available:

```bash
./gradlew :app:assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`.

Verify the signature (use the build-tools `apksigner` from the Android SDK):

```bash
apksigner verify --print-certs --verbose app/build/outputs/apk/release/app-release.apk
```

Check the printed certificate SHA-256 digest against your recorded release
identity. You can also inspect the keystore without exposing the private
key:

```bash
keytool -list -v -keystore /absolute/path/to/pocketpanel-release.jks -alias pocketpanel-release
```

Install and smoke-test the APK on a device:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Run `./scripts/check-static.sh` before pushing any release-preparation
change. It validates the version file, application ID, signing wiring and
workflow triggers without needing a JDK.

## 6. GitHub Release workflow

`.github/workflows/release.yml` has **only** a `workflow_dispatch` trigger.
There are no `push`, `tag`, `pull_request`, `schedule`, `workflow_run` or
other automatic triggers.

Required repository secrets:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64 of the permanent keystore file |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `RELEASE_CERT_SHA256` | Expected certificate SHA-256 digest; colons, spaces and uppercase are accepted and normalized by the workflow |

Required repository configuration:

1. Create a GitHub environment named `release`.
2. Add required reviewers to that environment. The workflow declares
   `environment: release`, so a run pauses for approval before it can
   publish. Without reviewers, anyone with write access could trigger it.
3. Confirm that the default branch contains the workflow; `workflow_dispatch`
   only receives events from the default branch.

Manual inputs:

- `tag`: must equal `v<versionName>` from `release/version.properties`.
- `confirm`: must be typed as `PUBLISH`; any other value stops the job.

Workflow actions: validate the tag and secrets, decode the keystore into a
temporary runner file, build `:app:assembleRelease`, verify the APK with
`apksigner`, compare the certificate fingerprint to `RELEASE_CERT_SHA256`,
upload the signed APK as an artifact, create a source zip from the released
commit with `git archive`, upload that as a second artifact, and create the
GitHub Release with both the APK and the source zip plus generated notes.
The temporary keystore is removed in an `always()` step.

The source zip contains only tracked files at the released commit. It does
not contain `.git`, build outputs, local configuration or any signing
material. Locally, the equivalent archive can be produced with:

```bash
git archive --format=zip --output=pocketpanel-source.zip HEAD
```

## 7. Publishing procedure

1. Finish and verify the release locally (sections 4 and 5).
2. Push the version commit. This does **not** trigger CI (the verify workflow
   is manual only).
3. Request an authorized run of `verify-toolchain.yml` if a fresh build,
   lint and test report is needed.
4. With explicit approval, dispatch `release.yml` with the correct tag and
   `PUBLISH`.
5. Approve the `release` environment review.
6. Confirm the run log shows a successful `apksigner` verification and a
   matching certificate fingerprint.
7. Check the GitHub Release page: tag, notes, signed APK and source zip.
8. Verify the downloaded APK again with `apksigner verify --print-certs`,
   and confirm the source zip unpacks to the expected tracked tree.
9. Record the release tag, commit, APK digest and run ID in the project log.

Do not dispatch the release workflow without explicit owner approval, and
do not publish an untested build.

## 8. Security notes and concerns

- The runner is a third-party-owned environment. Secrets are exposed only
  to the job that needs them; never print them or pass them as command-line
  arguments that could appear in logs.
- The keystore exists on the runner only as a temporary file with `600`
  permissions and is removed even on failure.
- Fingerprint pinning (`RELEASE_CERT_SHA256`) prevents accidentally signing
  with the debug key or a different keystore.
- The `release` environment approval is the human gate; configure required
  reviewers before relying on this workflow.
- Debug builds are unaffected: they use the SDK-generated debug keystore.
- GitHub Actions artifacts are not a substitute for the published release
  asset; always verify the asset that users download.
- Store signing secrets separately from the repository and from device
  backups. Do not place them in `ci-results` or issue reports.

## 9. References

- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/studio/command-line/apksigner
- https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
