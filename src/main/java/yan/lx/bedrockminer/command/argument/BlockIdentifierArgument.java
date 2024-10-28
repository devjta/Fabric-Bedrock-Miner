package yan.lx.bedrockminer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import yan.lx.bedrockminer.utils.BlockUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.minecraft.command.argument.BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION;

public class BlockIdentifierArgument implements ArgumentType<Block> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone");
    @Nullable
    private Function<Identifier, Boolean> filter;

    public static Block getBlock(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, Block.class);
    }

    public Block parse(StringReader reader) throws CommandSyntaxException {
        var i = reader.getCursor();
        while (reader.canRead() && isCharValid(reader.peek())) {
            reader.skip();
        }
        // 获取用户输入的字符串内容
        var string = reader.getString().substring(i, reader.getCursor());
        // 检查方块注册表中是否存在该名称
        var blockId = Identifier.of(string);
        var optionalBlock = Registries.BLOCK.stream().filter(block -> BlockUtils.getIdentifier(block).equals(blockId)).findFirst();
        if (optionalBlock.isEmpty()) {
            reader.setCursor(i);
            throw INVALID_BLOCK_ID_EXCEPTION.create(string);
        }
        // 已获取到方块信息
        var block = optionalBlock.get();
        // 检查过滤器
        if (filter != null && !filter.apply(BlockUtils.getIdentifier(block))) {
            reader.setCursor(i);
            throw INVALID_BLOCK_ID_EXCEPTION.create(string);
        }
        return block;
    }


    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        // 读取标识符
        var i = reader.getCursor();
        while (reader.canRead() && isCharValid(reader.peek())) {
            reader.skip();
        }
        // 获取用户输入的字符串内容
        var string = reader.getString().substring(i, reader.getCursor());
        // 检查方块注册表中是否存在该名称
        Registries.BLOCK.forEach(block -> {
            var identifier = BlockUtils.getIdentifier(block);
            var namespace = identifier.getNamespace();
            var path = identifier.getPath();
            if (namespace.contains(string) || path.contains(string)) {
                if (filter != null && filter.apply(identifier)) {
                    // 添加建议列表
                    builder.suggest(BlockUtils.getId(block));
                }
            }
        });
        return builder.buildFuture();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public BlockIdentifierArgument setFilter(Function<Identifier, Boolean> filter) {
        this.filter = filter;
        return this;
    }

    public static boolean isCharValid(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

}
