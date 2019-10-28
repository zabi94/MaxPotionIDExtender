package zabi.minecraft.maxpotidext;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.ForgeRegistry;

@Mod(modid=MaxPotionIDExtender.MOD_ID, name=MaxPotionIDExtender.NAME, version=MaxPotionIDExtender.VERSION, certificateFingerprint = MaxPotionIDExtender.FINGERPRINT)
public class MaxPotionIDExtender {
	
	public static final String MOD_ID = "maxpotidext";
	public static final String NAME = "MaxPotionIDExtender";
	public static final String VERSION = "@VERSION@";
	public static final String MC_VERSION = "[1.12.2,1.12.2]";
	public static final String FINGERPRINT = "@FINGERPRINT@";
	
	@Instance
	public static MaxPotionIDExtender INSTANCE;
	
	private static Field fieldMax = ReflectionHelper.findField(ForgeRegistry.class, "max");
	
	@EventHandler
	public void init(FMLPreInitializationEvent evt) {
		try {
			int maxPotions = fieldMax.getInt(ForgeRegistries.POTIONS);
			if (maxPotions<256) {
				throw new ASMException("Limit is unchanged, id extension failed. Limit is: "+maxPotions);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@EventHandler
	public void certInvalid(FMLFingerprintViolationEvent evt) {
		Log.w("\n\n!!! WARNING:\nThe signature for the mod MaxPotionIDExtender is invalid.\nPlease make sure to download only from the official source to avoid malware, go to https://minecraft.curseforge.com/projects/max-potion-id-extender\n\n");
	}
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent evt) {
		if (evt.getModID().equals(MOD_ID)) {
			ConfigManager.sync(MOD_ID, Type.INSTANCE);
		}
	}
	
	@SubscribeEvent
	public void registryPotion(RegistryEvent.Register<Potion> evt) {
		if (ModConfig.generateTestPotions>0) {
			Log.i("Generating test potions");
		}
		for (int i=0;i<ModConfig.generateTestPotions;i++) {
			Potion p = new PotionTest(i);
			p.setRegistryName(new ResourceLocation(MOD_ID, "TestPotion"+i));
			evt.getRegistry().register(p);
		}
	}
	
	@SubscribeEvent
	public void registryPotionType(RegistryEvent.Register<PotionType> evt) {
		if (ModConfig.generateTestPotions>0) {
			Log.i("Generating test potion types");
		}
		for (int i=0;i<ModConfig.generateTestPotions;i++) {
			PotionType pt = new PotionType(new PotionEffect(Potion.REGISTRY.getObject(new ResourceLocation(MOD_ID, "TestPotion"+i)), 2000, 0, false, true));
			pt.setRegistryName(new ResourceLocation(MOD_ID, "TestPotionType"+i));
			evt.getRegistry().register(pt);
		}
	}
	
	public static class PotionTest extends Potion {

		private static final Random r = new Random();
		private String nm = "";
		
		protected PotionTest(int id) {
			super(false, 0xFFFFFF & r.nextInt(Integer.MAX_VALUE));
			nm = "Test Potion #"+id;
		}
		
		@Override
		public String getName() {
			return nm;
		}
	}
}
