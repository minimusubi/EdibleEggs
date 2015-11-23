package com.github.smiley43210.edibleeggs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.yaml.snakeyaml.scanner.ScannerException;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public class Main extends JavaPlugin {

	private final int UPDATE_INTERVAL = 60 * 60 * 24;

	private static Configuration configuration;
	private BukkitScheduler scheduler;
	private HashMap<Integer, HashMap<String, Integer>> rawEggEffects;
	private ArrayList<Integer> rawEggChanceList;

	@Override
	public void onEnable() {
		configuration = getConfig();
		FileConfigurationOptions options = (FileConfigurationOptions) configuration.options();
		options.copyDefaults(true);
		// options.copyHeader();
		saveConfig();

		scheduler = Bukkit.getScheduler();
		scheduleUpdates();

		getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);

		parseEffects();

		getCommand("edibleeggs").setExecutor(new CommandHandler(this));

		ItemStack roastedEgg = new ItemStack(Material.EGG);
		ItemMeta roastedEggMeta = roastedEgg.getItemMeta();
		roastedEggMeta.setDisplayName("Roasted Egg");
		roastedEgg.setItemMeta(roastedEggMeta);
		net.minecraft.server.v1_8_R3.ItemStack roastedEggNMS = CraftItemStack.asNMSCopy(roastedEgg);
		NBTTagCompound tag = roastedEggNMS.getTag();
		if (tag == null) {
			tag = new NBTTagCompound();
		}
		tag.setBoolean("Roasted", true);
		roastedEggNMS.setTag(tag);
		roastedEgg = CraftItemStack.asBukkitCopy(roastedEggNMS);
		FurnaceRecipe furnaceRecipe = new FurnaceRecipe(roastedEgg, Material.EGG);
		getServer().addRecipe(furnaceRecipe);
	}

	@Override
	public void onDisable() {
	}

	private void scheduleUpdates() {
		scheduler.cancelTasks(this);

		if (getConfig().getBoolean("notifyOnAvailableUpdate")) {
			scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
				@Override
				public void run() {
					scheduleUpdate(1);
				}
			}, 20L, UPDATE_INTERVAL * 20L);
		}
	}

	private void scheduleUpdate(int delay) {
		Main plugin = this;
		scheduler.scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				try {
					UpdateChecker updateChecker = new UpdateChecker(plugin);
					if (updateChecker.hasUpdate()) {
						plugin.getLogger()
								.warning("An update (v" + updateChecker.getNewVersion() + ") is now available!");
						plugin.getLogger().warning("Download it from http://dev.bukkit.org/bukkit-plugins/edibleeggs/");
					}
				} catch (UpdateCheckException e) {
					plugin.getLogger().warning("Unable to check for updates! Will try again later.");
					scheduleUpdate(60 * 5);
				}
			}
		}, delay * 20L);
	}

	private void parseEffects() {
		rawEggEffects = new HashMap<Integer, HashMap<String, Integer>>();
		rawEggChanceList = new ArrayList<Integer>();
		int totalChance = 0;

		Set<String> effectList = configuration.getConfigurationSection("rawEggEffects").getKeys(false);
		for (String effect : effectList) {
			HashMap<String, Integer> effectMap = new HashMap<String, Integer>();
			ConfigurationSection effectSection = configuration.getConfigurationSection("rawEggEffects")
					.getConfigurationSection(effect);

			effectMap.put("id", Integer.parseInt(effect));

			if (effectSection.getString("chance") == null) {
				throw new InvalidEffectConfigurationException(
						"The effect " + effectSection.getName() + " is missing \"chance: \"!");
			} else if (effectSection.getInt("chance") < 1) {
				throw new InvalidEffectConfigurationException(
						"The chance for effect " + effectSection.getName() + " is less than 1!");
			} else {
				totalChance += effectSection.getInt("chance");
			}

			if (effectSection.getString("amplifier") == null) {
				throw new InvalidEffectConfigurationException(
						"The effect " + effectSection.getName() + " is missing \"amplifier: \"!");
			} else {
				effectMap.put("amplifier", effectSection.getInt("amplifier"));
			}

			if (effectSection.getString("duration") == null) {
				throw new InvalidEffectConfigurationException(
						"The effect " + effectSection.getName() + " is missing \"duration: \"!");
			} else {
				effectMap.put("duration", effectSection.getInt("duration"));
			}

			rawEggEffects.put(totalChance, effectMap);
			rawEggChanceList.add(totalChance);
		}

		Collections.sort(rawEggChanceList);

		if (totalChance > 100) {
			throw new ChanceBoundsException("The total chances for the effects exceed 100! (" + totalChance + ")");
		}
	}

	public HashMap<Integer, HashMap<String, Integer>> getRawEggEffects() {
		return rawEggEffects;
	}

	public ArrayList<Integer> getRawEggChances() {
		return rawEggChanceList;
	}

	public boolean reloadCustomConfig() {
		try {
			reloadConfig();
			configuration = getConfig();
			FileConfigurationOptions options = (FileConfigurationOptions) configuration.options();
			options.copyDefaults(true);
			// options.copyHeader();
			saveConfig();
			scheduleUpdates();
			parseEffects();
		} catch (ScannerException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
