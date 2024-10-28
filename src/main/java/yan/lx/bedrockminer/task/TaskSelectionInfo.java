package yan.lx.bedrockminer.task;

import net.minecraft.util.math.BlockPos;

public class TaskSelectionInfo {
    public BlockPos pos1;
    public BlockPos pos2;

    public TaskSelectionInfo(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }
}
