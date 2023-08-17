package yan.lx.bedrockminer.task;

import com.google.common.collect.Queues;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.Debug;
import yan.lx.bedrockminer.model.BlockInfo;
import yan.lx.bedrockminer.model.SchemeInfo;
import yan.lx.bedrockminer.utils.BlockBreakerUtils;
import yan.lx.bedrockminer.utils.BlockPlacerUtils;
import yan.lx.bedrockminer.utils.BlockUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static net.minecraft.block.Block.sideCoversSmallSquare;

public class TaskHandler {
    public final ClientWorld world;
    public final Block block;
    public final BlockPos pos;
    public final SchemeInfo[] schemeInfos;
    public final Queue<SchemeInfo> schemeQueues;
    public TaskState state;
    private int retryCount;
    private boolean timeout;
    private boolean fail;
    private boolean succeed;
    private boolean modifyLook;
    private boolean executed;
    private boolean executedModifyLook;


    private int totalTick;
    private final int totalMaxTick = 60;
    private int waitCount;
    private int waitCustom;
    private @Nullable TaskState waitNextState;
    private final int recycledItemsTickMaxCount = 20;
    private boolean recycledItems;


    public TaskHandler(ClientWorld world, Block block, BlockPos pos) {
        this.world = world;
        this.block = block;
        this.pos = pos;
        this.schemeInfos = SchemeFinder.findAllPossible(pos);
        this.schemeQueues = Queues.newConcurrentLinkedQueue();
        this.state = TaskState.INITIALIZE;
        this.waitNextState = null;
        Debug.info();
    }

    private void onModifyLook(Direction facing, TaskState nextState) {
        Debug.info("[%s] 修改视角, 下一个状态: %s", totalTick, nextState);
        modifyLook = true;
        ModifyLookManager.set(facing);
        state = TaskState.WAIT;
        waitNextState = nextState;
    }

    private void onModifyLook(BlockInfo blockInfo, TaskState nextState) {
        onModifyLook(blockInfo.facing, nextState);
    }

    private void onRevertLook() {
        Debug.info("[%s] 还原视角", totalTick);
        modifyLook = false;
        ModifyLookManager.reset();
    }

    private void onWait() {
        int waitTick = 1 + waitCustom;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        ClientPlayerEntity player = client.player;
        if (networkHandler != null && player != null) {
            PlayerListEntry playerListEntry = networkHandler.getPlayerListEntry(player.getUuid());
            if (playerListEntry != null) {
                int latency = playerListEntry.getLatency();
                waitTick += latency / 50;
            }
        }
        Debug.info("[%s] 等待 %stick, 当前tick: %s, 下一个状态: %s", totalTick, waitTick, waitTick, waitCount, waitNextState);
        if (waitCount++ > waitTick) {
            if (waitNextState != null) {
                state = waitNextState;
                waitNextState = null;
            } else {
                state = TaskState.WAIT_GAME_UPDATE;
            }
            waitCount = 0;
            waitCustom = 0;
        } else {
            state = TaskState.WAIT;
        }
    }


    private void onInit() {
        this.totalTick = 0;
        this.timeout = false;
        this.recycledItems = false;
        this.succeed = false;
        this.state = TaskState.WAIT_GAME_UPDATE;
        Debug.info("[%s] 初始化", totalTick);
    }

    private boolean canPlaceEntity(BlockState blockState, BlockPos blockPos) {
        boolean b = true;
        var shape = blockState.getCollisionShape(world, blockPos);
        if (!shape.isEmpty()) {
            for (var entity : world.getEntities()) {
                // 过滤掉落物实体
                if (entity instanceof ItemEntity) {
                    continue;
                }
                // 检查实体是否在目标位置内
                if (entity.collidesWithStateAtPos(blockPos, blockState)) {
                    b = false;
                }
            }
        }
        return b;
    }

