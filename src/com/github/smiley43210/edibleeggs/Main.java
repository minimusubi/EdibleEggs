package com.github.smiley43210.edibleeggs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.craftbukkit.v1_9_R1.inventory.CraftItemStack;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.yaml.snakeyaml.scanner.ScannerException;
import net.minecraft.server.v1_9_R1.NBTTagCompound;

public class Main extends JavaPlugin {

	private final int UPDATE_INTERVAL = 60 * 60 * 24;
	private final String RAW_EFFECT_SECTION = "rawEggEffects";

	private static Configuration configuration;
	private BukkitScheduler scheduler;
	private HashMap<Integer, EggEffect> rawEggEffects;
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
		net.minecraft.server.v1_9_R1.ItemStack roastedEggNMS = CraftItemStack.asNMSCopy(roastedEgg);
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
						plugin.getLogger().warning("An update (current: v" + plugin.getDescription().getVersion()
								+ ", new: v" + updateChecker.getNewVersion() + ") is now available!");
						plugin.getLogger().warning("Download it from http://dev.bukkit.org/bukkit-plugins/edibleeggs/");
					}
				} catch (UpdateCheckException e) {
					plugin.getLogger().warning("Unable to check for updates! Will try again later.");
					scheduleUpdate(60 * 5);
				}
			}
		}, delay * 20L);
	}

	@SuppressWarnings("deprecation")
	private void parseEffects() {
		rawEggEffects = new HashMap<Integer, EggEffect>();
		rawEggChanceList = new ArrayList<Integer>();
		int totalChance = 0;

		Set<String> effectList = configuration.getConfigurationSection(RAW_EFFECT_SECTION).getKeys(false);
		for (String effect : effectList) {
			ConfigurationSection effectSection = configuration.getConfigurationSection(RAW_EFFECT_SECTION)
					.getConfigurationSection(effect);

			try {
				int effectID = Integer.parseInt(effect);
				if (PotionEffectType.getById(effectID) == null) {
					getLogger().warning(
							"The raw egg effect \"" + effect + "\" is not a valid effect ID! Skipping effect.");
					continue;
				}

				String effectName = PotionEffectType.getById(effectID).getName().toLowerCase();

				Map<String, Object> effectValues = effectSection.getValues(true);
				configuration.getConfigurationSection(RAW_EFFECT_SECTION).set(effect, null);
				effectSection = configuration.getConfigurationSection(RAW_EFFECT_SECTION).createSection(effectName,
						effectValues);

				getLogger().info("Converted raw egg effect ID \"" + effect + "\" to \"" + effectName + "\".");

				effect = effectName;
			} catch (NumberFormatException e) {
				if (PotionEffectType.getByName(effect) == null) {
					getLogger().warning(
							"The raw egg effect \"" + effect + "\" is not a valid effect name! Skipping effect.");
				}
			}

			if (effectSection.getString("chance") == null) {
				getLogger().warning("The effect " + effect + " is missing \"chance: \"! Skipping effect.");
			} else if (effectSection.getInt("chance") < 1) {
				getLogger().warning("The chance for effect " + effect + " is less than 1! Skipping effect.");
			}

			if (effectSection.getString("amplifier") == null) {
				getLogger().warning("The effect " + effect + " is missing \"amplifier: \"! Skipping effect.");
			}

			if (effectSection.getString("duration") == null) {
				getLogger().warning("The effect " + effect + " is missing \"duration: \"! Skipping effect.");
			}

			totalChance += effectSection.getInt("chance");
			rawEggEffects.put(totalChance,
					new EggEffect(PotionEffectType.getByName(effect), effectSection.getInt("chance"),
							effectSection.getInt("amplifier"), effectSection.getInt("duration")));
			rawEggChanceList.add(totalChance);
		}

		Collections.sort(rawEggChanceList);

		if (totalChance > 100) {
			throw new ChanceBoundsException("The total chances for the effects exceed 100%! (" + totalChance + "%)");
		}

		saveConfig();
	}

	public HashMap<Integer, EggEffect> getRawEggEffects() {
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
