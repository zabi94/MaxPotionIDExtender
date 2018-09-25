package zabi.minecraft.maxpotidext;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@Mod(clientSideOnly=false, modid=MaxPotionIDExtender.MOD_ID, name=MaxPotionIDExtender.NAME, version=MaxPotionIDExtender.VERSION)
public class MaxPotionIDExtender {
	public static final String MOD_ID = "maxpotidext";
	public static final String NAME = "MaxPotionIDExtender";
	public static final String VERSION = "1.0";
	public static final String MC_VERSION = "[1.8,1.12.2]";
	
	@EventHandler
	public void init(FMLPreInitializationEvent evt) {
		Object o = new SPacketEntityEffect();
		if (ReflectionHelper.findField(SPacketEntityEffect.class, "effectInt") == null) {
			throw new ASMException("ASM not applied: SPacketEntityEffectInit");
		}
		
		NetHandlerPlayClient.class.getName();
	}
}
