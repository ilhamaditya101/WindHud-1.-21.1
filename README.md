# WindHUD (Paper 1.21.1)

Standalone plugin with no third-party plugin dependencies.


## Build
Use GitHub Actions (Actions tab -> Build WindHUD -> Run workflow) or run `gradle build` with Java 21 and Gradle 8.14.
Artifact: `build/libs/WindHUD-1.0.0.jar`.

## Commands
- `/windhud` or `/windhud toggle` - toggle HUD
- `/windhud on` - enable
- `/windhud off` - disable
- `/windhud reload` - reload config (admin permission)

## Notes
This first version uses a native BossBar as the top HUD/frame, not a custom textured UI. No resource pack is required. Wind is simulated and shared globally; direction and strength change on the configured interval. It does not read real weather.
