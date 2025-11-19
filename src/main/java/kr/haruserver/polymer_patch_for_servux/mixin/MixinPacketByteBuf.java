package kr.haruserver.polymer_patch_for_servux.mixin;

import kr.haruserver.polymer_patch_for_servux.utils.PolymerItemConverter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PacketByteBuf.class)
public abstract class MixinPacketByteBuf
{
    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void onWriteNbt(NbtElement nbt, CallbackInfoReturnable<PacketByteBuf> cir)
    {
        if (!(nbt instanceof NbtCompound compound)) return;
        if (compound.isEmpty()) return;
        if (!compound.getBoolean("_polymer_patch_for_servux_", false)) return;
        compound.remove("_polymer_patch_for_servux_");

        try
        {
            NbtCompound converted = PolymerItemConverter.convertCompound(compound);
            if (converted != null)
            {
                for (String _key : compound.getKeys().toArray(new String[0])) {
                    compound.remove(_key);
                }
                compound.copyFrom(converted);
            }
        }
        catch (Throwable ignored) {}
    }
}