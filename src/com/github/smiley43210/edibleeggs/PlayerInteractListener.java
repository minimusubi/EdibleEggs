package com.github.smiley43210.edibleeggs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public class PlayerInteractListener implements Listener {

	private Main plugin;
	private Random random;

	public PlayerInteractListener(Main plugin) {
		this.plugin = plugin;
		random = new Random();
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void playerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (player.isSneaking() && event.hasItem() && event.getItem().getType() == Material.EGG) {
				if (player.getFoodLevel() < 20 && player.hasPermission("edibleeggs.eat")) {
					ItemStack itemStack = player.getItemInHand();

					if (!isRoasted(itemStack)) {
						HashMap<Integer, HashMap<String, Integer>> effectsMap = plugin.getRawEggEffects();
						ArrayList<Integer> chanceList = plugin.getRawEggChances();

						player.setFoodLevel(
								Math.min(player.getFoodLevel() + plugin.getConfig().getInt("rawEggRestoreHunger"), 20));
						player.setSaturation((float) Math.min(
								player.getSaturation() + plugin.getConfig().getDouble("rawEggRestoreSaturation"),
								player.getFoodLevel()));

						int effectChance = random.nextInt(100) + 1;
						int resultChance = 0;

						// plugin.getLogger().warning(Arrays.toString(chanceList.toArray()));
						// plugin.getLogger().warning("effectChance:" +
						// effectChance);

						for (int i = chanceList.size() - 1; i >= 0; i--) {
							int chance = chanceList.get(i);
							if (effectChance <= chance) {
								resultChance = chance;
							}
						}
						// plugin.getLogger().warning("resultChance:" +
						// resultChance);
						if (effectsMap.containsKey(resultChance)) {
							HashMap<String, Integer> effectMap = effectsMap.get(resultChance);
							player.addPotionEffect(new PotionEffect(PotionEffectType.getById(effectMap.get("id")),
									effectMap.get("duration") * 20, effectMap.get("amplifier") - 1), true);
						}
					} else {
						player.setFoodLevel(Math
								.min(player.getFoodLevel() + plugin.getConfig().getInt("roastedEggRestoreHunger"), 20));
						player.setSaturation((float) Math.min(
								player.getSaturation() + plugin.getConfig().getDouble("roastedEggRestoreSaturation"),
								player.getFoodLevel()));
					}

					int amount = itemStack.getAmount();
					if (amount > 1) {
						itemStack.setAmount(amount - 1);
					} else {
						player.setItemInHand(new ItemStack(Material.AIR));
					}
				}
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.hasItem() && event.getItem().getType() == Material.EGG) {
				if (isRoasted(player.getItemInHand())) {
					event.setCancelled(true);
				}
			}
		}
	}

	private boolean isRoasted(ItemStack itemStack) {
		net.minecraft.server.v1_8_R3.ItemStack itemStackNMS = CraftItemStack.asNMSCopy(itemStack);
		if (itemStackNMS.hasTag()) {
			NBTTagCompound tag = itemStackNMS.getTag();
			if (tag.hasKey("Roasted") && tag.getBoolean("Roasted")) {
				return true;
			}
		}
		return false;
	}

}
