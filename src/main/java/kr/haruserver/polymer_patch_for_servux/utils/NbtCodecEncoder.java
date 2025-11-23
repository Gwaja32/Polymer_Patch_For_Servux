package kr.haruserver.polymer_patch_for_servux.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;
import xyz.nucleoid.packettweaker.PacketContext;

public class NbtCodecEncoder {
    private static NbtCompound selectivelyEncodeComponents(ComponentMap componentChanges) {
        NbtCompound componentsNbt = new NbtCompound();

        for (var entry : componentChanges.stream().toList()) {
            Identifier componentId = Identifier.of(entry.type().toString());
            Object value = entry.value();
            DataResult<? extends NbtElement> result = encodeComponentSafely(entry.type(), value);
            result.result().ifPresentOrElse(
                    element -> {
                        componentsNbt.put(componentId.toString(), element);
                    }, () -> {}
            );
        }
        return componentsNbt;
    }

    private static <T> DataResult<? extends NbtElement> encodeComponentSafely(ComponentType<T> type, Object value) {
        Codec<T> codec = type.getCodec();

        if (codec == null) {
            return DataResult.error(() -> "Codec is null for component: " + type);
        }

        try {
            @SuppressWarnings("unchecked")
            T castedValue = (T) value;

            return codec.encodeStart(NbtOps.INSTANCE, castedValue);

        } catch (ClassCastException e) {
            return DataResult.error(() -> "ClassCastException during encoding for component: " + type);
        }
    }

    public static NbtCompound encodeFilamentStackSelectively(ItemStack originalStack) {
        ComponentMap componentChanges = originalStack.getComponents();
        NbtCompound componentsNbt = selectivelyEncodeComponents(componentChanges);
        ItemStack polymerStack = PolymerItemUtils.createItemStack(originalStack, PacketContext.get());
        Identifier polymerItemId = Registries.ITEM.getId(polymerStack.getItem());
        NbtCompound finalPolymerNbtInput = new NbtCompound();
        finalPolymerNbtInput.putString("id", polymerItemId.toString());
        finalPolymerNbtInput.putByte("Count", (byte) polymerStack.getCount());
        if (!componentsNbt.isEmpty()) {
            finalPolymerNbtInput.put("components", componentsNbt);
        }
        DataResult<Pair<ItemStack, NbtElement>> rawDecodeResult = ItemStack.CODEC.decode(NbtOps.INSTANCE, finalPolymerNbtInput);
        DataResult<ItemStack> finalStackResult = rawDecodeResult.map(Pair::getFirst);
        ItemStack finalValidStack = finalStackResult.result()
                .orElseThrow(() -> new IllegalStateException("Failed to decode final NBT into valid ItemStack. Check required components."));
        NbtCompound polymerNbt = (NbtCompound) ItemStack.CODEC
                .encodeStart(NbtOps.INSTANCE, finalValidStack)
                .result()
                .orElseThrow(() -> new IllegalStateException("Final valid stack failed Codec encoding."));

        return polymerNbt;
    }
}
