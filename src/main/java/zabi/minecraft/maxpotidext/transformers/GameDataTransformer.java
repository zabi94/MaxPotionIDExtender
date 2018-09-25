package zabi.minecraft.maxpotidext.transformers;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.potion.Potion;
import zabi.minecraft.maxpotidext.ASMException;
import zabi.minecraft.maxpotidext.Log;

public class GameDataTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!transformedName.equals("net.minecraftforge.registries.GameData")) {
			return basicClass;
		}
		
		Log.i("Transforming GameData");

		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		FieldNode mpid = cn.fields.parallelStream()
				.filter(fn -> fn.name.equals("MAX_POTION_ID"))
				.findAny().orElseThrow(() -> new ASMException("Error finding MAX_POTION_ID constant in GameData.class"));

		cn.fields.remove(mpid);
		cn.fields.add(new FieldNode(mpid.access, mpid.name, mpid.desc, mpid.signature, Integer.MAX_VALUE-1));

//		FieldNode mptid = cn.fields.parallelStream()
//				.filter(fn -> fn.name.equals("MAX_POTIONTYPE_ID"))
//				.findAny().orElseThrow(() -> new ASMException("Error finding MAX_POTIONTYPE_ID constant in GameData.class"));
//
//		cn.fields.remove(mptid);
//		cn.fields.add(new FieldNode(mptid.access, mptid.name, mptid.desc, mptid.signature, Integer.MAX_VALUE));
		
		MethodNode mn = cn.methods.parallelStream()
				.filter(n -> n.name.equals("init") && n.desc.equals("()V"))
				.findAny().orElseThrow(() -> new ASMException("Can't find target init method in GameData.class"));
		
		AbstractInsnNode target = null;

		Iterator<AbstractInsnNode> i = mn.instructions.iterator();
		while (i.hasNext() && target == null) {
			AbstractInsnNode ninst = i.next();
			if (ninst.getOpcode()==Opcodes.SIPUSH) {
				if (ninst.getPrevious().getOpcode()==Opcodes.LDC) {
					LdcInsnNode ldc = (LdcInsnNode) ninst.getPrevious();
					if (ldc.cst.toString().equals("L"+Type.getInternalName(Potion.class)+";")) {
						target = ninst;
					}
				}
			}
		}
		
		if (target==null) {
			throw new ASMException("Couldn't find target instruction for GameData L"+Type.getInternalName(Potion.class)+";");
		}
		
		mn.instructions.insert(target, new LdcInsnNode(Integer.MAX_VALUE-1));
		mn.instructions.remove(target);

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

}
