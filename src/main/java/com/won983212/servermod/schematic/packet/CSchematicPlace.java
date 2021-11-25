package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.schematic.SchematicPrinter;
import com.won983212.servermod.utility.BlockHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class CSchematicPlace implements IMessage {
    public ItemStack stack;

    public CSchematicPlace(ItemStack stack) {
        this.stack = stack;
    }

    public CSchematicPlace(PacketBuffer buffer) {
        stack = buffer.readItem();
    }

    public void write(PacketBuffer buffer) {
        buffer.writeItem(stack);
    }

    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity player = context.get().getSender();
            if (player == null) {
                return;
            }

            World world = player.getLevel();
            SchematicPrinter printer = new SchematicPrinter();
            printer.loadSchematic(stack, world, !player.canUseGameMasterBlocks());
            if (!printer.isLoaded()) {
                return;
            }

            boolean includeAir = false;
            if (stack.hasTag() && stack.getTag().getBoolean("IncludeAir")) {
                includeAir = true;
            }

            while (printer.advanceCurrentPos()) {
                if (!printer.shouldPlaceCurrent(world)) {
                    continue;
                }

                boolean finalIncludeAir = includeAir;
                printer.handleCurrentTarget((pos, state, tile) -> {
                    boolean placingAir = state.getBlock().isAir(state, world, pos);
                    if (placingAir && !finalIncludeAir) {
                        return;
                    }

                    CompoundNBT tileData = tile != null ? tile.save(new CompoundNBT()) : null;
                    BlockHelper.placeSchematicBlock(world, state, pos, null, tileData);
                }, (pos, entity) -> world.addFreshEntity(entity));
            }
        });
        context.get().setPacketHandled(true);
    }
}
