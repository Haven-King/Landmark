package dev.hephaestus.landmark.mixin.client;

import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
    @ModifyConstant(method = "tweakTransparency", constant = @Constant(intValue = 0xFC000000))
    private static int allowAlmostTransparentText(int old) {
        // Seriously, fuck this shit.
        // Why is Mojang only checking SOME of the alpha bits?
        // Attempting to render text with alpha 0, 1, 2, or 3 causes the text to render at full opacity
        // I can understand rendering fully transparent text as fully opaque, I guess
        // But 1, 2, or 3? Fucking WHY?
        return 0xFF000000;
    }
}
