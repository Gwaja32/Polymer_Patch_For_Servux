package kr.haruserver.polymer_patch_for_servux.mixin;

import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.packet.ServuxTweaksHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServuxTweaksHandler.class)
public abstract class MixinServuxTweaksHandler
{
    @Inject(method = "encodeServerData", at = @At("HEAD"))
    private <P extends IServerPayloadData> void beforeEncode(ServerPlayerEntity player, P data, CallbackInfo ci)
    {
        if (!(data instanceof ServuxTweaksPacket packet)) return;
        if (!packet.getType().equals(ServuxTweaksPacket.Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE)) return;

        NbtCompound tag = packet.getCompound();
        if (tag == null) return;
        tag.putBoolean("_polymer_patch_for_servux_", true);
    }
}
