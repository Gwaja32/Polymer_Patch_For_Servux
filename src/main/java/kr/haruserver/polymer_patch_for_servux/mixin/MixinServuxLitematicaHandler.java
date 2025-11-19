package kr.haruserver.polymer_patch_for_servux.mixin;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.packet.ServuxLitematicaHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServuxLitematicaHandler.class)
public abstract class MixinServuxLitematicaHandler
{
    @Inject(method = "encodeServerData", at = @At("HEAD"))
    private <P extends IServerPayloadData> void beforeEncode(ServerPlayerEntity player, P data, CallbackInfo ci)
    {
        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;
        if (packet == null) return;
        NbtCompound tag = packet.getCompound();
        if (tag == null) return;
        tag.putBoolean("_polymer_patch_for_servux_", true);
    }
}
