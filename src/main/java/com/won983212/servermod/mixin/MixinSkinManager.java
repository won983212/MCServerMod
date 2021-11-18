package com.won983212.servermod.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.won983212.servermod.skin.SkinRedirector;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SkinManager.class)
public class MixinSkinManager {
    private SkinRedirector wrapper;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        wrapper = new SkinRedirector();
    }

    @Inject(method = "registerSkins", at = @At("HEAD"), cancellable = true)
    private void loadProfileTextures(GameProfile profile, SkinManager.ISkinAvailableCallback skinAvailableCallback, boolean requireSecure, CallbackInfo ci) {
        wrapper.loadProfileTexture(profile, skinAvailableCallback);
        ci.cancel();
    }

    @Inject(method = "getInsecureSkinInformation", at = @At("RETURN"), cancellable = true)
    private void loadSkinFromCache(GameProfile profile, CallbackInfoReturnable<Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> ci) {
        ci.setReturnValue(wrapper.loadSkinFromCache(profile));
    }
}
