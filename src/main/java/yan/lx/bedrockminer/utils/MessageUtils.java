package yan.lx.bedrockminer.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class MessageUtils {
    public static void setOverlayMessage(Text message) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(message, false);
    }

    public static void addMessage(Text message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }
}
