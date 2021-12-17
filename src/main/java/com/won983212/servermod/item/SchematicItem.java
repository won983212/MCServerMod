package com.won983212.servermod.item;

import com.won983212.servermod.schematic.SchematicProcessor;
import com.won983212.servermod.schematic.IProgressEvent;
import com.won983212.servermod.schematic.parser.SchematicFileParser;
import com.won983212.servermod.schematic.parser.SchematicContainer;
import com.won983212.servermod.server.command.SchematicCommand;
import com.won983212.servermod.utility.Lang;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.nio.file.Path;
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
        tag.putBoolean("IncludeAir", true);
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
        BlockPos bounds = loadSchematicSize(blueprint);
        tag.put("Bounds", NBTUtil.writeBlockPos(bounds));
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
        if (processNBT) {
            settings.addProcessor(SchematicProcessor.INSTANCE);
        }
        return settings;
    }

    public static BlockPos loadSchematicSize(ItemStack blueprint) {
        try {
            Path path = getSchematicPath(blueprint);
            return SchematicFileParser.parseSchematicBounds(path.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BlockPos.ZERO;
    }

    public static SchematicContainer loadSchematic(ItemStack blueprint, IProgressEvent event) {
        try {
            Path path = getSchematicPath(blueprint);
            return SchematicFileParser.parseSchematicFile(path.toFile(), event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new SchematicContainer();
    }

    private static Path getSchematicPath(ItemStack blueprint) throws IOException {
        String owner = blueprint.getTag().getString("Owner");
        String schematic = blueprint.getTag().getString("File");
        if (!SchematicFileParser.isSupportedExtension(schematic)) {
           throw new IOException("Unsupported file!");
        }
        return SchematicCommand.getFilePath(owner, schematic);
    }
}
