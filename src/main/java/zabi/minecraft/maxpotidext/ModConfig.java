package zabi.minecraft.maxpotidext;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.RequiresMcRestart;

@Config(modid = MaxPotionIDExtender.MOD_ID)
public class ModConfig {
	@RequiresMcRestart
	@Config.Comment("Set how many test potions should be generated")
	@Config.RangeInt(min = 0, max = Integer.MAX_VALUE>>5)
	@Config.Name("Test Potions")
	public static int generateTestPotions = 2000;
}
