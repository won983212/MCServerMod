package com.won983212.servermod.utility;

import com.won983212.servermod.ServerMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Lang {
	public static TranslationTextComponent translate(String key, Object... args) {
		return createTranslationTextComponent(key, args);
	}

	public static TranslationTextComponent createTranslationTextComponent(String key, Object... args) {
		return new TranslationTextComponent(ServerMod.MODID + "." + key, args);
	}

	public static void sendStatus(PlayerEntity player, String key, Object... args) {
		player.displayClientMessage(createTranslationTextComponent(key, args), true);
	}

	public static List<ITextComponent> translatedOptions(String prefix, String... keys) {
		List<ITextComponent> result = new ArrayList<>(keys.length);
		for (String key : keys)
			result.add(translate(prefix + "." + key));

		return result;
	}

	public static String asId(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
