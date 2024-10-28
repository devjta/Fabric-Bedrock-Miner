package yan.lx.bedrockminer.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yan.lx.bedrockminer.task.TaskManager;
import yan.lx.bedrockminer.task.TaskModifyLookHandle;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        TaskModifyLookHandle.onTick();
        TaskManager.tick();
    }
}
