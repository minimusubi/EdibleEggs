package com.github.smiley43210.edibleeggs;

import org.bukkit.potion.PotionEffectType;

public class EggEffect {

	private PotionEffectType effect;
	private int chance;
	private int amplifier;
	private int duration;

	public EggEffect(PotionEffectType effect, int chance, int amplifier, int duration) {
		this.effect = effect;
		this.chance = chance;
		this.amplifier = amplifier;
		this.duration = duration;
	}

	public PotionEffectType getEffect() {
		return effect;
	}

	public int getChance() {
		return chance;
	}

	public int getAmplifier() {
		return amplifier;
	}

	public int getDuration() {
		return duration;
	}
}
