package com.won983212.servermod;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.Util;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SkinRedirector {
    private final Gson gson = new Gson();
    private final LoadingCache<String, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> skinCacheLoader;

    public SkinRedirector() {
        skinCacheLoader = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.SECONDS).build(new CacheLoader<String, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>>() {
            public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> load(String name) {
                return loadTextureInfo(name);
            }
        });
    }

    /**
     * 최초 플레이어 스킨을 캐싱할 때 1회 호출됨.
     */
    public void loadProfileTexture(GameProfile profile, SkinManager.ISkinAvailableCallback skinAvailableCallback) {
        Runnable runnable = () -> {
            Logger.info("Load player texture: " + profile.getName());
            final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = loadSkinFromCache(profile);
            final Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                RenderSystem.recordRenderCall(() -> {
                    ImmutableList.of(MinecraftProfileTexture.Type.SKIN, MinecraftProfileTexture.Type.CAPE).forEach((p_229296_3_) -> {
                        if (map.containsKey(p_229296_3_)) {
                            mc.getSkinManager().loadSkin(map.get(p_229296_3_), p_229296_3_, skinAvailableCallback);
                        }
                    });
                });
            });
        };
        Util.getServerExecutor().execute(runnable);
    }

    /**
     * 여러번 호출될 수 있음. 따라서 캐싱을 내부적으로 지원해야함.
     */
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache(GameProfile profile) {
        return this.skinCacheLoader.getUnchecked(profile.getName());
    }

    private static void AppendSkin(Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map, MinecraftProfileTexture.Type type, String skinFilename) {
        if (skinFilename != null)
            map.put(type, new MinecraftProfileTexture(ServerMod.HOST + "textures/" + skinFilename, null));
    }

    /**
     * 실제로 웹에서 택스쳐정보를 불러옴. 이 메서드는 캐시해서 사용해야함. (skinCacheLoader를 통해서 사용)
     */
    private Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadTextureInfo(String name) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = Maps.newHashMap();
        Logger.info("Load texture info: " + name);
        try {
            String json = connectionGetUserTexture(name);
            SkinTextureInfo info = gson.fromJson(json, SkinTextureInfo.class);
            AppendSkin(map, MinecraftProfileTexture.Type.SKIN, info.getSkin());
            AppendSkin(map, MinecraftProfileTexture.Type.CAPE, info.getCape());
            AppendSkin(map, MinecraftProfileTexture.Type.ELYTRA, info.getElytra());
        } catch (FileNotFoundException e) {
            Logger.warn("Can't find skin profile: " + name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static String connectionGetUserTexture(final String name) throws IOException {
        Validate.notNull(name);

        final URL url = new URL(ServerMod.HOST + name + ".json");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);

        Logger.debug("Reading data from " + url);

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
            final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
            Logger.debug("Successful read, server response was " + connection.getResponseCode());
            Logger.debug("Response: " + result);
            return result;
        } catch (final IOException e) {
            IOUtils.closeQuietly(inputStream);
            inputStream = connection.getErrorStream();

            if (inputStream != null) {
                Logger.debug("Reading error page from " + url);
                final String result = IOUtils.toString(inputStream, Charsets.UTF_8);
                Logger.debug("Successful read, server response was " + connection.getResponseCode());
                Logger.debug("Response: " + result);
            } else {
                Logger.debug("Request failed: " + e.toString());
            }
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
