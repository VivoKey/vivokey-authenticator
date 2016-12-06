# VivoKey Authenticator

## Release

* bump `versionCode` and `versionName` in `app/build.gradle`
* update NEWS
* `./gradlew assembleRelease`
* `cp app/build/outputs/apk/app-release-unsigned.apk app-release-unaligned.apk`
* `jarsigner app-release-unaligned.apk vivo -keystore vivoauth.keystore`
* `zipalign -v 4 app-release-unaligned.apk app-release.apk`
