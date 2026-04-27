# ParticleEngine

Production-grade particle engine plugin for Paper servers. Now ships with a
small embedded scripting language (**PEScript**) for sequencing complex shows
without restarting the server.

## Quick start

1. Build the plugin jar.
   ```bash
   ./gradlew build
   ```
2. Copy `build/libs/particles-<version>.jar` to your Paper server `plugins/` folder.
3. Start/restart the server once so default config files are generated.
4. Use `/pe list` in-game to verify templates were loaded.

## Command reference

The main command is `/particleengine` (aliases: `/pe`, `/particles`).

| Command                                              | Description                                                                       |
|------------------------------------------------------|-----------------------------------------------------------------------------------|
| `/pe help`                                           | List available subcommands.                                                       |
| `/pe spawn <template\|@saved>`                       | Spawn a built-in or saved-alias effect at the player's location.                  |
| `/pe at <x> <y> <z> <template> [world]`              | Spawn a template at explicit coordinates (world defaults to player's world).      |
| `/pe info <template>`                                | Print template metadata (family, particle, emitters, modifiers, timeline).        |
| `/pe stop <runtimeId\|tag:tag\|all>`                 | Stop one effect by UUID, all effects with a tag, or every active effect.          |
| `/pe pause <runtimeId\|tag:tag\|all>`                | Pause without stopping. Time stops advancing for paused effects.                  |
| `/pe resume <runtimeId\|tag:tag\|all>`               | Resume previously paused effects.                                                 |
| `/pe list`                                           | Summary of templates and active runtime effects.                                  |
| `/pe save <alias> <template>`                        | Save a friendly alias to `named-effects.yml`.                                     |
| `/pe reload`                                         | Reload `config.yml`, all `effects/*.yml` templates, and all `scripts/*.pes`.      |
| `/pe debug`                                          | Toggle per-player debug visuals/messages.                                         |
| `/pe run <scriptName>`                               | Execute a PEScript file from `plugins/ParticleEngine/scripts/`.                   |
| `/pe scripts`                                        | List discovered scripts and any currently running script runs.                    |
| `/pe stopscript <runId\|all>`                        | Cancel a specific script run (or every running script).                           |

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

```yaml
anchor:
  type: PLAYER
  selector: head
  space: LOCAL
```

## Modifiers

Modifiers transform every sampled emitter point each tick. Numeric parameters
support keyframed values (see `KeyframeValueResolver`).

| Type         | Parameters                                | Effect                                                |
|--------------|-------------------------------------------|-------------------------------------------------------|
| `SPIN_YAW`   | `speed` (deg/tick)                        | Rotate around the Y axis.                             |
| `SPIN_PITCH` | `speed`                                   | Rotate around the X axis.                             |
| `SPIN_ROLL`  | `speed`                                   | Rotate around the Z axis.                             |
| `WAVE_X`     | `amplitude`, `frequency`                  | Sinusoidal X offset over time.                        |
| `WAVE_Y`     | `amplitude`, `frequency`                  | Sinusoidal Y offset over time.                        |
| `WAVE_Z`     | `amplitude`, `frequency`                  | Sinusoidal Z offset over time.                        |
| `OFFSET`     | `x`, `y`, `z`                             | Constant translation.                                 |
| `GRAVITY`    | `strength`                                | Downward acceleration over the lifetime of the point. |
| `SCALE`      | `factor` or `x`, `y`, `z`                 | Multiplicative scale on each axis.                    |
| `PULSE`      | `amplitude`, `frequency`                  | Radial breathing scale around origin.                 |
| `JITTER`     | `amplitude`                               | Random ±amplitude offset on every tick.               |
| `NOISE`      | `amplitude`, `frequency`                  | Smooth pseudo-noise drift.                            |

### Easings

Timelines and keyframed values support a wide range of easings:
`linear`, `step`, `smoothstep`, `smootherstep`,
`ease_in`/`ease_out`/`ease_in_out` (alias for `_quad` variants),
`ease_in_quad`, `ease_out_quad`, `ease_in_out_quad`,
`ease_in_cubic`, `ease_out_cubic`, `ease_in_out_cubic`,
`ease_in_quart`, `ease_out_quart`, `ease_in_out_quart`,
`ease_in_sine`, `ease_out_sine`, `ease_in_out_sine`,
`ease_in_expo`, `ease_out_expo`, `ease_in_out_expo`,
`ease_in_circ`, `ease_out_circ`, `ease_in_out_circ`,
`ease_in_back`, `ease_out_back`, `ease_in_out_back`,
`ease_in_elastic`, `ease_out_elastic`, `ease_in_out_elastic`,
`ease_in_bounce`, `ease_out_bounce`, `ease_in_out_bounce`.

## PEScript

PEScript is a tiny dynamically-typed language designed for choreographing
effects from a single file. Scripts live in `plugins/ParticleEngine/scripts/`
and use the `.pes` extension. Run them with `/pe run <scriptName>`.

### Language tour

```text
// Comments use //, /* ... */ also works.

let count = 6;        // variables (mutable)
let tags = ["boss"];  // lists
let opts = map("scale", 1.5);  // maps from key/value pairs

if (count > 3) {
    log("plenty");
} else {
    log("not many");
}

while (count > 0) {
    count = count - 1;
}

for (i in range(0, 5)) {
    log("step", i);
}

fn burst(template, t) {
    spawn(template, at(0, 64, 0), tags);
    wait t;
    stop_tag("boss");
}

burst("orbital_arcane", 40);
```

`wait <expr>` suspends the script for the given number of server ticks
without blocking the main thread.

### Built-ins

Engine bridges:
- `spawn(templateId, [location], [tags], [overrides])` — returns the runtime
  UUID as a string. `location` may be `at(x, y, z)` or `at(world, x, y, z)`.
  `tags` is a list, `overrides` is a map.
- `stop(runtimeId)` / `stop_tag(tag)` / `stop_all()`
- `pause(runtimeId)` / `pause_tag(tag)` / `resume(runtimeId)` / `resume_tag(tag)`
- `template_exists(name)` — boolean.
- `log(...)` — prints to the server log.

Math: `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `abs`, `floor`,
`ceil`, `round`, `sqrt`, `sign`, `pi`, `e`, `pow`, `min`, `max`, `clamp`,
`lerp`, `random`, `randint`.

Collections / utility: `len`, `range`, `list`, `map`, `push`, `keys`,
`values`, `contains`, `str`, `num`, `at`, `tags`.

### Example: pulsating boss aura

```text
log("Boss aura starting!");

for (i in range(20)) {
    spawn("pulsing_aura", at(0, 64, 0), ["boss"]);
    wait 10;
}

stop_tag("boss");
```

Save it as `plugins/ParticleEngine/scripts/boss.pes` and run with
`/pe run boss`.

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

### Stop, pause, or resume
```java
api.stop(runtimeId);
api.stopByTag("bossFight");
api.pause(runtimeId);
api.resume(runtimeId);
api.pauseByTag("bossFight");
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
- `particleengine.command.info`
- `particleengine.command.at`
- `particleengine.command.pause`
- `particleengine.command.resume`
- `particleengine.command.script`
- `particleengine.effect.*`
