package zabi.minecraft.maxpotidext;

public class ASMException extends RuntimeException {
	private static final long serialVersionUID = -8581611883691404427L;
	
	public ASMException(String message) {
		super("MaxPotionIDExtender - Class transformation error\n"+message);
	}
}
