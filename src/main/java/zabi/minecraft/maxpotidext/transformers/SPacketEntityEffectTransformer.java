package zabi.minecraft.maxpotidext.transformers;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.PotionEffect;
import zabi.minecraft.maxpotidext.ASMException;
import zabi.minecraft.maxpotidext.CodeSnippets;
import zabi.minecraft.maxpotidext.Log;

public class SPacketEntityEffectTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (!transformedName.equals("net.minecraft.network.play.server.SPacketEntityEffect")) {
			return basicClass;
		}
		
		Log.i("Transforming SPacketEntityEffect");
		
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		//Adding a new field, int effectInt
		cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "effectInt", "I", null, 0));
		
		//Initialize this field in the constructor
		String desc_init = "(IL"+Type.getInternalName(PotionEffect.class)+";)V";
		MethodNode mn_init = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc_init) && n.name.equals("<init>"))
				.findAny().orElseThrow(() -> new ASMException("Constructor for SPacketEntityEffect not found"));
		
		
		
		Iterator<AbstractInsnNode> i = mn_init.instructions.iterator();
		AbstractInsnNode targetNode = null;
		int line = 0;
		while (i.hasNext() && targetNode == null) {
			AbstractInsnNode node = i.next();
			if (node instanceof LineNumberNode) {
				if (line == 1) {
					targetNode = node;
				}
				line++;
			}
		}
		
		if (targetNode == null) {
			throw new ASMException("Can't find target node for SPacketEntityEffect constructor");
		}
		
		//These are reversed, they get pushed down the stack
		mn_init.instructions.insert(targetNode, new FieldInsnNode(Opcodes.PUTFIELD, name.replace('.', '/'), "effectInt", "I"));
		mn_init.instructions.insert(targetNode, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(CodeSnippets.class), "getIdFromPotEffect", "(L"+Type.getInternalName(PotionEffect.class)+";)I", false));
		mn_init.instructions.insert(targetNode, new VarInsnNode(Opcodes.ALOAD, 2));
		mn_init.instructions.insert(targetNode, new VarInsnNode(Opcodes.ALOAD, 0));
		
		Log.i("SPacketEntityEffect#<init> patched");


		//Patch readPacketData
		String desc_readPacket = "(L"+Type.getInternalName(PacketBuffer.class)+";)V";
		MethodNode mn_readPacket = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc_readPacket) && (n.name.equals("readPacketData") || n.name.equals("func_148837_a")))
				.findAny().orElseThrow(() -> new ASMException("readPacketData for SPacketEntityEffect not found"));
		InsnList current = new InsnList();
		String readVarInt_name = (name.equals(transformedName)?"readvarInt":"func_150792_a");
		current.add(new VarInsnNode(Opcodes.ALOAD, 0));
		current.add(new VarInsnNode(Opcodes.ALOAD, 1));
		current.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), readVarInt_name, "()I", false));
		current.add(new FieldInsnNode(Opcodes.PUTFIELD, name.replace(".", "/"), "entityInt", "I"));
		i = mn_readPacket.instructions.iterator();
		i.next(); i.next();
		while (i.hasNext()) {
			current.add(i.next());
		}
		current.insertBefore(current.getFirst(), mn_readPacket.instructions.get(1));
		current.insertBefore(current.getFirst(), mn_readPacket.instructions.get(0));
		
		Log.i("SPacketEntityEffect#readPacketData patched");



		//Patch writePacketData
		String desc_writePacket = "(L"+Type.getInternalName(PacketBuffer.class)+";)V";
		MethodNode mn_writePacket = cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc_writePacket) && (n.name.equals("writePacketData") || n.name.equals("func_148840_b")))
				.findAny().orElseThrow(() -> new ASMException("writePacketData for SPacketEntityEffect not found"));
		String writeVarInt_name = (name.equals(transformedName)?"writeVarInt":"func_150787_b");
		current = new InsnList();
		current.add(new VarInsnNode(Opcodes.ALOAD, 1));
		current.add(new VarInsnNode(Opcodes.ALOAD, 0));
		current.add(new FieldInsnNode(Opcodes.GETFIELD, name.replace('.', '/'), "effectInt", "I"));
		current.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), writeVarInt_name, "(I)L"+Type.getInternalName(PacketBuffer.class)+";", false));
		current.add(new InsnNode(Opcodes.POP));
		i = mn_writePacket.instructions.iterator();
		i.next(); i.next();
		while (i.hasNext()) {
			current.add(i.next());
		}
		
		current.insertBefore(current.getFirst(), mn_writePacket.instructions.get(1));
		current.insertBefore(current.getFirst(), mn_writePacket.instructions.get(0));
		
		
		Log.i("SPacketEntityEffect#writePacketData patched");
		
		Log.i("SPacketEntityEffect - patch complete");

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}


}
