package kr.haruserver.polymer_patch_for_servux.utils;

import com.mojang.serialization.DataResult;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;
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
                    NbtCompound converted = cache.get(itemId);
                    if (converted == null)
                    {
                        try
                        {
                            callPolymerConvert(compound, itemId);
                        }
                        catch (Exception ignored) {} //Throwable ignored
                    } else
                    {
                        for (String _key : converted.getKeys()) {
                            NbtElement _element = converted.get(_key);
                            if (_element != null) {
                                compound.put(_key, _element.copy());
                            }
                        }
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
            for (NbtElement child : list) {
                walkAndConvert(child);
            }
        }
    }

    private static void callPolymerConvert(NbtCompound itemNbt, String mapKey)
    {
        ItemStack originalStack = new ItemStack(Registries.ITEM.get(Identifier.of(itemNbt.getString("id").orElseThrow())));
        NbtCompound polymerNbt = NbtCodecEncoder.encodeFilamentStackSelectively(originalStack);

        NbtCompound cacheCompound = new NbtCompound();
        for (String key : polymerNbt.getKeys()) {
            NbtElement element = polymerNbt.get(key);
            if (element != null) {
                if (!key.equals("count")) {
                    itemNbt.put(key, element.copy());
                    cacheCompound.put(key, element.copy());
                }
            }
        }
        putCache(mapKey, cacheCompound.copy());
    }

    private static boolean isVanilla(String itemId)
    {
        return itemId.startsWith("minecraft:");
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
