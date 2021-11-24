package com.won983212.servermod.item;

import com.won983212.servermod.schematic.SchematicProcessor;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import com.won983212.servermod.utility.Lang;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SchematicItem extends Item {

    public SchematicItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(String schematic, String owner) {
        ItemStack blueprint = new ItemStack(ModItems.itemSchematic);
        writeTo(blueprint, schematic, owner);
        return blueprint;
    }

    public static void writeTo(ItemStack stack, String schematic, String owner) {
        CompoundNBT tag = new CompoundNBT();
        tag.putBoolean("Deployed", false);
        tag.putString("Owner", owner);
        tag.putString("File", schematic);
        tag.put("Anchor", NBTUtil.writeBlockPos(BlockPos.ZERO));
        tag.putString("Rotation", Rotation.NONE.name());
        tag.putString("Mirror", Mirror.NONE.name());
        stack.setTag(tag);
        writeSize(stack);
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void appendHoverText(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            if (stack.getTag().contains("File")) {
                tooltip.add(new StringTextComponent(TextFormatting.GOLD + stack.getTag().getString("File")));
            }
        } else {
            tooltip.add(Lang.translate("schematic.invalid").withStyle(TextFormatting.RED));
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    public static void writeSize(ItemStack blueprint) {
        CompoundNBT tag = blueprint.getTag();
        Template t = loadSchematic(blueprint);
        tag.put("Bounds", NBTUtil.writeBlockPos(t.getSize()));
        blueprint.setTag(tag);
    }

    public static PlacementSettings getSettings(ItemStack blueprint) {
        return getSettings(blueprint, true);
    }

    public static PlacementSettings getSettings(ItemStack blueprint, boolean processNBT) {
        CompoundNBT tag = blueprint.getTag();
        PlacementSettings settings = new PlacementSettings();
        settings.setRotation(Rotation.valueOf(tag.getString("Rotation")));
        settings.setMirror(Mirror.valueOf(tag.getString("Mirror")));
        if (processNBT)
            settings.addProcessor(SchematicProcessor.INSTANCE);
        return settings;
    }

    public static Template loadSchematic(ItemStack blueprint) {
        Template t = new Template();
        String owner = blueprint.getTag().getString("Owner");
        String schematic = blueprint.getTag().getString("File");
        String schematicExt = schematic.substring(schematic.lastIndexOf('.') + 1);

        if (!SchematicFileParser.isSupportedExtension(schematicExt))
            return t;

        Path dir;
        Path file;

        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            dir = Paths.get("schematics", "uploaded").toAbsolutePath();
            file = Paths.get(owner, schematic);
        } else {
            dir = Paths.get("schematics").toAbsolutePath();
            file = Paths.get(schematic);
        }

        Path path = dir.resolve(file).normalize();
        if (!path.startsWith(dir))
            return t;

        try {
            return SchematicFileParser.parseSchematicFile(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    @Nonnull
    @Override
    public ActionResultType useOn(ItemUseContext context) {
        if (context.getPlayer() != null && !onItemUse(context.getPlayer(), context.getHand()))
            return super.useOn(context);
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (!onItemUse(playerIn, handIn))
            return super.use(worldIn, playerIn, handIn);
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getItemInHand(handIn));
    }

    private boolean onItemUse(PlayerEntity player, Hand hand) {
        // TODO Test Code
        if (player.isShiftKeyDown()) {
            ItemStack stack = player.getItemInHand(hand);
            writeTo(stack, "ModernHouse137.schem", player.getName().getString());
            return true;
        }

        if (!player.isShiftKeyDown() || hand != Hand.MAIN_HAND)
            return false;
        return player.getItemInHand(hand).hasTag();
    }

}
