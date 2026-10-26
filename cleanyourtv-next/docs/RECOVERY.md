# Becoming Home, and going back to Google's launcher

CleanYourTV never disables Google's launcher. Both stay installed and enabled, so going back is always possible and needs nothing technical.

## Try it without changing anything

Open **CleanYourTV** from the TV's app row. It runs as an ordinary app: Home still leads to Google's launcher. Use this to check the look and the remote navigation first.

## Make it the Home screen

Press the **Home** button while CleanYourTV is installed. If Android asks which Home screen to use, choose CleanYourTV, then **Always** (or **Just once** to try it).

What happens next depends on the firmware, and I could not test it on your Mi TV Stick:

- **The question appears and the choice sticks**: done.
- **Home keeps opening Google's launcher and no question appears**: the firmware decides, not the app. Some Xiaomi firmware only lets launchers on its own allow-list take the Home button. The repository records one such case in `docs/devices/xiaomi-mitv-aetp0-dofus.md` (another Xiaomi model). It is not known whether the Mi TV Stick Full HD does the same.

If the firmware refuses, the safe options are:

1. **Keep using CleanYourTV from the app row.** It works fully that way, and Home still returns to Google.
2. **Do nothing clever.** CleanYourTV does not borrow another app's package name, does not use an accessibility service to redirect the button, and does not change secure settings. These tricks exist, and they can leave a TV without a working Home screen.
3. Ask Xiaomi's support or forum whether the model allows a third-party Home screen.

The **Home screen of the system** row in CleanYourTV's Settings opens Android's own home settings page, when the TV has one. Many TVs do not, and the app then says so.

## Go back to Google's launcher

Pick whichever is easiest, from the least to the most drastic. Each leaves Google's launcher exactly as it was.

1. **Clear the default**: TV **Settings**, **Apps**, **CleanYourTV**, then **Clear defaults** (the wording varies). The next press of Home asks again: choose Google's launcher.
2. **Uninstall CleanYourTV**: TV **Settings**, **Apps**, **CleanYourTV**, **Uninstall**. Google's launcher is then the only Home screen left, so Home returns to it at once.
3. **Over ADB**, if you use it. This removes only this app:

   ```
   adb uninstall com.cleanyourtv.launcher
   ```

If the screen shows nothing useful (a black screen with the app stuck), unplug the stick for ten seconds and plug it back in: nothing here runs at boot, so the TV starts normally. Then use option 2.
