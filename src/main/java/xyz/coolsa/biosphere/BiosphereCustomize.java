package xyz.coolsa.biosphere;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BiosphereCustomize extends Screen {
    private final Text message;
    private Text scoreText;
    private final List<ButtonWidget> buttons = Lists.newArrayList();

    public BiosphereCustomize(@Nullable Text message, boolean isHardcore) {
        super(new TranslatableText("biosphere.customize"));
        this.message = message;
    }

    protected void init() {
        this.buttons.clear();
        this.buttons.add((ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height / 4 + 72, 200, 20, new TranslatableText("deathScreen.respawn"), (button) -> {
            this.client.player.requestRespawn();
            this.client.setScreen((Screen)null);
        })));


        ButtonWidget buttonWidget;
        for(Iterator var1 = this.buttons.iterator(); var1.hasNext(); buttonWidget.active = false) {
            buttonWidget = (ButtonWidget)var1.next();
        }

        this.scoreText = (new TranslatableText("deathScreen.score")).append(": ").append((new LiteralText(Integer.toString(this.client.player.getScore()))).formatted(Formatting.YELLOW));
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private void onConfirmQuit(boolean quit) {
        if (quit) {
            this.quitLevel();
        } else {
            this.client.player.requestRespawn();
            this.client.setScreen((Screen)null);
        }

    }

    private void quitLevel() {
        if (this.client.world != null) {
            this.client.world.disconnect();
        }

        this.client.disconnect(new SaveLevelScreen(new TranslatableText("menu.savingLevel")));
        this.client.setScreen(new TitleScreen());
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.fillGradient(matrices, 0, 0, this.width, this.height, 1615855616, -1602211792);
        matrices.push();
        matrices.scale(2.0F, 2.0F, 2.0F);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2 / 2, 30, 16777215);
        matrices.pop();
        if (this.message != null) {
            drawCenteredText(matrices, this.textRenderer, this.message, this.width / 2, 85, 16777215);
        }

        drawCenteredText(matrices, this.textRenderer, this.scoreText, this.width / 2, 100, 16777215);
        if (this.message != null && mouseY > 85) {
            Objects.requireNonNull(this.textRenderer);
            if (mouseY < 85 + 9) {
                Style style = this.getTextComponentUnderMouse(mouseX);
                this.renderTextHoverEffect(matrices, style, mouseX, mouseY);
            }
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Nullable
    private Style getTextComponentUnderMouse(int mouseX) {
        if (this.message == null) {
            return null;
        } else {
            int i = this.client.textRenderer.getWidth(this.message);
            int j = this.width / 2 - i / 2;
            int k = this.width / 2 + i / 2;
            return mouseX >= j && mouseX <= k ? this.client.textRenderer.getTextHandler().getStyleAt(this.message, mouseX - j) : null;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.message != null && mouseY > 85.0D) {
            Objects.requireNonNull(this.textRenderer);
            if (mouseY < (double)(85 + 9)) {
                Style style = this.getTextComponentUnderMouse((int)mouseX);
                if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == Action.OPEN_URL) {
                    this.handleTextClick(style);
                    return false;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isPauseScreen() {
        return false;
    }
}
