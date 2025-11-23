package kr.haruserver.polymer_patch_for_servux.mixin;

import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;
import fi.dy.masa.servux.network.packet.ServuxTweaksHandler;
import fi.dy.masa.servux.network.packet.ServuxTweaksPacket;
import fi.dy.masa.servux.network.packet.ServuxTweaksPacket.Type;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServuxTweaksHandler.class)
public abstract class MixinServuxTweaksHandler
{
    @Unique
    private static final Set<Type> NBT_POLYMER_CONVERSION_TARGETS = Set.of(
            Type.PACKET_S2C_METADATA,
            Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE,
            Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE,
            Type.PACKET_S2C_NBT_RESPONSE_START,
            Type.PACKET_S2C_NBT_RESPONSE_DATA
    );

    @Inject(method = "encodeServerData", at = @At("HEAD"))
    private <P extends IServerPayloadData> void beforeEncode(ServerPlayerEntity player, P data, CallbackInfo ci)
    {
        if (!(data instanceof ServuxTweaksPacket packet)) return;
        if (!NBT_POLYMER_CONVERSION_TARGETS.contains(packet.getType())) return;

        NbtCompound tag = packet.getCompound();
        if (tag == null) return;
        tag.putBoolean("_polymer_patch_for_servux_", true);
    }
}
