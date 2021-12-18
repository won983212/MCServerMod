package com.won983212.servermod.schematic.network;

import com.won983212.servermod.item.ModItems;
import com.won983212.servermod.network.IMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class CSchematicSync implements IMessage {

    public final int slot;
    public final boolean deployed;
    public final BlockPos anchor;
    public final Rotation rotation;
    public final Mirror mirror;
    public final boolean includeAir;

    public CSchematicSync(int slot, PlacementSettings settings,
                          BlockPos anchor, boolean deployed, boolean includeAir) {
        this.slot = slot;
        this.deployed = deployed;
        this.anchor = anchor;
        this.rotation = settings.getRotation();
        this.mirror = settings.getMirror();
        this.includeAir = includeAir;
    }

    public CSchematicSync(PacketBuffer buffer) {
        slot = buffer.readVarInt();
        deployed = buffer.readBoolean();
        anchor = buffer.readBlockPos();
        rotation = buffer.readEnum(Rotation.class);
        mirror = buffer.readEnum(Mirror.class);
        includeAir = buffer.readBoolean();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(slot);
        buffer.writeBoolean(deployed);
        buffer.writeBlockPos(anchor);
        buffer.writeEnum(rotation);
        buffer.writeEnum(mirror);
        buffer.writeBoolean(includeAir);
    }

    @Override
    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity player = context.get().getSender();
            if (player == null) {
                return;
            }
            ItemStack stack;
            if (slot == -1) {
                stack = player.getMainHandItem();
            } else {
                stack = player.inventory.getItem(slot);
            }
            if (ModItems.itemSchematic != stack.getItem()) {
                return;
            }
            CompoundNBT tag = stack.getOrCreateTag();
            tag.putBoolean("Deployed", deployed);
            tag.put("Anchor", NBTUtil.writeBlockPos(anchor));
            tag.putString("Rotation", rotation.name());
            tag.putString("Mirror", mirror.name());
            tag.putBoolean("IncludeAir", includeAir);
        });
        context.get().setPacketHandled(true);
    }

}
