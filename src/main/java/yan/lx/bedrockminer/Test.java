package yan.lx.bedrockminer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yan.lx.bedrockminer.command.argument.BlockPosArgumentType;
import yan.lx.bedrockminer.utils.BlockBreakerUtils;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.block.Block.sideCoversSmallSquare;

public class Test {

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> root) {
        root.then(literal("test").executes(Test::onCommandExecute)
                .then(argument("blockPos", BlockPosArgumentType.blockPos()).executes(Test::onBlockPos)));
    }

    private static int onBlockPos(CommandContext<FabricClientCommandSource> context) {
        var blockPos = BlockPosArgumentType.getBlockPos(context, "blockPos");
        BlockPlacerUtils.placement(blockPos.up(), Direction.NORTH);
        return 0;
    }

    public static int onCommandExecute(CommandContext<FabricClientCommandSource> context) {
        var client = MinecraftClient.getInstance();
        var world = client.world;
        var player = client.player;
        var hitResult = client.crosshairTarget;
        var interactionManager = client.interactionManager;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (world == null || player == null || hitResult == null || networkHandler == null) return 0;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            var blockHitResult = (BlockHitResult) hitResult;
            var blockPos = blockHitResult.getBlockPos();
            var blockState = world.getBlockState(blockPos);
            var block = blockState.getBlock();
            BlockBreakerUtils.simpleBreakBlock(blockPos);
            // Debug.info(world.getWorldBorder().getMaxRadius());
        }

        //BlockPlacerUtils.placement(new BlockPos(8, -60, 8), Direction.NORTH, Items.REDSTONE_TORCH);
//        var itemStack = player.getMainHandStack();
//        var registry = world.getRegistryManager().get(RegistryKeys.BLOCK);
//        var cachedBlockPosition = new CachedBlockPosition(world, blockPos, false);
//        Debug.info();
//        Debug.info("[canPlaceOn]" + itemStack.canPlaceOn(registry, cachedBlockPosition));
//        Debug.info("[canDestroy]" + itemStack.canDestroy(registry, cachedBlockPosition));

        return 0;
    }


    public static void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        var pos = hitResult.getPos();
        var side = hitResult.getSide();
        var blockPos = hitResult.getBlockPos();
        var insideBlock = hitResult.isInsideBlock();
        // Debug.info("%s, %s, %s, %s, %s", hand, pos, side, blockPos, insideBlock);
    }

    public static void onTick(ClientConnection connection, MinecraftServer server, ServerPlayerEntity player) {
        //  Debug.info("%s, %s", player.getYaw(), player.getPitch());
    }

    public static void onPlayerInteractBlock(ServerPlayerEntity player, PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        var hitResult = packet.getBlockHitResult();
        var hand = packet.getHand();
        var sequence = packet.getSequence();
        var pos = hitResult.getPos();
        var side = hitResult.getSide();
        var blockPos = hitResult.getBlockPos();
        var insideBlock = hitResult.isInsideBlock();
        Debug.info("%s, %s", player.getYaw(), player.getPitch());
        Debug.info("%s, %s, %s, %s, %s, %s", sequence, hand, pos, side, blockPos, insideBlock);
    }
}
