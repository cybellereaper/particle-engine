# ParticleEngine

Production-grade particle engine plugin for Paper servers.

## Quick start

1. Build the plugin jar.
   ```bash
   ./gradlew build
   ```
2. Copy `build/libs/particles-<version>.jar` to your Paper server `plugins/` folder.
3. Start/restart the server once so default config files are generated.
4. Use `/pe list` in-game to verify templates were loaded.

## Command how-to examples

The main command is `/particleengine` (aliases: `/pe`, `/particles`).

### 1) Spawn a built-in template
```text
/pe spawn trail_smoke
```
Spawns the `trail_smoke` template at your current location.

### 2) Spawn another built-in template
```text
/pe spawn orbital_arcane
```
Spawns the `orbital_arcane` template.

### 3) Save a template alias, then reuse it
```text
/pe save aura orbital_arcane
/pe spawn @aura
```
This stores `aura -> orbital_arcane` in `named-effects.yml` and lets you spawn by alias.

### 4) Stop an effect by runtime id
```text
/pe stop 3f0ec0c8-0ca5-46b8-baa0-b3fa88a01f7e
```
Use when you have a specific runtime UUID to stop.

### 5) Stop effects by tag
```text
/pe stop tag:bossFight
```
Stops all active effects with the tag `bossFight`.

### 6) Reload config and effect templates
```text
/pe reload
```
Reloads `config.yml` and all templates from `plugins/ParticleEngine/effects/*.yml`.

### 7) Toggle debug mode (player only)
```text
/pe debug
```
Turns debug visuals/messages on or off for your player.

## Effect template how-to example

Create a new file under `plugins/ParticleEngine/effects/`, for example `my-effects.yml`:

```yaml
effects:
  heal_ring:
    family: ORBITAL
    particle: ENCHANT
    count: 1
    lifetimeTicks: 120
    anchor:
      type: PLAYER
      selector: self
      space: LOCAL
    emitters:
      - shape: CIRCLE
        rate: 1
        points: 28
        radius: 1.2
    modifiers:
      - type: SPIN_YAW
        speed: 3.0
```

Then reload and spawn it:

```text
/pe reload
/pe spawn heal_ring
```


Anchor selector supports common anchor points when resolving from an entity/player:
- `feet` (default)
- `head`
- `back`

`/pe spawn` now anchors to the executing player entity, so these selectors apply there too.

Example:
```yaml
anchor:
  type: PLAYER
  selector: head
  space: LOCAL
```


## API how-to examples (for other plugins)

### Spawn an effect at a location
```java
import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.ParticleEngineApiProvider;
import com.github.cybellereaper.particleengine.api.SpawnRequest;

ParticleEngineApi api = ParticleEngineApiProvider.get();
api.spawn("trail_smoke", SpawnRequest.at(player.getLocation()));
```

### Spawn with custom tags/metadata
```java
import com.github.cybellereaper.particleengine.api.SpawnRequest;

SpawnRequest request = new SpawnRequest(
    player.getUniqueId(),
    player.getLocation(),
    player.getUniqueId(),
    java.util.Set.of("bossFight", "phase2"),
    java.util.Map.of("scale", 1.3)
);

java.util.UUID runtimeId = api.spawn("orbital_arcane", request);
```

### Stop by runtime id or tag
```java
api.stop(runtimeId);
api.stopByTag("bossFight");
```

## Permissions

Default command permissions are defined in `plugin.yml`:
- `particleengine.command.help` (default: true)
- `particleengine.command.spawn`
- `particleengine.command.stop`
- `particleengine.command.list`
- `particleengine.command.reload`
- `particleengine.command.debug`
- `particleengine.command.save`
- `particleengine.effect.*`
