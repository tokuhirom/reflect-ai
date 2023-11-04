This is a Kotlin Multiplatform project targeting Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CryptoKit for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

- トークンが溢れたときのしょりが無い。
  - とりあえず、直近の６行ぐらいだけ使うようにすれば良さそう。
- 日付が変わったときのしょりが無い
  - 日ごとにファイル分けたいなぁ
