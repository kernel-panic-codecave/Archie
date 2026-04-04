# Archie Config V2 (Scaffold)

This directory contains phase-1 scaffolding for a UI-decoupled config system.

## Layout

- `model/` - UI-agnostic schema objects (`ConfigDocument`, `ConfigCategory`, `ConfigField`)
- `runtime/` - mutable runtime state and validation (`ConfigState`, `ConfigV2Engine`)
- `ui/` - adapter SPI (`ConfigUiAdapter`, `ConfigUiAdapters`)
- `legacy/` - bridge from current `ConfigSpec`/`CategorySpec` (`LegacyCategorySpecAdapter`)
- `ConfigV2Smoke.kt` - tiny smoke runner

## Quick verify

```zsh
cd /home/kernelpanic/IdeaProjects/Archie
./gradlew :common:compileKotlin --no-daemon
```

## Current bridge coverage

`LegacyCategorySpecAdapter` maps:

- boolean
- int
- string
- color (converted to `KColor`)

Additional field kinds can be added incrementally in the adapter.

