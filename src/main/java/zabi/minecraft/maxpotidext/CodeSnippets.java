package zabi.minecraft.maxpotidext;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class CodeSnippets {
	
	public static int getIdFromPotEffect(PotionEffect pe) {
		return Potion.getIdFromPotion(pe.getPotion());
	}
	
}
