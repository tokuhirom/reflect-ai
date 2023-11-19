# ReflectAI

## What is this?

This is a free and open source personal assistant software based on AI technology.

<img width="979" alt="image" src="https://github.com/tokuhirom/reflect-ai/assets/21084/ae5df307-97fd-47a8-bd00-73ab992b7f77">


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
