SearchTwitter
=============
Android app, which allows to search tweets as user types

Overview
--------

User can search tweets with a given keyword as he or she types or by pressing search icon. Application has infinite scroll. Implementation of the dynamic search is quite simple thanks to RxJava and Reactive Programming principles.

Configuration of Twitter API keys and tokens
--------------------------------------------

Go to https://apps.twitter.com/ website, register your account and Twitter app. Next, generate your keys and tokens. When you have them, go to the `app/src/main/java/com/pwittchen/search/twitter/twitter/TwitterOAuthConfig.java` file and put your generated tokens and keys there. After that, you can build and run the app.

Building the project
--------------------

We can build project with Gradle Wrapper and the following command:

```
./gradlew build
```

Tests
-----

Tets are located in `app/src/androidTest/java` and can be executed on device or emulator with the following command:

```
./gradlew connectedCheck
```

Reports from tests are located in `app/build/reports/androidTests/` directory.

Static Code Analysis
--------------------

Project uses the following tools for static code analysis configured in `config/quality.gradle`
file:
- Checkstyle
- PMD
- FindBugs
- Android Lint

Reference to this file is added in `app/build.gradle` file.

Static Code analysis can be executed with the following command:

```
./gradlew check
```

Reports from analysis are generated in `app/build/reports/` directory.

Used libraries
--------------
- In application
  - RxJava
  - RxAnadroid
  - Dagger 2
  - ButterKnife
  - Joda Time
  - Picasso
  - Twitter4J
  - Material SearchView
- In tests
  - JUnit4
  - Google Truth
  - Mockito
