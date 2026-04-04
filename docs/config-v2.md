# Config V2 (UI-Decoupled)

This is the first phase of a full config rewrite to decouple model/runtime from UI toolkits.

## What is in place

- `common` now has a UI-agnostic config model under `config/v2/model`.
- Runtime state/validation is in `config/v2/runtime`.
- UI adapters are abstracted by `config/v2/ui/ConfigUiAdapter`.
- A legacy bridge exists in `config/v2/legacy/LegacyCategorySpecAdapter`.

## Why this exists

The old system directly references Cloth Config classes from common code (`CategorySpec`).
That creates classloading risk and blocks alternate UIs like YACL.

## Current scope

Phase 1 intentionally supports a subset in the legacy adapter:

- boolean
- int
- string
- color (converted to `KColor`)

Additional field types can be added incrementally without changing the v2 runtime API.

## Next phases

1. Add remaining field conversions in `LegacyCategorySpecAdapter`.
2. Implement a Fabric/NeoForge client adapter for Cloth Config.
3. Implement a YACL adapter.
4. Flip `AConfigPlatform` screen registration to route through adapter selection.
5. Deprecate old UI-coupled builder path.

