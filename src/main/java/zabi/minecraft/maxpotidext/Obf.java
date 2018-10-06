package zabi.minecraft.maxpotidext;

import org.objectweb.asm.Type;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.potion.Potion;

public class Obf {
	
	
	public static boolean isPotionClass(String s) {
		if (s.endsWith(";")) {
			s = s.substring(1, s.length()-1);
		}
		return s.equals(Type.getInternalName(Potion.class)) || s.equals("uz");
	}
	
	public static boolean isDeobf() {
		return (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
	}
	
	public static void loadData() {
		if ((boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) {
			NBTTagCompound = "net/minecraft/nbt/NBTTagCompound";     
			PotionEffect ="net/minecraft/potion/PotionEffect";
			SPacketEntityEffect = "net/minecraft/network/play/server/SPacketEntityEffect";
			PacketBuffer = "net/minecraft/network/PacketBuffer";
			ItemStack = "net/minecraft/item/ItemStack";
			World = "net/minecraft/world/World";
			ITooltipFlag = "net/minecraft/client/util/ITooltipFlag";
			Enchantment = "net/minecraft/enchantment/Enchantment";
			EntityPlayer = "net/minecraft/entity/player/EntityPlayer";
		} else {
			NBTTagCompound = "fy";     
			PotionEffect = "va";       
			SPacketEntityEffect = "kw";
			PacketBuffer = "gy";     
			ItemStack = "aip";
			World = "ams";
			ITooltipFlag = "akb";
			Enchantment = "ali";
			EntityPlayer = "aed";
		}
	}
	
	public static String NBTTagCompound;
	public static String PotionEffect;
	public static String SPacketEntityEffect;
	public static String PacketBuffer;
	public static String ItemStack;
	public static String World;
	public static String ITooltipFlag;
	public static String Enchantment;
	public static String EntityPlayer;
}
