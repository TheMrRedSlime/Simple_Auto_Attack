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
    public boolean limit = false;

<<<<<<< HEAD
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int limitms = 500;

} 
=======
} 
>>>>>>> 378f41d0a6769282ad868f85efabda5a47e624cd
