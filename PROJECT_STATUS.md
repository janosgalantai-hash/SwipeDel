# SwipeDel — Project Status & Context

## Mi ez az app?
Android alkalmazás fotók gyors törlésére. Fullscreen képnézegető, balra swipe = trash, jobbra = megtart. Nincs per-swipe dialog — a képek az app saját privát trash mappájába kerülnek, és csak az "Empty Trash" gombbal törlődnek véglegesen (egy system dialog az összeshez).

## Tech stack
- Android, Jetpack Compose, Kotlin
- Coil képbetöltés
- MediaStore API
- Min SDK: 26 (Android 8), Target SDK: 36
- Package: `com.swipedel`

## Repo
- GitHub: https://github.com/janosgalantai/SwipeDel
- Branch: master
- 2 commit: initial release + thumbnail browser

## Főbb funkciók (kész)
- Swipe left/right animáció piros/zöld overlay-el
- App-privát Trash mappa (copy, nem delete — no permission dialog)
- Trash screen: multi-select + Restore gomb + Empty Trash (egy dialog)
- Browse screen: thumbnail grid, tap = jump to that photo, current position kiemelve
- Animated monochrome splash screen
- Power gomb (exit) a top barban
- Custom swipe-left launcher ikon (Tabler Icons, MIT)
- Android 12+ system splash elnyomva (values-v31)

## Fájlstruktúra (fontosak)
```
app/src/main/java/com/swipedel/
  MainActivity.kt       — minden fő logika
  SplashScreen.kt       — animált splash
app/src/main/res/drawable/
  ic_arrow_left.xml     — custom wide arrow
  ic_arrow_right.xml    — custom wide arrow
  ic_power.xml          — exit gomb ikon
  ic_grid.xml           — browse gomb ikon
  ic_check.xml          — trash selection checkmark
  ic_launcher_foreground.xml  — swipe-left ikon (Tabler, MIT)
  ic_launcher_background.xml  — #1A1A1A háttér
  ic_transparent.xml    — system splash elnyomáshoz
app/src/main/res/values-v31/themes.xml  — Android 12+ splash config
```

## Signing / Release build
- Keystore: `C:/Users/janos/swipedel-release.jks`
- Alias: `swipedel`
- Credentials: `local.properties`-ben (gitignore-ban van, nem publikus)
- Release AAB build parancs (PowerShell, két sor):
  ```
  $env:JAVA_HOME = "C:/Program Files/Android/Android Studio/jbr"
  & "C:/Users/janos/AndroidStudioProjects/SwipeDel/gradlew.bat" -p "C:/Users/janos/AndroidStudioProjects/SwipeDel" bundleRelease
  ```
- AAB helye: `app/build/outputs/bundle/release/app-release.aab`
- Jelenlegi versionCode: 2, versionName: 1.0

## Google Play Console státusz (2026-05-04)
- Developer account: Janos A Galantai, Auckland NZ
- App: SwipeDel, com.swipedel
- **Internal testing: Active** (AAB v1 feltöltve)
- **Closed testing (Alpha): In review** (AAB v2 feltöltve, Google vizsgálja)
- Open testing: Inactive
- Production: Inactive (closed test után lehet alkalmazni)

### Closed testing követelmények (még folyamatban)
- [ ] 12 tesztelő opt-in szükséges
- [ ] 14 napos futás a 12. opt-in után
- Tesztelő lista neve: `SwipeDel` (Play Console-ban)
- Opt-in link: Play Console → Closed testing → Alpha → Testers fülön

### Tesztelők szerzése
- Reddit: r/betatesting, r/androidapps
- Minta poszt szöveg a README-ben

### Store listing státusz
- App name: SwipeDel
- Short description: kész
- Full description: kész
- Content rating: PEGI 3 / Everyone (kész)
- Target audience: 18+ (kész)
- Data safety: No data collected (kész)
- Privacy policy: https://github.com/janosgalantai/SwipeDel/blob/master/privacy-policy.md
- App category: Tools
- Ads declaration: No ads (kész)
- Price: $3.99 (fizetős)
- Screenshotok: szükséges (még hiányozhat)
- Feature graphic: 1024x500px (SVG kész: `feature-graphic.svg`, konvertálni kell PNG-be)
- Play Store icon: 512x512px (SVG kész: `playstore-icon.svg`, konvertálni kell PNG-be)

### SVG → PNG konverzió
- Eszköz: cloudconvert.com/svg-to-png
- `playstore-icon.svg` → 512x512 PNG
- `feature-graphic.svg` → 1024x500 PNG

## Következő lépések
1. Megszerezni 12 tesztelőt (Reddit r/betatesting)
2. Megvárni a Google review-t (1-3 nap)
3. 14 napos closed test lefuttatása
4. Production access kérelem
5. App megjelenik a Play Store-on

## Fizetés
- Google Payments profil: beállítva (Janos A Galantai, Auckland)
- Payment method: hozzá kell adni bankszámlát
- Google 15-30%-ot elvesz, a többi a fejlesztőé

## Hasznos linkek
- Play Console: https://play.google.com/console
- GitHub repo: https://github.com/janosgalantai/SwipeDel
- Privacy policy: https://github.com/janosgalantai/SwipeDel/blob/master/privacy-policy.md
