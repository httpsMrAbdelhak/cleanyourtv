# TMDB: trending titles and series posters

The Explore page (trending films and series of the week) and the posters on the cards of your streaming app come from [The Movie Database](https://www.themoviedb.org). TMDB needs a key, and **the key is yours: it is never in this repository.**

## Where the key goes

Pick one:

1. **In Settings on the TV**: Settings, Streaming content, TMDB key. Type a v3 API key (32 characters) or a v4 read access token. Settings shows only its last four characters. "Forget the typed TMDB key" removes it.
2. **In the build**: pass `TMDB_API_KEY` as an environment variable or a Gradle property (`./gradlew assembleDebug -PTMDB_API_KEY=...`). For the GitHub workflow, add a repository secret named `TMDB_API_KEY` (repository Settings, Secrets and variables, Actions, New repository secret). The workflow hands it to the build, and the APK then works without typing anything.

A key typed in Settings wins over the one in the APK.

**Mind that a key built into the APK can be read back from the APK** by anyone who has the file. On a public repository, the workflow's artifacts can be downloaded by any signed-in GitHub user. A TMDB key only reads public film data and can be regenerated on themoviedb.org at any time, but if that matters to you, leave the secret out and type the key on the TV instead.

## What is sent to TMDB

The titles (and the year of a film) of the cards your streaming app published, or the TMDB or IMDb id found in the app's intent, plus the language (fr-FR or en-US). Nothing about you, your account or your TV. Pictures are then fetched from `image.tmdb.org`.

## Without a key

Explore says that a key is needed and offers Settings. The cards of your streaming app show the app's own pictures. Nothing else changes.

## Not tested live

The sandbox that wrote this could not reach TMDB. The parsers are tested on samples of the documented answers (`TmdbTest`). A refused key shows "TMDB refused the key"; Settings, Status, TMDB shows the last outcome.

This product uses the TMDB API but is not endorsed or certified by TMDB.
