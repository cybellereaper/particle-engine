package com.github.cybellereaper.particleengine.command;

import com.github.cybellereaper.particleengine.api.ParticleEngineApi;
import com.github.cybellereaper.particleengine.api.SpawnRequest;
import com.github.cybellereaper.particleengine.debug.DebugService;
import com.github.cybellereaper.particleengine.persistence.NamedEffectStore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class ParticleEngineCommand implements CommandExecutor, TabCompleter {
    private final ParticleEngineApi api;
    private final DebugService debugService;
    private final Runnable reloadAction;
    private final NamedEffectStore namedStore;
    private final Map<String, String> namedTemplates = new HashMap<>();

    public ParticleEngineCommand(ParticleEngineApi api, DebugService debugService, Runnable reloadAction, NamedEffectStore namedStore) {
        this.api = api;
        this.debugService = debugService;
        this.reloadAction = reloadAction;
        this.namedStore = namedStore;
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
        String requested = args[1];
        String template = requested.startsWith("@") ? namedTemplates.getOrDefault(requested.substring(1), "") : requested;
        if (template.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Unknown saved effect alias: " + requested);
            return true;
        }
        try {
            UUID id = api.spawn(template, SpawnRequest.at(player.getLocation()));
            sender.sendMessage(ChatColor.GREEN + "Spawned effect " + template + " with runtime id " + id);
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + "Spawn failed: " + ex.getMessage());
        }
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionNodes.STOP)) return deny(sender);
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /pe stop <runtime-uuid|tag:tagName>");
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
        sender.sendMessage(ChatColor.AQUA + "Active runtime effects: " + api.activeEffects().size());
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(PermissionNodes.RELOAD)) return deny(sender);
        reloadAction.run();
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

    private boolean deny(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission.");
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "ParticleEngine Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/pe help");
        sender.sendMessage(ChatColor.YELLOW + "/pe spawn <template|@saved>");
        sender.sendMessage(ChatColor.YELLOW + "/pe stop <runtimeId|tag:name>");
        sender.sendMessage(ChatColor.YELLOW + "/pe list");
        sender.sendMessage(ChatColor.YELLOW + "/pe reload");
        sender.sendMessage(ChatColor.YELLOW + "/pe debug");
        sender.sendMessage(ChatColor.YELLOW + "/pe save <alias> <template>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "spawn", "stop", "list", "reload", "debug", "save").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> options = new ArrayList<>();
            api.templates().forEach(t -> options.add(t.id()));
            namedTemplates.keySet().forEach(k -> options.add("@" + k));
            return options.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
