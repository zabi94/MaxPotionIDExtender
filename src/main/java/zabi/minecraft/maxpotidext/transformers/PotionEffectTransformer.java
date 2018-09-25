package zabi.minecraft.maxpotidext.transformers;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.nbt.NBTTagCompound;
import zabi.minecraft.maxpotidext.ASMException;
import zabi.minecraft.maxpotidext.Log;

public class PotionEffectTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!transformedName.equals("net.minecraft.potion.PotionEffect")) {
			return basicClass;
		}
		
		Log.i("Transforming PotionEffect");
		
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		String tcompDesc = "L"+Type.getInternalName(NBTTagCompound.class)+";";
		String desc = "("+tcompDesc+")"+tcompDesc;
		
		MethodNode mn = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc) && (n.name.equals("writeCustomPotionEffectToNBT") || n.name.equals("func_82719_a")))
				.findAny().orElseThrow(() -> new ASMException("writeCustomPotionEffectToNBT for PotionEffect not found"));
		
		AbstractInsnNode ant = null;
		Iterator<AbstractInsnNode> i = mn.instructions.iterator();
		while (i.hasNext() && ant==null) {
			AbstractInsnNode node = i.next();
			if (node.getOpcode() == Opcodes.I2B) {
				ant = node;
			}
		}
		if (ant==null) {
			throw new ASMException("No target instruction found for writeCustomPotionEffectToNBT");
		}
		String mname = (name==transformedName?"setInteger":"func_74768_a");
		
		MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(NBTTagCompound.class), mname, "(Ljava/lang/String;I)V", false);
		mn.instructions.remove(ant.getNext());
		mn.instructions.insert(ant, call);
		mn.instructions.remove(ant);
		
		String desc2 = "("+tcompDesc+")L"+name.replace('.', '/')+";";
		
		MethodNode mn2 = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc2) && (n.name.equals("readCustomPotionEffectFromNBT") || n.name.equals("func_82722_b")))
				.findAny().orElseThrow(() -> new ASMException("readCustomPotionEffectFromNBT for PotionEffect not found"));
		
		AbstractInsnNode ant2 = null;
		Iterator<AbstractInsnNode> i2 = mn2.instructions.iterator();
		while (i2.hasNext() && ant2==null) {
			AbstractInsnNode node = i2.next();
			if (node.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				ant2 = node;
			}
		}
		if (ant2==null) {
			throw new ASMException("No target instruction found for readCustomPotionEffectFromNBT");
		}
		
		String name2 = (name.equals(transformedName)?"getInteger":"func_74762_e");
		
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.insert(ant2, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(NBTTagCompound.class), name2, "(Ljava/lang/String;)I", false));
		mn2.instructions.remove(ant2);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

}
