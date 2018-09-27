package zabi.minecraft.maxpotidext;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("Max Potion ID Extender")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"zabi.minecraft.maxpotidext"})
public class IDExtenderPlugin implements IFMLLoadingPlugin {
	
	private static final String[] TRANSFORMERS = {
			"zabi.minecraft.maxpotidext.MPIDTransformer"
	};

	@Override
	public String[] getASMTransformerClass() {
		Obf.loadData();
		return TRANSFORMERS;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
