package kr.haruserver.polymer_patch_for_servux.mixin;

import fi.dy.masa.servux.network.IServerPayloadData;
import fi.dy.masa.servux.network.packet.ServuxStructuresHandler;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket;
import fi.dy.masa.servux.network.packet.ServuxStructuresPacket.Type;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServuxStructuresHandler.class)
public class MixinServuxStructuresHandler
{
    @Unique
    private static final Set<Type> NBT_POLYMER_CONVERSION_TARGETS = Set.of(
            Type.PACKET_S2C_METADATA,
            Type.PACKET_S2C_STRUCTURE_DATA,
            Type.PACKET_S2C_STRUCTURE_DATA_START,
            Type.PACKET_S2C_SPAWN_METADATA
    );

    @Inject(method = "encodeStructuresPacket", at = @At("HEAD"))
    private <P extends IServerPayloadData> void beforeEncode(ServerPlayerEntity player, ServuxStructuresPacket packet, CallbackInfo ci)
    {
        if (!NBT_POLYMER_CONVERSION_TARGETS.contains(packet.getType())) return;

        NbtCompound tag = packet.getCompound();
        if (tag == null) return;
        tag.putBoolean("_polymer_patch_for_servux_", true);
    }
}
