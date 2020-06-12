package dev.hephaestus.landmark.impl;

import me.sargunvohra.mcmods.autoconfig1u.ConfigData;
import me.sargunvohra.mcmods.autoconfig1u.annotation.Config;

@Config(name = "landmark")
public class LandmarkConfig implements ConfigData {
	public float namePopupDuration = 3.5F;
	public float namePopupFadeIn = 0.5F;
	public float namePopupFadeOut = 1F;
	public float namePopupScale = 1F;
}