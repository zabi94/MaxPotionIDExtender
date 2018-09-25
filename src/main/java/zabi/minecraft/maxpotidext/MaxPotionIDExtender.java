package zabi.minecraft.maxpotidext;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(clientSideOnly=false, modid=MaxPotionIDExtender.MOD_ID, name=MaxPotionIDExtender.NAME, version=MaxPotionIDExtender.VERSION)
public class MaxPotionIDExtender {
	public static final String MOD_ID = "maxpotidext";
	public static final String NAME = "MaxPotionIDExtender";
	public static final String VERSION = "1.0";
	public static final String MC_VERSION = "[1.8,1.12.2]";
	
	@EventHandler
	public void init(FMLPreInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void registryPotion(RegistryEvent.Register<Potion> evt) {
		Log.i("Registering 3000 to 3300 potion and types");
		for (int i=0;i<300;i++) {
			Potion p = new Potion(false, i) {};
			p.setRegistryName(new ResourceLocation(MOD_ID, "TestPotion"+i));
			evt.getRegistry().register(p);
		}
	}
	
	@SubscribeEvent
	public void registryPotionType(RegistryEvent.Register<PotionType> evt) {
		Log.i("Registering 3000 to 3300 potion and types");
		for (int i=0;i<300;i++) {
			PotionType pt = new PotionType(new PotionEffect(Potion.REGISTRY.getObject(new ResourceLocation(MOD_ID, "TestPotion"+i)), 200, 1, true, true));
			pt.setRegistryName(new ResourceLocation(MOD_ID, "TestPotionType"+i));
			evt.getRegistry().register(pt);
		}
	}
}
