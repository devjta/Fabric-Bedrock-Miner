package yan.lx.bedrockminer.task;

public enum TaskState {
    INITIALIZE,
    WAIT_GAME_UPDATE,
    WAIT,
    FIND_PISTON,
    FIND_REDSTONE_TORCH,
    FIND_SLIME_BLOCK,

    PLACE_PISTON,
    PLACE_REDSTONE_TORCH,
    PLACE_SLIME_BLOCK,
    EXECUTE,
    TIMEOUT,
    FAIL,
    RECYCLED_ITEMS,
    SUCCESS
}
