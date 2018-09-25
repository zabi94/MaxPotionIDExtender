package zabi.minecraft.maxpotidext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {
	public static final Logger logger = LogManager.getLogger(MaxPotionIDExtender.MOD_ID);

	public static void i(CharSequence msg) {
		logger.info(msg);
	}
	
	public static void w(CharSequence msg) {
		logger.warn(msg);
	}
	
	public static void e(CharSequence msg) {
		logger.error(msg);
	}
	
	public static void d(CharSequence msg) {
		logger.debug(msg);
	}
	
}
