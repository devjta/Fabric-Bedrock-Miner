package yan.lx.bedrockminer.mixins;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yan.lx.bedrockminer.task.TaskModifyLookHandle;

@Mixin(PlayerMoveC2SPacket.class)
public class MixinPlayerMoveC2SPacket {
    @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static float modifyLookYaw(float yaw) {
        return TaskModifyLookHandle.onModifyLookYaw(yaw);
    }

    @ModifyVariable(method = "<init>(DDDFFZZZ)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static float modifyLookPitch(float pitch) {
        return TaskModifyLookHandle.onModifyLookPitch(pitch);
    }
}
