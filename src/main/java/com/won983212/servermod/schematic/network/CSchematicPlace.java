package com.won983212.servermod.schematic.network;

import com.won983212.servermod.CommonModDist;
import com.won983212.servermod.Logger;
import com.won983212.servermod.network.IMessage;
import com.won983212.servermod.schematic.SchematicPrinter;
import com.won983212.servermod.task.TaskScheduler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class CSchematicPlace implements IMessage {
    public final ItemStack stack;

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

            if (!player.canUseGameMasterBlocks()) {
                player.sendMessage(new StringTextComponent("§a관리자만 사용할 수 있는 기능입니다."), player.getUUID());
            }

            boolean includeAir = false;
            if (stack.hasTag() && stack.getTag().getBoolean("IncludeAir")) {
                includeAir = true;
            }

            final int[] percentIndex = {0};
            World world = player.getLevel();
            try {
                SchematicPrinter printer = SchematicPrinter.newPlacingSchematicTask(stack, world, (s, p) -> {
                    int percent = (int) Math.floor(p * 100);
                    if (percent >= percentIndex[0] * 10) {
                        percentIndex[0]++;
                        sendSchematicMessage(player, s + ": " + percent + "%");
                        if (percent == 100) {
                            sendSchematicMessage(player, "설치 완료했습니다.");
                        }
                    }
                }).includeAir(includeAir).maxBatchPlacing(10000);

                CommonModDist.SERVER_SCHEDULER.addAsyncTask(printer)
                        .exceptionally((e) -> handleException(player, e));
                sendSchematicMessage(player, "Schematic 설치를 시작합니다.");
            } catch (IllegalArgumentException e) {
                handleException(player, e);
            }
        });
        context.get().setPacketHandled(true);
    }

    private static void handleException(ServerPlayerEntity player, Exception e) {
        Logger.error(e);
        sendSchematicMessage(player, "설치 중 오류가 발생했습니다. 자세한 사항은 운영자에게 문의하세요.");
    }

    private static void sendSchematicMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(new StringTextComponent(TextFormatting.GOLD + "[Schematic] " + TextFormatting.RESET + message), player.getUUID());
    }
}
