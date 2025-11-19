package kr.haruserver.polymer_patch_for_servux.mixin;

import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.packet.ServuxStructuresHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServuxStructuresHandler.class)
public class MixinServuxStructuresHandler
{
    @Inject(method = "encodeStructuresPacket", at = @At("HEAD"))
    private <P extends IServerPayloadData> void beforeEncode(ServerPlayerEntity player, ServuxStructuresPacket packet, CallbackInfo ci)
    {
        NbtCompound tag = packet.getCompound();
        if (tag == null) return;
        tag.putBoolean("_polymer_patch_for_servux_", true);
    }
}
