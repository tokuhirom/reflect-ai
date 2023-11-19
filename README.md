# ReflectAI

## What is this?

This is a free and open source personal assistant software based on AI technology.

## Supported platforms

 * Mac OS X Ventura or later

Maybe it can run on Windows or Linux, but I haven't tested it.

## Dependencies

 * Java 17+

## How to run

    ./gradlew desktopRun  -DmainClass=MainKt

で実行できます。

## Build guide

    ./gradlew packageDmg

This command generate a dmg file in `composeApp/build/compose/binaries/main/dmg/` directory.
