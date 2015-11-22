package com.github.smiley43210.edibleeggs;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

	private Main plugin;

	public CommandHandler(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length != 1) {
			showHelp(sender);
			return true;
		}

		String subCommand = args[0].toLowerCase();

		if (subCommand.equals("reload")) {
			if (!hasPermission(sender, subCommand)) {
				sender.sendMessage(ChatColor.RED + "You do not have sufficient permissions to do that!");
				return true;
			}

			if (!plugin.reloadCustomConfig()) {
				sender.sendMessage(ChatColor.RED
						+ "Plugin configuration failed to reload! Check the console for the stack trace!");
				return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Plugin configuration reloaded!");
		} else {
			showHelp(sender);
		}
		return true;
	}

	private void showHelp(CommandSender sender) {

		sender.sendMessage(ChatColor.GREEN + "--------------------EdibleEggs Help--------------------");
		sender.sendMessage(
				ChatColor.GOLD + "/edibleeggs reload" + ChatColor.RESET + " - Reloads the configuration file");
	}

	private boolean hasPermission(CommandSender sender, String subCommand) {
		if (sender instanceof Player) {
			return sender.hasPermission("edibleeggs." + subCommand);
		}
		return true;
	}

}