    private void onSelectScheme() {
        List<SchemeInfo> list = new ArrayList<>();
        for (SchemeInfo schemeInfo : schemeInfos) {
            BlockInfo piston = schemeInfo.piston;
            BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
            BlockInfo slimeBlock = schemeInfo.slimeBlock;
            // 边界检查
            if (!World.isValid(piston.pos)) {
                Debug.info("[%s] 活塞超出世界边界", totalTick);
                continue;
            }
            if (!World.isValid(redstoneTorch.pos)) {
                Debug.info("[%s] 红石火把超出世界边界", totalTick);
                continue;
            }
            if (!World.isValid(slimeBlock.pos)) {
                Debug.info("[%s] 底座超出世界边界", totalTick);
                continue;
            }
            // 检查活塞
            if (!world.getBlockState(piston.pos).isReplaceable()
                    || !world.getBlockState(piston.pos.offset(piston.facing)).isReplaceable()) {
                continue;
            }
            // 检查红石火把
            if (!world.getBlockState(redstoneTorch.pos).isReplaceable()) {
                continue;
            }
            // 检查粘液块
            if (!(world.getBlockState(slimeBlock.pos).isReplaceable() || sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
                continue;
            }
            if (world.getBlockState(slimeBlock.pos).isReplaceable()) {
                if (!canPlaceEntity(Blocks.SLIME_BLOCK.getDefaultState(), slimeBlock.pos)) {
                    continue;
                }
                slimeBlock.level += 1;
            }
            list.add(schemeInfo);
        }
        // 重新排序
        list.sort((o1, o2) -> {
            int cr = 0;
            int a = o1.piston.level - o2.piston.level;
            if (a != 0) {
                cr = (a > 0) ? 3 : -1;
            } else {
                a = o1.redstoneTorch.level - o2.redstoneTorch.level;
                if (a != 0) {
                    cr = (a > 0) ? 2 : -2;
                } else {
                    a = o1.slimeBlock.level - o2.slimeBlock.level;
                    if (a != 0) {
                        cr = (a > 0) ? 1 : -3;
                    }
                }
            }
            return cr;
        });
        if (list.size() == 0) {
            Debug.info("[%s] 没有可以执行的方案", totalTick);
            onSucceed();
            return;
        }
        Debug.info("[%s] 查找方案, 已查找到%s个可执行方案", totalTick, list.size());
        schemeQueues.addAll(list);
//        for (var l : list) {
//            Debug.info("[%s] %s, (%s, %s, %s), (%s, %s, %s), (%s), (%s), (%s)",
//                    totalTick,
//                    l.direction,
//                    l.piston.level,
//                    l.redstoneTorch.level,
//                    l.slimeBlock.level,
//                    l.piston.facing,
//                    l.redstoneTorch.facing,
//                    l.slimeBlock.facing,
//                    l.piston.pos.toShortString(),
//                    l.redstoneTorch.pos.toShortString(),
//                    l.slimeBlock.pos.toShortString()
//            );
//        }
        if (!schemeQueues.isEmpty()) {
            state = TaskState.PLACE_SCHEME_BLOCK;
        }
    }

    private void onPlaceSchemeBlocks() {
        Debug.info("[%s] 放置方案方块", totalTick);
        var schemeInfo = schemeQueues.peek();
        if (schemeInfo != null) {
            BlockInfo piston = schemeInfo.piston;
            BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
            BlockInfo slimeBlock = schemeInfo.slimeBlock;
            Debug.info("[%s] [%s] [%s, %s, %s] [%s, %s, %s] (%s), (%s), (%s)",
                    totalTick,
                    schemeInfo.direction,
                    schemeInfo.piston.level,
                    schemeInfo.redstoneTorch.level,
                    schemeInfo.slimeBlock.level,
                    schemeInfo.piston.facing,
                    schemeInfo.redstoneTorch.facing,
                    schemeInfo.slimeBlock.facing,
                    schemeInfo.piston.pos.toShortString(),
                    schemeInfo.redstoneTorch.pos.toShortString(),
                    schemeInfo.slimeBlock.pos.toShortString()
            );

            // 放置
            if (world.getBlockState(piston.pos).isReplaceable()) {
                if (piston.modifyLook) {
                    Debug.info("[%s] 放置活塞", totalTick);
                    BlockPlacerUtils.placement(piston.pos, piston.facing, Items.PISTON);
                    piston.recycledItems = true;
                } else {
                    waitCustom = 1;
                    piston.modifyLook = true;
                    onModifyLook(piston, TaskState.PLACE_SCHEME_BLOCK);
                }
                return;
            }
            if (world.getBlockState(slimeBlock.pos).isReplaceable()) {
                if (slimeBlock.modifyLook) {
                    Debug.info("[%s] 放置粘液块", totalTick);
                    BlockPlacerUtils.placement(slimeBlock.pos, slimeBlock.facing, Items.SLIME_BLOCK);
                    slimeBlock.recycledItems = true;
                } else {
                    waitCustom = 1;
                    slimeBlock.modifyLook = true;
                    onModifyLook(slimeBlock, TaskState.PLACE_SCHEME_BLOCK);
                }
                return;
            }
            if (world.getBlockState(redstoneTorch.pos).isReplaceable()) {
                if (redstoneTorch.modifyLook) {
                    Debug.info("[%s] 放置红石火把", totalTick);
                    BlockPlacerUtils.placement(redstoneTorch.pos, redstoneTorch.facing, Items.REDSTONE_TORCH);
                    redstoneTorch.recycledItems = true;
                } else {
                    waitCustom = 1;
                    redstoneTorch.modifyLook = true;
                    onModifyLook(redstoneTorch, TaskState.PLACE_SCHEME_BLOCK);
                }
                return;
            }
            state = TaskState.WAIT;
            waitNextState = TaskState.WAIT_GAME_UPDATE;
        } else {
            state = TaskState.FAIL;
        }
    }

    private void onExecute() {
        Debug.info("[%s] 准备执行", totalTick);
        if (executed) {
            onWaitGameUpdate();
        } else {
            var schemeInfo = schemeQueues.peek();
            if (schemeInfo != null) {
                BlockInfo piston = schemeInfo.piston;
                BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
                if (!executedModifyLook) {
                    executedModifyLook = true;
                    waitCustom = 1;
                    onModifyLook(schemeInfo.direction.getOpposite(), TaskState.EXECUTE);
                    return;
                }
                // 打掉附近红石火把
                BlockPos[] nearbyRedstoneTorch = SchemeFinder.findPistonNearbyRedstoneTorch(piston.pos, world);
                for (BlockPos pos : nearbyRedstoneTorch) {
                    if (world.getBlockState(pos).isOf(Blocks.REDSTONE_TORCH) ||
                            world.getBlockState(pos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                        Debug.info("[%s] 打掉红石火把", totalTick);
                        BlockBreakerUtils.usePistonBreakBlock(pos);
                    }
                }
                if (world.getBlockState(redstoneTorch.pos).isOf(Blocks.REDSTONE_TORCH) ||
                        world.getBlockState(redstoneTorch.pos).isOf(Blocks.REDSTONE_WALL_TORCH)) {
                    Debug.info("[%s] 打掉红石火把", totalTick);
                    BlockBreakerUtils.usePistonBreakBlock(redstoneTorch.pos);
                }
                Debug.info("[%s] 打掉活塞", totalTick);
                BlockBreakerUtils.usePistonBreakBlock(piston.pos);
                Debug.info("[%s] 放置活塞", totalTick);
                BlockPlacerUtils.placement(piston.pos, schemeInfo.direction.getOpposite(), Items.PISTON);
                piston.recycledItems = true;
                executed = true;
                onWaitGameUpdate();
            }
        }
        //TODO: 待实现
    }

    private void onTimeoutHandle() {
        Debug.info("[%s] 超时处理", totalTick);
        state = TaskState.FAIL;
    }

    private void onFail() {
        Debug.info("[%s] 失败", totalTick);
        fail = true;
        state = TaskState.RECYCLED_ITEMS;
    }

    private void onSucceed() {
        if (modifyLook) {
            onRevertLook();
        }
        Debug.info("[%s] 成功", totalTick);
        succeed = true;
    }

    private void onRecycledItems() {
        Debug.info("[%s] 回收物品", totalTick);
        var schemeInfo = schemeQueues.peek();
        if (schemeInfo != null) {
            BlockInfo piston = schemeInfo.piston;
            BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
            BlockInfo slimeBlock = schemeInfo.slimeBlock;
            if (piston.recycledItems && piston.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收活塞", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(piston.pos)) {
                    piston.recycledItems = false;
                }
                ++piston.recycledTickCount;
                return;
            }
            if (redstoneTorch.recycledItems && redstoneTorch.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收红石火把", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(redstoneTorch.pos)) {
                    redstoneTorch.recycledItems = false;
                }
                ++redstoneTorch.recycledTickCount;
                return;
            }
            if (slimeBlock.recycledItems && slimeBlock.recycledTickCount < recycledItemsTickMaxCount) {
                Debug.info("[%s] 回收粘液块", totalTick);
                if (BlockBreakerUtils.usePistonBreakBlock(slimeBlock.pos)) {
                    slimeBlock.recycledItems = false;
                }
                ++slimeBlock.recycledTickCount;
                return;
            }
        }
        if (fail && retryCount++ < 1) {
            schemeQueues.clear();
            state = TaskState.INITIALIZE;
        } else {
            onSucceed();
        }
    }

