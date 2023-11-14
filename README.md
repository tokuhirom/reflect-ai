This is a Kotlin Multiplatform project targeting Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder
      name.
      For example, if you want to use Apple’s CryptoKit for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

Learn more
about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

- 基本的に mac で動くことだけを想定してます。

- chatgpt をつかった、パーソナルアシスタント的なものを実装したい。
  - チャット UI にこだわりはないが、一旦 chat ui っぽいやつを実装してしまった。
