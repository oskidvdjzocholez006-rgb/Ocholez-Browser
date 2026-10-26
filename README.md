# Ocholez Browser — Android project

Features included:
- Google search and website address bar
- Multiple tabs (tap a tab to switch; tap its × area or long-press to close)
- Bookmarks and browsing history stored locally
- Private tab mode (private pages are not added to saved history)
- Dark mode toggle
- Downloads through Android DownloadManager

## Build an APK
1. Install Android Studio on a computer and allow it to install the Android SDK.
2. Extract this ZIP.
3. In Android Studio choose **Open** and select the `OcholezBrowser` folder.
4. Wait for Gradle sync. If prompted, install Android SDK Platform 35 and a compatible JDK (Android Studio's bundled JDK is fine).
5. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. When the build completes, click **locate**. The debug APK is usually at `app/build/outputs/apk/debug/app-debug.apk`.
7. Transfer the APK to your Android phone and install it. You may need to allow installation from that file manager.

## Important notes
- This ZIP is the Android source project, not a precompiled APK. A working Android SDK/Gradle build environment is needed to produce the APK.
- Private mode is a best-effort local privacy mode, not anonymity. Web servers and network providers may still see connections. For stronger isolation, close private tabs when finished; Android WebView cookie behavior can vary by device.
- Downloads use Android's app-specific Downloads folder and Android's download notification.
- This is a starter browser. Some websites may not work perfectly in WebView, and advanced browser features such as password sync, extensions, and safe-browsing protection are not included.
