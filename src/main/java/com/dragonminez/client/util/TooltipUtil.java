package com.dragonminez.client.util;

import com.dragonminez.mixin.client.ClientTextTooltipAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class TooltipUtil {

    public static List<ClientTooltipComponent> wrapComponents(List<ClientTooltipComponent> components, Font font, int screenWidth) {
        int maxWidth = Math.max(screenWidth / 2, 60);
        List<ClientTooltipComponent> wrapped = new ArrayList<>();

        for (ClientTooltipComponent component : components) {
            if (component instanceof ClientTextTooltip textTooltip) {
                FormattedCharSequence charSequence = ((ClientTextTooltipAccessor) textTooltip).getText();
                Component text = toText(charSequence);

                if(text.getString().isEmpty()) {
                    wrapped.add(component);
                    continue;
                }

                List<FormattedCharSequence> splitLines = font.split(text, maxWidth);
                for (FormattedCharSequence splitLine : splitLines) wrapped.add(ClientTooltipComponent.create(splitLine));
            } else wrapped.add(component);
        }
        return wrapped;
    }

    public static Component toText(FormattedCharSequence sequence) {
        net.minecraft.network.chat.MutableComponent result = Component.empty();
        sequence.accept((index, style, codePoint) -> {
            result.append(Component.literal(new String(Character.toChars(codePoint))).withStyle(style));
            return true;
        });
        return result;
    }
}
