# Poll-in-one

## What does this do

Create and join poll in one action.

## Motivation

People waste a lot of their precious time to gather opinion for simple group activities.
Think about you and your friends wasting time at fraternities, sororities or school club
to count *the number of raised hands* again and again to decide where to eat out.

## How does it work

With Poll-in-one, you can create a room for vote. The room information is broadcasted
via bluetooth and sound<sup>1</sup>. Other friends can join your room by simply launching
the application. Their Poll-in-one will catch the broadcasted signal and automatically let
your friends join the room. Joining a vote can never be easier than using Poll-in-one.

<sup>[1] Tentative. This feature may not be available at the time of release.</sup>

## Disclaimer

This application is built as a term project for 2017 Spring CS442 Introduction to Mobile
Computing class held in KAIST.

The application and code is provided AS IS, and we won't take any resposibilities emerged
by using this application or code.

## Running Environment
1. Android 5.0 or above
2. `Google Play` application is needed to use `Google Nearby` functionality.

    2-1. If your device does not have `Google Play`, then please click `publish` button when boradcating a poll information, which generates an audible encoded sound. 

## Build Environment
### Requirement
1. Android Studio 2.3.3 [[link]](https://developer.android.com/studio/index.html?hl=ko)
2. Kotlin plugin [[link]](https://kotlinlang.org/docs/tutorials/kotlin-android.html#installing-the-kotlin-plugin)

### How to build
1. Clone or download the repository
2. Open the project with Andorid Sudio, where Kotlin Plugin is installed. 
3. Run `build` and test it. 
