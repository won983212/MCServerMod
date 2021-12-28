package com.won983212.servermod.schematic.client.tools;

import com.won983212.servermod.ModIcons;
import com.won983212.servermod.utility.Lang;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Tools {

    Move(new MoveTool(), ModIcons.TOOL_MOVE_XZ),
    MoveY(new MoveVerticalTool(), ModIcons.TOOL_MOVE_Y),
    Deploy(new DeployTool(), ModIcons.TOOL_DEPLOY),
    Rotate(new RotateTool(), ModIcons.TOOL_ROTATE),
    Flip(new FlipTool(), ModIcons.TOOL_MIRROR),
    ToggleAir(new ToggleAirTool(), ModIcons.EMPTY_BLOCK),
    Print(new PlaceTool(), ModIcons.CONFIRM);

    private final ISchematicTool tool;
    private final ModIcons icon;

    Tools(ISchematicTool tool, ModIcons icon) {
        this.tool = tool;
        this.icon = icon;
    }

    public ISchematicTool getTool() {
        return tool;
    }

    public TranslationTextComponent getDisplayName() {
        return Lang.translate("schematic.tool." + Lang.asId(name()));
    }

    public ModIcons getIcon() {
        return icon;
    }

    public List<ITextComponent> getDescription() {
        return Lang.translatedOptions("schematic.tool." + Lang.asId(name()) + ".description", "0", "1", "2", "3");
    }

}
