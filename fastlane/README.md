fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android bump_build

```sh
[bundle exec] fastlane android bump_build
```

versionCode 증가 (app/build.gradle.kts)

### android build_release

```sh
[bundle exec] fastlane android build_release
```

AAB 빌드만 수행 (Release)

### android internal

```sh
[bundle exec] fastlane android internal
```

AAB 빌드 후 Play Console 내부 테스트 트랙 업로드 (draft)

### android promote_to_closed

```sh
[bundle exec] fastlane android promote_to_closed
```

내부 테스트 → 비공개 테스트 (closed) 트랙 승격

### android promote_to_production

```sh
[bundle exec] fastlane android promote_to_production
```

비공개 테스트 → 프로덕션 트랙 승격 (staged rollout 10%)

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

45개 locale 스크린샷 자동 촬영 (adb 기반)

### android upload_screenshots

```sh
[bundle exec] fastlane android upload_screenshots
```

스크린샷만 Play Console 업로드 (AAB/메타 제외)

### android upload_metadata

```sh
[bundle exec] fastlane android upload_metadata
```

스크린샷 + 메타 업로드 (AAB 제외)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
