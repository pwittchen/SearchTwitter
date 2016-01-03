SearchTwitter [![Build Status](https://travis-ci.org/pwittchen/SearchTwitter.svg)](https://travis-ci.org/pwittchen/SearchTwitter)
=============
Android app, which allows to search tweets as user types

Contents
--------

- [Overview](#overview)
- [Configuration of Twitter API keys and tokens](#configuration-of-twitter-api-keys-and-tokens)
- [Twitter API rate limits](#twitter-api-rate-limits)
- [Building the project](#building-the-project)
- [Tests](#tests)
- [Static Code Analysis](#static-code-analysis)
- [Code style](#code-style)
- [Used libraries](#used-libraries)

Overview
--------

User can search tweets with a given keyword as he or she types or by touching search icon. Moreover, application has so called infinite scroll for tweets. Implementation of the dynamic search is quite simple thanks to RxJava and Reactive Programming principles.

Configuration of Twitter API keys and tokens
--------------------------------------------

Go to https://apps.twitter.com/ website, register your account and Twitter app. Next, generate your keys and tokens.
When you have them, go to `gradle.properties` file in the repository
and add the following contents to this file:

```
TWITTER_CONSUMER_KEY=your-consumer-key
TWITTER_CONSUMER_SECRET=your-consumer-secret
TWITTER_ACCESS_TOKEN=your-access-token
TWITTER_ACCESS_TOKEN_SECRET=your-access-token-secret
```

and set your keys and tokens to appropriate variables.

After that, you can build and run the app.

Twitter API rate limits
-----------------------

Please remember that Twitter API has its own [rate limits](https://dev.twitter.com/rest/public/rate-limiting), so when user will perform too many requests in a short period of time, further requests may be blocked for a given amount of time. Don't worry - it's not so long. Error code for rate limit is provided in `TwitterApiProvider` class and is used by RxJava error handling in `MainActivity` class.

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

Code style
----------

Code style used in the project is called `SquareAndroid` from Java Code Styles repository by Square available at: https://github.com/square/java-code-styles.

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
  - MaterialSearchView
- In tests
  - JUnit4
  - Google Truth
  - Mockito
