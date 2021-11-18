package com.won983212.servermod.schematic.client.tools;

import com.won983212.servermod.ModIcons;
import com.won983212.servermod.utility.Lang;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Tools {

    Deploy(new DeployTool(), ModIcons.I_TOOL_DEPLOY),
    Move(new MoveTool(), ModIcons.I_TOOL_MOVE_XZ),
    MoveY(new MoveVerticalTool(), ModIcons.I_TOOL_MOVE_Y),
    Rotate(new RotateTool(), ModIcons.I_TOOL_ROTATE),
    Print(new PlaceTool(), ModIcons.I_CONFIRM),
    Flip(new FlipTool(), ModIcons.I_TOOL_MIRROR);

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

    public static List<Tools> getTools(boolean creative) {
        List<Tools> tools = new ArrayList<>();
        Collections.addAll(tools, Move, MoveY, Deploy, Rotate, Flip);
        if (creative)
            tools.add(Print);
        return tools;
    }

    public List<ITextComponent> getDescription() {
        return Lang.translatedOptions("schematic.tool." + Lang.asId(name()) + ".description", "0", "1", "2", "3");
    }

}