    private void onWaitGameUpdate() {
        Debug.info("[%s] 等待更新", totalTick);
        if (schemeQueues.isEmpty()) {
            Debug.info("[%s] 没有课方案, 准备查找", totalTick);
            state = TaskState.SELECT_SCHEME;
            return;
        }
        var schemeInfo = schemeQueues.peek();
        if (schemeInfo == null) {
            Debug.info("[%s] 没有可行性方案, 待实现", totalTick);
            state = TaskState.FAIL;
            return;
        }
        BlockInfo piston = schemeInfo.piston;
        BlockInfo redstoneTorch = schemeInfo.redstoneTorch;
        BlockInfo slimeBlock = schemeInfo.slimeBlock;
        // 优先检查
        if (world.getBlockState(pos).isAir()) {
            Debug.info("[%s] 执行成功, 目标方块()不存在", totalTick, BlockUtils.getBlockName(block));
            state = TaskState.RECYCLED_ITEMS;
            return;
        }
        if (executed) {
            if (world.getBlockState(piston.pos).isOf(Blocks.MOVING_PISTON)) {
                Debug.info("[%s] 活塞正在移动处理", totalTick);
            }
            return;
        }
        // 底座检查
        BlockState slimeBlockState = world.getBlockState(slimeBlock.pos);
        if (!(slimeBlockState.isOf(Blocks.SLIME_BLOCK) || Block.sideCoversSmallSquare(world, slimeBlock.pos, slimeBlock.facing))) {
            Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或其他完整方块",
                    totalTick,
                    BlockUtils.getBlockName(slimeBlockState.getBlock()),
                    BlockUtils.getBlockName(Blocks.SLIME_BLOCK)
            );
            return;
        }
        // 红石火把检查
        BlockState redstoneTorchState = world.getBlockState(redstoneTorch.pos);
        if (!(redstoneTorchState.isOf(Blocks.REDSTONE_TORCH) || redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH))) {
            Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或 %s 方块",
                    totalTick,
                    BlockUtils.getBlockName(redstoneTorchState.getBlock()),
                    BlockUtils.getBlockName(Blocks.REDSTONE_BLOCK),
                    BlockUtils.getBlockName(Blocks.REDSTONE_WALL_TORCH)
            );
            return;
        }
        // 活塞检查
        BlockState pistonState = world.getBlockState(piston.pos);
        if (!(pistonState.isOf(Blocks.PISTON) || pistonState.isOf(Blocks.STICKY_PISTON))) {
            Debug.info("[%s] 状态错误, 目标方块(%s)不正确, 目标方块应为 %s 或 %s 方块",
                    totalTick,
                    BlockUtils.getBlockName(pistonState.getBlock()),
                    BlockUtils.getBlockName(Blocks.PISTON),
                    BlockUtils.getBlockName(Blocks.STICKY_PISTON)
            );
            return;
        }
        // 检查是否可以执行
        Direction facing = pistonState.get(PistonBlock.FACING);
        boolean extended = pistonState.get(PistonBlock.EXTENDED);
        // 红石火把方向检查
        if (!extended) {
            if (redstoneTorchState.isOf(Blocks.REDSTONE_WALL_TORCH)) {
                Direction redstoneTorchFacing = redstoneTorchState.get(WallRedstoneTorchBlock.FACING);
                if (redstoneTorchFacing != redstoneTorch.facing) {
                    Debug.info("[%s] 状态错误, 红石火把方向与方案方向不一致", totalTick);
                }
            } else if (redstoneTorchState.isOf(Blocks.REDSTONE_TORCH)) {
                if (redstoneTorch.facing == Direction.UP) {
                    Debug.info("[%s] 状态错误, 红石火把方向与方案方向不一致", totalTick);
                }
            }
        } else if (facing == piston.facing) {
            state = TaskState.EXECUTE;
            Debug.info("[%s] 条件充足, 准备执行", totalTick);
        }
    }

    public void onTick() {
        if (succeed) {
            return;
        }
        if (!timeout && totalTick > (recycledItems ? totalMaxTick + recycledItemsTickMaxCount : totalMaxTick)) {
            timeout = true;
            state = TaskState.TIMEOUT;
        }
        switch (state) {
            case INITIALIZE -> onInit();
            case SELECT_SCHEME -> onSelectScheme();
            case PLACE_SCHEME_BLOCK -> onPlaceSchemeBlocks();
            case WAIT_GAME_UPDATE -> onWaitGameUpdate();
            case WAIT -> onWait();
            case EXECUTE -> onExecute();
            case TIMEOUT -> onTimeoutHandle();
            case FAIL -> onFail();
            case RECYCLED_ITEMS -> onRecycledItems();
        }
        ++totalTick;
    }

    public boolean isSucceed() {
        return succeed;
    }
}