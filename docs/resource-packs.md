# Resource Pack Utilities

Archie provides a generic resource reload listener that automatically discovers and
deserializes data files using kotlinx.serialization.

---

## SerializationReloadListener

Extend `SerializationReloadListener<T>` to load a custom data type from resource packs:

```kotlin
@Serializable
data class ThemeDefinition(val primaryColor: Int, val accentColor: Int)

class ThemeLoader : SerializationReloadListener<ThemeDefinition>(
    format        = Json { ignoreUnknownKeys = true },
    serializer    = ThemeDefinition.serializer(),
    directory     = "themes",       // looks under assets/<ns>/themes/
    fileExtension = ".json",
) {
    override fun apply(
        prepared: Map<ResourceLocation, ThemeDefinition>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller,
    ) {
        ThemeRegistry.THEMES.clear()
        ThemeRegistry.THEMES += prepared
        LOGGER.info("Loaded {} theme(s)", prepared.size)
    }
}
```

Register the listener during `initClient()`:

```kotlin
// Architectury:
ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, ThemeLoader())
```

---

## Supported formats

Any kotlinx.serialization `StringFormat` works:

| Format | Import |
|--------|--------|
| JSON | `kotlinx.serialization.json.Json` |
| JSON5 | via tomlkt or custom |
| TOML | `net.peanuuutz.tomlkt.Toml` |

Files are discovered automatically by scanning the `directory` folder across all active
resource packs, with later packs overriding earlier ones for the same resource ID.
