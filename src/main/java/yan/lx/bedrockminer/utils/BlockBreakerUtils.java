package yan.lx.bedrockminer.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.BedrockMinerMod;
import yan.lx.bedrockminer.Debug;


public class BlockBreakerUtils {
    public static boolean simpleBreakBlock(BlockPos pos) {
        return breakBlock(pos, Direction.UP);
    }

    public static boolean breakBlock(BlockPos blockPos, Direction direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || world == null || interactionManager == null || blockPos == null) {
            return false;
        }
        PlayerInventory playerInventory = player.getInventory();

        // 预检查
        if (world.getBlockState(blockPos).isReplaceable()) return true;
        if (world.getBlockState(blockPos).getBlock().getHardness() < 0) return false;

        // InventoryManagerUtils.autoSwitch();

        // interactionManager.attackBlock(blockPos, direction);
        // 更新方块正在破坏进程
        if (interactionManager.updateBlockBreakingProgress(blockPos, direction)) {
            client.particleManager.addBlockBreakingParticles(blockPos, direction);
        }
        // 检查是否已经成功
        return world.getBlockState(blockPos).isReplaceable();
    }

}