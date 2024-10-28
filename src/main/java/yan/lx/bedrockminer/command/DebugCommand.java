package yan.lx.bedrockminer.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import yan.lx.bedrockminer.BedrockMinerLang;
import yan.lx.bedrockminer.config.Config;
import yan.lx.bedrockminer.utils.MessageUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class DebugCommand extends BaseCommand {

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.then(literal("true").executes(context -> toggleSwitch(true)))
                .then(literal("false").executes(context -> toggleSwitch(false)));
    }

    private int toggleSwitch(boolean b) {
        if (b) {
            MessageUtils.addMessage(BedrockMinerLang.DEBUG_ON);
        } else {
            MessageUtils.addMessage(BedrockMinerLang.DEBUG_OFF);
        }
        Config.INSTANCE.debug = b;
        Config.save();
        return 0;
    }
}
