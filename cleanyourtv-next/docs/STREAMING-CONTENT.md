# Content from your streaming app (Nuvio)

The panel above the dock, with its three tabs Explore, Movies and Series, shows **what your streaming app itself publishes for the Android TV home screen**. No TMDB key, no YouTube key, no account and no server of ours: CleanYourTV reads the app's own rows from the TV.

## How it works

Android TV has a content provider for the home screen (`android.media.tv`). An app that supports it publishes rows there: **"Continue watching"** (Watch Next) and its own **channels** (preview programs), each with a title, a picture, a progress and the intent that opens it. The stock home screen displays them. CleanYourTV reads the same rows for the app you chose, and shows them in its own style:

- Explore: everything the app published, "Continue watching" first, then each of its channels.
- Movies and Series: the same, filtered by the type the app gave each entry (movie, series, season, episode).
- OK opens the entry with the intent the app published, or resumes it. For safety, an intent is started only if it targets the chosen app itself.
- The panel keeps showing this content while you move along the dock, whichever tile is selected. Only the weather tile swaps it for the weather.

## What you must do

1. In **Settings, Streaming content**, check the app (it is found automatically if its name contains "Nuvio"; otherwise pick it from the list) and press OK on **Permission to read the TV home content**. Android asks once.
2. In the streaming app's **own settings**, turn on its option to show content on the Android TV home screen (names vary: home screen channels, continue watching on the home screen, Watch Next). An app publishes nothing until you do.

## What is not known (not tested on a TV)

I could not run this on a TV, so these are open, and the panel says which one applies:

- **Does your Nuvio publish anything?** I found no official statement that the Nuvio Android TV app publishes home-screen channels. If it does not, the panel says "Nuvio publishes nothing for the Android TV home screen", and no setting of CleanYourTV can change that.
- **Does your TV let a third-party home screen read them?** The permission `READ_TV_LISTINGS` is of the "dangerous" kind (requested at run time) in the Android sources I could find, which means an ordinary app may ask for it. But whether the TV provider then returns another app's rows, and whether Xiaomi's firmware does, could not be checked here. If the TV refuses, the panel says so, and Settings shows the last outcome.
- **The package name** of Nuvio is not known to me: it is found by name, or chosen by you.
- **Pictures** are loaded from the address the app published (`https://` or its own `content://`). Anything else is refused.

If this does not work on your TV, the honest answer is that the TV or the app does not offer it; no workaround is built in (no accessibility service, no notification access).

## Privacy

The permission lets CleanYourTV read the TV's channel and program listings. It only keeps the rows of the app you chose, in memory, and nothing is sent anywhere. Pictures are cached on the TV.
