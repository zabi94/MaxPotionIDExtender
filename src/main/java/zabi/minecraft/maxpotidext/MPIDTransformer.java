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

public class MPIDTransformer implements IClassTransformer {
	
	public static int classesTransformed = 0;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (transformedName.equals("net.minecraft.client.network.NetHandlerPlayClient")) {
			return transformNetHandlerPlayClient(basicClass);
		} 
		if (transformedName.equals("net.minecraft.potion.PotionEffect")) {
			return transformPotionEffect(basicClass);
		}
		if (transformedName.equals("net.minecraftforge.registries.GameData")) {
			return transformGameData(basicClass);
		}
		if (transformedName.equals("net.minecraft.network.play.server.SPacketEntityEffect")) {
			return transformSPacketEntityEffect(basicClass);
		}
		if (transformedName.equals("net.minecraft.network.play.server.SPacketRemoveEntityEffect")) {
			return transformSPacketRemoveEntityEffect(basicClass);
		}
		return basicClass;
	}
	
	private static MethodNode locateMethod(ClassNode cn, String desc, String nameIn, String deobfNameIn) {
		return cn.methods.parallelStream()
				.filter(n -> n.desc.equals(desc) && (n.name.equals(nameIn) || n.name.equals(deobfNameIn)))
				.findAny().orElseThrow(() -> new ASMException(nameIn +" ("+deobfNameIn+"): "+desc+" cannot be found in "+cn.name, cn));
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
			throw new ASMException("Can't locate target instruction in "+mn.name, mn);
		}
		return target;
	}
	
	private byte[] transformSPacketRemoveEntityEffect(byte[] basicClass) {
		Log.i("Patching SPacketRemoveEntityEffect");
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		String descriptors = "(L"+Obf.PacketBuffer+";)V";
		MethodNode rpd = locateMethod(cn, descriptors, "readPacketData", "a");
		AbstractInsnNode target = locateTargetInsn(rpd, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode)n).name.equals("readUnsignedByte"));
		rpd.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.PacketBuffer, "readInt", "()I", false));
		rpd.instructions.remove(target);
		
		MethodNode wpd = locateMethod(cn, descriptors, "writePacketData", "b");
		target = locateTargetInsn(wpd, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL && ((MethodInsnNode)n).name.equals("writeByte"));
		wpd.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.PacketBuffer, "writeInt", "(I)Lio/netty/buffer/ByteBuf;", false));
		wpd.instructions.remove(target);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		classesTransformed++;
		return cw.toByteArray();
	}
	
	private byte[] transformSPacketEntityEffect(byte[] basicClass) {
		Log.i("Patching SPacketEntityEffect");
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		//Adding a new field, int effectInt
		cn.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "effectInt", "I", null, 0));
		
		//Initialize this field in the constructor
		MethodNode mn_init = locateMethod(cn, "(IL"+Obf.PotionEffect+";)V", "<init>", "<init>");
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
		mn_init.instructions.insert(targetNode, new FieldInsnNode(Opcodes.PUTFIELD, Obf.SPacketEntityEffect, "effectInt", "I"));
		mn_init.instructions.insert(targetNode, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(CodeSnippets.class), "getIdFromPotEffect", "(L"+Obf.PotionEffect+";)I", false));
		mn_init.instructions.insert(targetNode, new VarInsnNode(Opcodes.ALOAD, 2));
		mn_init.instructions.insert(targetNode, new VarInsnNode(Opcodes.ALOAD, 0));
		


		//Patch readPacketData
		MethodNode mn_readPacket = locateMethod(cn, "(L"+Obf.PacketBuffer+";)V", "readPacketData", "a");
		String readVarInt_name = (Obf.isDeobf()?"readVarInt":"g");
		
		AbstractInsnNode target = mn_readPacket.instructions.get(1);
		
		mn_readPacket.instructions.insert(target, new FieldInsnNode(Opcodes.PUTFIELD, Obf.SPacketEntityEffect, "entityInt", "I"));
		mn_readPacket.instructions.insert(target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.PacketBuffer, readVarInt_name, "()I", false));
		mn_readPacket.instructions.insert(target, new VarInsnNode(Opcodes.ALOAD, 1));
		mn_readPacket.instructions.insert(target, new VarInsnNode(Opcodes.ALOAD, 0));
		

		//Patch writePacketData
		MethodNode mn_writePacket = locateMethod(cn, "(L"+Obf.PacketBuffer+";)V", "writePacketData", "b");
		String writeVarInt_name = (Obf.isDeobf()?"writeVarInt":"d");
		AbstractInsnNode wp_target = mn_writePacket.instructions.get(1);
		mn_writePacket.instructions.insert(wp_target, new InsnNode(Opcodes.POP));
		mn_writePacket.instructions.insert(wp_target, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.PacketBuffer, writeVarInt_name, "(I)L"+Obf.PacketBuffer+";", false));
		mn_writePacket.instructions.insert(wp_target, new FieldInsnNode(Opcodes.GETFIELD, Obf.SPacketEntityEffect, "effectInt", "I"));
		mn_writePacket.instructions.insert(wp_target, new VarInsnNode(Opcodes.ALOAD, 0));
		mn_writePacket.instructions.insert(wp_target, new VarInsnNode(Opcodes.ALOAD, 1));
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		classesTransformed++;
		return cw.toByteArray();
	}
	
	private byte[] transformGameData(byte[] basicClass) {
		Log.i("Patching GameData");
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);

		FieldNode mpid = cn.fields.parallelStream()
				.filter(fn -> fn.name.equals("MAX_POTION_ID"))
				.findAny().orElseThrow(() -> new ASMException("Error finding MAX_POTION_ID constant in GameData.class"));

		cn.fields.remove(mpid);
		cn.fields.add(new FieldNode(mpid.access, mpid.name, mpid.desc, mpid.signature, Integer.MAX_VALUE>>2));

		MethodNode mn = locateMethod(cn, "()V", "init", "init");
		AbstractInsnNode target = locateTargetInsn(mn, n -> n.getOpcode()==Opcodes.SIPUSH && n.getPrevious().getOpcode()==Opcodes.LDC && Obf.isPotionClass(((LdcInsnNode) n.getPrevious()).cst.toString()));
		mn.instructions.insert(target, new LdcInsnNode(Integer.MAX_VALUE>>2));
		mn.instructions.remove(target);

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		classesTransformed++;
		return cw.toByteArray();
	}
	
	private byte[] transformPotionEffect(byte[] basicClass) {
		Log.i("Patching PotionEffect");
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		MethodNode mn = locateMethod(cn, "(L"+Obf.NBTTagCompound+";)L"+Obf.NBTTagCompound+";", "writeCustomPotionEffectToNBT", "a");
		AbstractInsnNode ant = locateTargetInsn(mn, n -> n.getOpcode() == Opcodes.I2B);
		String mname = (Obf.isDeobf()?"setInteger":"a");
		MethodInsnNode call = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.NBTTagCompound, mname, "(Ljava/lang/String;I)V", false);
		mn.instructions.remove(ant.getNext());
		mn.instructions.insert(ant, call);
		mn.instructions.remove(ant);
		
		
		MethodNode mn2 = locateMethod(cn, "(L"+Obf.NBTTagCompound+";)L"+Obf.PotionEffect+";", "readCustomPotionEffectFromNBT", "b");
		AbstractInsnNode ant2 = locateTargetInsn(mn2, n -> n.getOpcode() == Opcodes.INVOKEVIRTUAL);
		
		String name2 = (Obf.isDeobf()?"getInteger":"h");
		
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.remove(ant2.getNext());
		mn2.instructions.insert(ant2, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Obf.NBTTagCompound, name2, "(Ljava/lang/String;)I", false));
		mn2.instructions.remove(ant2);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		classesTransformed++;
		return cw.toByteArray();
	}

	private byte[] transformNetHandlerPlayClient(byte[] basicClass) {
		Log.i("Patching NetHandlerPlayClient");
		ClassReader cr = new ClassReader(basicClass);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		MethodNode mn = locateMethod(cn, "(L"+Obf.SPacketEntityEffect+";)V", "handleEntityEffect", "a");
		AbstractInsnNode target = locateTargetInsn(mn, n -> n.getOpcode()==Opcodes.SIPUSH);
		mn.instructions.remove(target.getPrevious());
		mn.instructions.remove(target.getNext());
		mn.instructions.insertBefore(target, new FieldInsnNode(Opcodes.GETFIELD, Obf.SPacketEntityEffect, "effectInt", "I"));
		mn.instructions.remove(target);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		classesTransformed++;
		return cw.toByteArray();
	}
	
	

}
