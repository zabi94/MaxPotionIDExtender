package zabi.minecraft.maxpotidext.transformers;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.network.play.server.SPacketEntityEffect;
import zabi.minecraft.maxpotidext.ASMException;
import zabi.minecraft.maxpotidext.Log;

public class NetHandlerPlayTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!transformedName.equals("net.minecraft.client.network.NetHandlerPlayClient")) {
			return basicClass;
		}
		
		Log.i("Transforming NetHandlerPlayClient");
		
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		String desc = "(L"+Type.getInternalName(SPacketEntityEffect.class)+";)V";
		MethodNode mn = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc) && (n.name.equals("handleEntityEffect") || n.name.equals("func_147260_a")))
				.findAny().orElseThrow(() -> new ASMException("handleEntityEffect cannot be found in NetHandlerPlayClient"));
		
		AbstractInsnNode target = null;
		Iterator<AbstractInsnNode> i = mn.instructions.iterator();
		while (i.hasNext() && target == null) {
			AbstractInsnNode n = i.next();
			if (n.getOpcode()==Opcodes.SIPUSH) {
				target = n;
			}
		}
		if (target==null) {
			throw new ASMException("Can't locate target instruction SIPUSH in NetHandlerPlay");
		}
		
		mn.instructions.remove(target.getPrevious());
		mn.instructions.remove(target.getNext());
		mn.instructions.insertBefore(target, new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(SPacketEntityEffect.class), "effectInt", "I"));
//		mn.instructions.insertBefore(target, new InsnNode(Opcodes.DUP));
//		mn.instructions.insertBefore(target, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(CodeSnippets.class), "printIDReceived", "(I)V", false));
		mn.instructions.remove(target);
		
		Log.i("NetHandlerPlay patch complete");
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

}
