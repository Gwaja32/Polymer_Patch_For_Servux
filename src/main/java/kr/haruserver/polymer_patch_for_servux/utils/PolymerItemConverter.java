package kr.haruserver.polymer_patch_for_servux.utils;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public final class PolymerItemConverter {
    private static final int MAX_CACHE = 10000;
    private static final ConcurrentMap<String, NbtCompound> cache = new ConcurrentHashMap<>();
    private static final Deque<String> lru = new ConcurrentLinkedDeque<>();
    private static final Object lruLock = new Object();

    private PolymerItemConverter() {}

    public static NbtCompound convertCompound(NbtCompound original)
    {
        if (original == null || original.isEmpty()) return original;
        NbtCompound copy = original.copy();
        walkAndConvert(copy);
        return copy;
    }

    private static void walkAndConvert(NbtElement element)
    {
        if (element instanceof NbtCompound compound)
        {
            if (compound.contains("id") && compound.contains("count"))
            {
                String itemId = compound.getString("id", "");
                if (!itemId.isEmpty() && !isVanilla(itemId))
                {
                    String key = makeCacheKey(itemId, compound);
                    NbtCompound converted = cache.get(key);
                    if (converted == null)
                    {
                        try
                        {
                            callPolymerConvert(compound, key);
                        }
                        catch (Throwable ignored) {}
                    } else
                    {
                        int count = compound.getInt("count").orElseThrow();
                        for (String _key : converted.getKeys()) {
                            NbtElement _element = converted.get(_key);
                            if (_element != null) {
                                compound.put(_key, _element.copy());
                            }
                        }
                        compound.putInt("count", count);
                    }
                    return;
                }
            }

            for (String k : new ArrayList<>(compound.getKeys()))
            {
                NbtElement child = compound.get(k);
                walkAndConvert(child);
            }
        }
        else if (element instanceof NbtList list)
        {
            for (int i = 0; i < list.size(); i++)
            {
                NbtElement child = list.get(i);
                walkAndConvert(child);
            }
        }
    }

    private static void callPolymerConvert(NbtCompound itemNbt, String mapKey)
    {
        ItemStack originalStack = new ItemStack(Registries.ITEM.get(Identifier.of(itemNbt.getString("id").orElseThrow())));
        ItemStack polymerStack = PolymerItemUtils.createItemStack(originalStack, PacketContext.get());
        polymerStack.setCount(itemNbt.getInt("count", 1));
        if (!(ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, polymerStack).result().orElseThrow() instanceof NbtCompound polymerNbt)) {
            return;
        }
        NbtCompound cacheCompound = new NbtCompound();
        for (String key : polymerNbt.getKeys()) {
            NbtElement element = polymerNbt.get(key);
            if (element != null) {
                itemNbt.put(key, element.copy());
                cacheCompound.put(key, element.copy());
            }
        }
        putCache(mapKey, cacheCompound);
    }

    private static boolean isVanilla(String itemId)
    {
        return itemId.startsWith("minecraft:");
    }

    private static String makeCacheKey(String itemId, NbtCompound compound)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] tagBytes = compound.contains("tag") ? compound.getCompound("tag").toString().getBytes(StandardCharsets.UTF_8) : new byte[0];
            md.update(itemId.getBytes(StandardCharsets.UTF_8));
            md.update((byte)':');
            md.update(tagBytes);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(itemId).append(':');
            for (int i = 0; i < 8 && i < digest.length; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        }
        catch (Exception e)
        {
            return itemId + ":" + compound.hashCode();
        }
    }

    private static void putCache(String key, NbtCompound value)
    {
        if (cache.size() > MAX_CACHE)
        {
            synchronized (lruLock)
            {
                String oldest = lru.pollFirst();
                if (oldest != null) cache.remove(oldest);
            }
        }
        cache.put(key, value);
        synchronized (lruLock)
        {
            lru.offerLast(key);
        }
    }
}
