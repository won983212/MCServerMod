package com.won983212.servermod.schematic.packet;

import com.won983212.servermod.CommonModDist;
import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.schematic.SchematicPrinter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
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

            boolean includeAir = false;
            if (stack.hasTag() && stack.getTag().getBoolean("IncludeAir")) {
                includeAir = true;
            }

            final int[] percentIndex = {0};
            World world = player.getLevel();
            SchematicPrinter printer = new SchematicPrinter(includeAir);
            printer.loadSchematicAsync(stack, world, !player.canUseGameMasterBlocks(), (s, p) -> {
                int percent = (int) Math.floor(p * 100);
                if(percent >= percentIndex[0] * 20) {
                    percentIndex[0]++;
                    player.sendMessage(new StringTextComponent("§6[Schematic] §r" + s + ": " + percent + "%"), player.getUUID());
                }
            });

            String message;
            if(CommonModDist.PRINTERS.isEmpty()) {
                message = "§6[Schematic] §r블록 설치작업을 바로 시작합니다.";
            } else {
                message = "§6[Schematic] §r남은 " + CommonModDist.PRINTERS.size() + "개의 작업이 끝나면 바로 블록 설치작업을 시작합니다.";
            }
            player.sendMessage(new StringTextComponent(message), player.getUUID());
            CommonModDist.PRINTERS.offer(printer);
        });
        context.get().setPacketHandled(true);
    }
}
