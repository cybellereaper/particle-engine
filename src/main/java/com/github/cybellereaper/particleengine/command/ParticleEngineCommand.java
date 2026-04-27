package com.github.cybellereaper.particleengine.command;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.SpawnRequest;
import com.github.cybellereaper.particleengine.debug.DebugService;
import com.github.cybellereaper.particleengine.effect.EffectTemplate;
import com.github.cybellereaper.particleengine.persistence.NamedEffectStore;
import com.github.cybellereaper.particleengine.runtime.ActiveEffect;
import com.github.cybellereaper.particleengine.script.ScriptError;
import com.github.cybellereaper.particleengine.script.ScriptHost;
import com.github.cybellereaper.particleengine.script.ScriptLoader;
import com.github.cybellereaper.particleengine.script.ScriptRuntime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ParticleEngineCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "spawn", "stop", "list", "reload", "debug", "save",
            "info", "at", "pause", "resume", "run", "stopscript", "scripts"
    );

    private final ParticleEngineApi api;
    private final DebugService debugService;
    private final Runnable reloadAction;
    private final NamedEffectStore namedStore;
    private final ScriptRuntime scriptRuntime;
    private final ScriptLoader scriptLoader;
    private final ScriptHost scriptHost;
    private final Map<String, String> namedTemplates = new HashMap<>();

    public ParticleEngineCommand(ParticleEngineApi api,
                                 DebugService debugService,
                                 Runnable reloadAction,
                                 NamedEffectStore namedStore,
                                 ScriptRuntime scriptRuntime,
                                 ScriptLoader scriptLoader,
                                 ScriptHost scriptHost) {
        this.api = api;
        this.debugService = debugService;
        this.reloadAction = reloadAction;
        this.namedStore = namedStore;
        this.scriptRuntime = scriptRuntime;
        this.scriptLoader = scriptLoader;
        this.scriptHost = scriptHost;
        this.namedTemplates.putAll(namedStore.load());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            if (!sender.hasPermission(PermissionNodes.HELP)) return deny(sender);
            help(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "spawn" -> spawn(sender, args);
            case "stop" -> stop(sender, args);
            case "list" -> list(sender);
            case "reload" -> reload(sender);
            case "debug" -> debug(sender);
            case "save" -> save(sender, args);
            case "info" -> info(sender, args);
            case "at" -> at(sender, args);
            case "pause" -> pause(sender, args);
            case "resume" -> resume(sender, args);
            case "run" -> run(sender, args);
            case "stopscript" -> stopScript(sender, args);
            case "scripts" -> scripts(sender);
            default -> { help(sender); yield true; }
        };
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SPAWN)) return deny(sender);
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use spawn without API anchor override.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe spawn <template|@savedName>");
            return true;
        }
        String template = resolveTemplateAlias(args[1]);
        if (template.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Unknown saved effect alias: " + args[1]);
            return true;
        }
        try {
            SpawnRequest request = new SpawnRequest(player.getUniqueId(), player.getLocation(), player.getUniqueId(), Set.of(), Map.of());
            UUID id = api.spawn(template, request);
            sender.sendMessage(ChatColor.GREEN + "Spawned effect " + template + " with runtime id " + id);
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Spawn failed: " + ex.getMessage());
        }
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.STOP)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe stop <runtime-uuid|tag:tagName|all>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int removed = api.stopAll();
            sender.sendMessage(ChatColor.GREEN + "Stopped " + removed + " active effect(s).");
            return true;
        }
        if (args[1].startsWith("tag:")) {
            int removed = api.stopByTag(args[1].substring("tag:".length()));
            sender.sendMessage(ChatColor.GREEN + "Stopped " + removed + " effect(s).");
            return true;
        }
        try {
            boolean stopped = api.stop(UUID.fromString(args[1]));
            sender.sendMessage(stopped ? ChatColor.GREEN + "Stopped." : ChatColor.YELLOW + "Runtime ID not found.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID.");
        }
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.LIST)) return deny(sender);
        sender.sendMessage(ChatColor.AQUA + "Templates (" + api.templates().size() + "): " +
                api.templates().stream().map(t -> t.id() + "[" + t.family() + "]").collect(Collectors.joining(", ")));
        long paused = api.activeEffects().stream().filter(ActiveEffect::isPaused).count();
        sender.sendMessage(ChatColor.AQUA + "Active runtime effects: " + api.activeEffects().size() +
                (paused > 0 ? ChatColor.GRAY + " (paused: " + paused + ")" : ""));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.RELOAD)) return deny(sender);
        reloadAction.run();
        namedTemplates.clear();
        namedTemplates.putAll(namedStore.load());
        sender.sendMessage(ChatColor.GREEN + "Particle engine reloaded.");
        return true;
    }

    private boolean debug(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.DEBUG)) return deny(sender);
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players may toggle debug mode.");
            return true;
        }
        boolean nowEnabled = debugService.toggle(player.getUniqueId());
        sender.sendMessage(nowEnabled ? ChatColor.GREEN + "Debug enabled." : ChatColor.YELLOW + "Debug disabled.");
        return true;
    }

    private boolean save(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SAVE)) return deny(sender);
        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe save <name> <template>");
            return true;
        }
        namedTemplates.put(args[1], args[2]);
        namedStore.save(Map.copyOf(namedTemplates));
        sender.sendMessage(ChatColor.GREEN + "Saved alias '" + args[1] + "' -> " + args[2]);
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.INFO)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe info <template>");
            return true;
        }
        Optional<EffectTemplate> tpl = api.findTemplate(args[1]);
        if (tpl.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Unknown template: " + args[1]);
            return true;
        }
        EffectTemplate t = tpl.get();
        sender.sendMessage(ChatColor.GOLD + "Template: " + ChatColor.WHITE + t.id());
        sender.sendMessage(ChatColor.YELLOW + "  family: " + ChatColor.WHITE + t.family());
        sender.sendMessage(ChatColor.YELLOW + "  particle: " + ChatColor.WHITE + t.particle());
        sender.sendMessage(ChatColor.YELLOW + "  count: " + ChatColor.WHITE + t.count() +
                ChatColor.YELLOW + ", lifetimeTicks: " + ChatColor.WHITE + t.lifetimeTicks());
        sender.sendMessage(ChatColor.YELLOW + "  emitters: " + ChatColor.WHITE + t.emitters().size() +
                ChatColor.YELLOW + ", modifiers: " + ChatColor.WHITE + t.modifiers().size());
        if (t.timeline() != null) {
            sender.sendMessage(ChatColor.YELLOW + "  timeline: " + ChatColor.WHITE +
                    t.timeline().keyframes().size() + " keyframes" +
                    (t.timeline().loop() ? " (looping)" : ""));
        }
        return true;
    }

    private boolean at(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.AT)) return deny(sender);
        if (args.length < 5) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe at <x> <y> <z> <template> [world]");
            return true;
        }
        try {
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            String template = resolveTemplateAlias(args[4]);
            World world;
            if (args.length >= 6) {
                world = Bukkit.getWorld(args[5]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown world: " + args[5]);
                    return true;
                }
            } else if (sender instanceof Player p) {
                world = p.getWorld();
            } else {
                sender.sendMessage(ChatColor.RED + "Specify a world when running from console.");
                return true;
            }
            UUID id = api.spawn(template, new SpawnRequest(null, new Location(world, x, y, z), null, Set.of(), Map.of()));
            sender.sendMessage(ChatColor.GREEN + "Spawned " + template + " at " + x + "," + y + "," + z + " runtime " + id);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Coordinates must be numbers.");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Spawn failed: " + ex.getMessage());
        }
        return true;
    }

    private boolean pause(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.PAUSE)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe pause <runtime-uuid|tag:tagName|all>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int affected = 0;
            for (ActiveEffect effect : api.activeEffects()) {
                if (!effect.isPaused()) {
                    effect.pause();
                    affected++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Paused " + affected + " effect(s).");
            return true;
        }
        if (args[1].startsWith("tag:")) {
            int affected = api.pauseByTag(args[1].substring("tag:".length()));
            sender.sendMessage(ChatColor.GREEN + "Paused " + affected + " effect(s).");
            return true;
        }
        try {
            boolean ok = api.pause(UUID.fromString(args[1]));
            sender.sendMessage(ok ? ChatColor.GREEN + "Paused." : ChatColor.YELLOW + "Runtime ID not found.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID.");
        }
        return true;
    }

    private boolean resume(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.RESUME)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe resume <runtime-uuid|tag:tagName|all>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int affected = 0;
            for (ActiveEffect effect : api.activeEffects()) {
                if (effect.isPaused()) {
                    effect.resume();
                    affected++;
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Resumed " + affected + " effect(s).");
            return true;
        }
        if (args[1].startsWith("tag:")) {
            int affected = api.resumeByTag(args[1].substring("tag:".length()));
            sender.sendMessage(ChatColor.GREEN + "Resumed " + affected + " effect(s).");
            return true;
        }
        try {
            boolean ok = api.resume(UUID.fromString(args[1]));
            sender.sendMessage(ok ? ChatColor.GREEN + "Resumed." : ChatColor.YELLOW + "Runtime ID not found.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid UUID.");
        }
        return true;
    }

    private boolean run(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SCRIPT)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe run <scriptName>");
            return true;
        }
        String name = args[1];
        String source = scriptLoader.find(name);
        if (source == null) {
            sender.sendMessage(ChatColor.RED + "Unknown script '" + name + "'. Use /pe scripts to list available scripts.");
            return true;
        }
        try {
            ScriptRuntime.RunningScript handle = scriptRuntime.run(name, scriptHost, source, error -> {
                if (error == null) {
                    sender.sendMessage(ChatColor.GRAY + "Script '" + name + "' completed.");
                } else if (error instanceof ScriptError) {
                    sender.sendMessage(ChatColor.RED + "Script '" + name + "' errored: " + error.getMessage());
                } else {
                    sender.sendMessage(ChatColor.RED + "Script '" + name + "' crashed: " + error);
                }
            });
            if (handle.isFailed()) {
                sender.sendMessage(ChatColor.RED + "Script failed to parse: " + handle.parseError().getMessage());
            } else {
                sender.sendMessage(ChatColor.GREEN + "Started script '" + name + "' (id " + handle.id() + ")");
            }
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Failed to start script: " + ex.getMessage());
        }
        return true;
    }

    private boolean stopScript(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.SCRIPT)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe stopscript <runId|all>");
            return true;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int n = scriptRuntime.cancelAll();
            sender.sendMessage(ChatColor.GREEN + "Cancelled " + n + " script(s).");
            return true;
        }
        try {
            boolean ok = scriptRuntime.cancel(UUID.fromString(args[1]));
            sender.sendMessage(ok ? ChatColor.GREEN + "Cancelled." : ChatColor.YELLOW + "Run id not found.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Invalid run id.");
        }
        return true;
    }

    private boolean scripts(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.SCRIPT)) return deny(sender);
        sender.sendMessage(ChatColor.AQUA + "Available scripts (" + scriptLoader.scripts().size() + "): " +
                String.join(", ", scriptLoader.scripts().keySet()));
        List<ScriptRuntime.RunningScript> running = scriptRuntime.snapshot();
        if (running.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No running scripts.");
        } else {
            sender.sendMessage(ChatColor.AQUA + "Running:");
            for (ScriptRuntime.RunningScript handle : running) {
                sender.sendMessage(ChatColor.GRAY + "  " + handle.id() + " - " + handle.name());
            }
        }
        return true;
    }

    private String resolveTemplateAlias(String requested) {
        return requested.startsWith("@") ? namedTemplates.getOrDefault(requested.substring(1), "") : requested;
    }

    private boolean deny(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission.");
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "ParticleEngine Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/pe help");
        sender.sendMessage(ChatColor.YELLOW + "/pe spawn <template|@saved>");
        sender.sendMessage(ChatColor.YELLOW + "/pe at <x> <y> <z> <template> [world]");
        sender.sendMessage(ChatColor.YELLOW + "/pe stop <runtimeId|tag:name|all>");
        sender.sendMessage(ChatColor.YELLOW + "/pe pause <runtimeId|tag:name|all>");
        sender.sendMessage(ChatColor.YELLOW + "/pe resume <runtimeId|tag:name|all>");
        sender.sendMessage(ChatColor.YELLOW + "/pe info <template>");
        sender.sendMessage(ChatColor.YELLOW + "/pe list");
        sender.sendMessage(ChatColor.YELLOW + "/pe reload");
        sender.sendMessage(ChatColor.YELLOW + "/pe debug");
        sender.sendMessage(ChatColor.YELLOW + "/pe save <alias> <template>");
        sender.sendMessage(ChatColor.YELLOW + "/pe run <script>   /pe scripts   /pe stopscript <id|all>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "spawn", "info" -> {
                    List<String> options = new ArrayList<>();
                    api.templates().forEach(t -> options.add(t.id()));
                    namedTemplates.keySet().forEach(k -> options.add("@" + k));
                    return options.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                }
                case "run", "stopscript" -> {
                    return new ArrayList<>(scriptLoader.scripts().keySet()).stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .toList();
                }
                case "stop", "pause", "resume" -> {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    api.activeEffects().forEach(eff -> {
                        eff.tags().forEach(tag -> options.add("tag:" + tag));
                    });
                    return options.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
                }
                default -> {
                    return List.of();
                }
            }
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("at")) {
            List<String> options = new ArrayList<>();
            api.templates().forEach(t -> options.add(t.id()));
            namedTemplates.keySet().forEach(k -> options.add("@" + k));
            return options.stream().filter(s -> s.startsWith(args[4].toLowerCase())).toList();
        }
        return List.of();
    }
}
