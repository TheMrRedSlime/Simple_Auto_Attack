package org.simpleautoattack.SimpleAutoAttack.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "simple_auto_attack")
public class AutoAttackConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean alwaysAttack = false;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean preventBlockBreaking = true;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean bypass = false;

} 