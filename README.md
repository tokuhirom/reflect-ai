# ReflectAI

## What is this?

This is a free and open source personal assistant software based on AI technology.

<img width="979" alt="image" src="https://github.com/tokuhirom/reflect-ai/assets/21084/ae5df307-97fd-47a8-bd00-73ab992b7f77">

## Features

 * Talk with OpenAI GPT-3/GPT-4
 * Builtin functions to support Function Calling.
   * Google search
   * Image generation based on DALL-E-2
   * Fetch content via URL

## Supported platforms

 * Mac OS X Ventura or later
 * Java 17+

Maybe it can run on Windows or Linux, but I haven't tested it.

## How to run

    ./gradlew desktopRun  -DmainClass=MainKt

で実行できます。

## Build guide

    ./gradlew packageDmg

This command generate a dmg file in `composeApp/build/compose/binaries/main/dmg/` directory.

## Download Llama model

See also

 * https://blog.curegit.jp/posts/ai/nlp/run-japanese-llama-cpp/
 * https://huggingface.co/mmnga/ELYZA-japanese-Llama-2-7b-instruct-gguf

   mkdir -p ~/ReflectAI/models/llama/
   curl -L https://huggingface.co/mmnga/ELYZA-japanese-Llama-2-7b-instruct-gguf/resolve/main/ELYZA-japanese-Llama-2-7b-instruct-q2_K.gguf -o ~/ReflectAI/models/llama/ELYZA-japanese-Llama-2-7b-instruct-q2_K.gguf
   

