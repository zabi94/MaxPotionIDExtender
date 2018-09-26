package zabi.minecraft.maxpotidext;

import java.util.Iterator;
import java.util.function.Predicate;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public class MPIDTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (transformedName.equals("net.minecraft.client.network.NetHandlerPlayClient")) {
			return transformNetHandlerPlayClient(basicClass);
		} 
		if (transformedName.equals("net.minecraft.potion.PotionEffect")) {
			return transformPotionEffect(basicClass, name, transformedName);
		}
		if (transformedName.equals("net.minecraftforge.registries.GameData")) {
			return transformGameData(basicClass);
		}
		if (transformedName.equals("net.minecraft.network.play.server.SPacketEntityEffect")) {
			return transformSPacketEntityEffect(basicClass, name, transformedName);
		}
		if (transformedName.equals("net.minecraft.network.play.server.SPacketRemoveEntityEffect")) {
			return transformSPacketRemoveEntityEffect(basicClass);
		}
		return basicClass;
	}
	
	private static MethodNode locateMethod(ClassNode cn, String desc, String nameIn, String deobfNameIn) {
		return cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc) && (n.name.equals(nameIn) || n.name.equals(deobfNameIn)))
				.findAny().orElseThrow(() -> new ASMException(nameIn+" cannot be found in "+cn.name));
	}
	
	private static AbstractInsnNode locateTargetInsn(MethodNode mn, Predicate<AbstractInsnNode> filter) {
		AbstractInsnNode target = null;
		Iterator<AbstractInsnNode> i = mn.instructions.iterator();
		while (i.hasNext() && target == null) {
			AbstractInsnNode n = i.next();
			if (filter.test(n)) {
				target = n;
			}
		}
		if (target==null) {
			throw new ASMException("Can't locate target instruction in "+mn.name);
		}
		return target;
	}
	
	private byte[] transformSPacketRemoveEntityEffect(byte[] basicClass) {
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		String descriptors = "(L"+Type.getInternalName(PacketBuffer.class)+";)V";
		MethodNode rpd = locateMethod(cn, descriptors, "readPacketData", "func_148837_a");
		AbstractInsnNode target = locateTargetInsn(rpd, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode)n).name.equals("readUnsignedByte"));
		rpd.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), "readInt", "()I", false));
		rpd.instructions.remove(target);
		
		MethodNode wpd = locateMethod(cn, descriptors, "writePacketData", "func_148840_b");
		target = locateTargetInsn(wpd, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode)n).name.equals("writeByte"));
		wpd.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), "writeInt", "(I)Lio/netty/buffer/ByteBuf;", false));
		wpd.instructions.remove(target);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}
	
	private byte[] transformSPacketEntityEffect(byte[] basicClass, String name, String transformedName) {
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		//Adding a new field, int effectInt
		cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "effectInt", "I", null, 0));
		
		//Initialize this field in the constructor
		MethodNode mn_init = locateMethod(cn, "(IL"+Type.getInternalName(PotionEffect.class)+";)V", "<init>", "<init>");
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
		


		//Patch readPacketData
		MethodNode mn_readPacket = locateMethod(cn, "(L"+Type.getInternalName(PacketBuffer.class)+";)V", "readPacketData", "func_148837_a");
		String readVarInt_name = (name.equals(transformedName)?"readvarInt":"func_150792_a");
		
		AbstractInsnNode target = mn_readPacket.instructions.get(1);
		
		mn_readPacket.instructions.insert(target, new FieldInsnNode(Opcodes.PUTFIELD, name.replace(".", "/"), "entityInt", "I"));
		mn_readPacket.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), readVarInt_name, "()I", false));
		mn_readPacket.instructions.insert(target, new VarInsnNode(Opcodes.ALOAD, 1));
		mn_readPacket.instructions.insert(target, new VarInsnNode(Opcodes.ALOAD, 0));
		

		//Patch writePacketData
		MethodNode mn_writePacket = locateMethod(cn, "(L"+Type.getInternalName(PacketBuffer.class)+";)V", "writePacketData", "func_148840_b");
		String writeVarInt_name = (name.equals(transformedName)?"writeVarInt":"func_150787_b");
		AbstractInsnNode wp_target = mn_writePacket.instructions.get(1);
		mn_writePacket.instructions.insert(wp_target, new InsnNode(Opcodes.POP));
		mn_writePacket.instructions.insert(wp_target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PacketBuffer.class), writeVarInt_name, "(I)L"+Type.getInternalName(PacketBuffer.class)+";", false));
		mn_writePacket.instructions.insert(wp_target, new FieldInsnNode(Opcodes.GETFIELD, name.replace('.', '/'), "effectInt", "I"));
		mn_writePacket.instructions.insert(wp_target, new VarInsnNode(Opcodes.ALOAD, 0));
		mn_writePacket.instructions.insert(wp_target, new VarInsnNode(Opcodes.ALOAD, 1));
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}
	
	private byte[] transformGameData(byte[] basicClass) {
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		FieldNode mpid = cn.fields.parallelStream()
				.filter(fn -> fn.name.equals("MAX_POTION_ID"))
				.findAny().orElseThrow(() -> new ASMException("Error finding MAX_POTION_ID constant in GameData.class"));

		cn.fields.remove(mpid);
		cn.fields.add(new FieldNode(mpid.access, mpid.name, mpid.desc, mpid.signature, Integer.MAX_VALUE-1));

		MethodNode mn = locateMethod(cn, "()V", "init", "init");
		AbstractInsnNode target = locateTargetInsn(mn, n -> n.getOpcode()==Opcodes.SIPUSH && n.getPrevious().getOpcode()==Opcodes.LDC && ((LdcInsnNode) n.getPrevious()).cst.toString().equals("L"+Type.getInternalName(Potion.class)+";"));
		mn.instructions.insert(target, new LdcInsnNode(Integer.MAX_VALUE-1));
		mn.instructions.remove(target);

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}
	
	private byte[] transformPotionEffect(byte[] basicClass, String name, String transformedName) {
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		String tcompDesc = "L"+Type.getInternalName(NBTTagCompound.class)+";";
		
		MethodNode mn = locateMethod(cn, "("+tcompDesc+")"+tcompDesc, "writeCustomPotionEffectToNBT", "func_82719_a");
		AbstractInsnNode ant = locateTargetInsn(mn, n -> n.getOpcode() == Opcodes.I2B);
		String mname = (name==transformedName?"setInteger":"func_74768_a");
		MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(NBTTagCompound.class), mname, "(Ljava/lang/String;I)V", false);
		mn.instructions.remove(ant.getNext());
		mn.instructions.insert(ant, call);
		mn.instructions.remove(ant);
		
		
		MethodNode mn2 = locateMethod(cn, "("+tcompDesc+")L"+name.replace('.', '/')+";", "readCustomPotionEffectFromNBT", "func_82722_b");
		AbstractInsnNode ant2 = locateTargetInsn(mn2, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL);
		
		String name2 = (name.equals(transformedName)?"getInteger":"func_74762_e");
		
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.insert(ant2, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(NBTTagCompound.class), name2, "(Ljava/lang/String;)I", false));
		mn2.instructions.remove(ant2);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}

	private byte[] transformNetHandlerPlayClient(byte[] basicClass) {
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		MethodNode mn = locateMethod(cn, "(L"+Type.getInternalName(SPacketEntityEffect.class)+";)V", "handleEntityEffect", "func_147260_a");
		AbstractInsnNode target = locateTargetInsn(mn, n -> n.getOpcode()==Opcodes.SIPUSH);
		mn.instructions.remove(target.getPrevious());
		mn.instructions.remove(target.getNext());
		mn.instructions.insertBefore(target, new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(SPacketEntityEffect.class), "effectInt", "I"));
		mn.instructions.remove(target);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		return cw.toByteArray();
	}
	
	

}
