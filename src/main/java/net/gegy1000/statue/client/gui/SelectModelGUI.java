package net.gegy1000.statue.client.gui;

import net.gegy1000.statue.client.gui.element.ModelViewElement;
import net.gegy1000.statue.server.api.ImportFile;
import net.gegy1000.statue.server.api.ModelProvider;
import net.gegy1000.statue.server.api.ProviderHandler;
import net.gegy1000.statue.server.api.StatueModel;
import net.gegy1000.statue.server.api.StatueTexture;
import net.gegy1000.statue.server.api.TextureProvider;
import net.gegy1000.statue.server.block.entity.StatueBlockEntity;
import net.gegy1000.statue.server.block.entity.StatueProperty;
import net.ilexiconn.llibrary.LLibrary;
import net.ilexiconn.llibrary.client.gui.ElementGUI;
import net.ilexiconn.llibrary.client.gui.element.ButtonElement;
import net.ilexiconn.llibrary.client.gui.element.ListElement;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SelectModelGUI extends ElementGUI implements ModelViewGUI {
    private Map<String, ImportFile> models = new HashMap<>();
    private List<String> modelNames = new ArrayList<>();

    private ListElement<SelectModelGUI> modelList;

    private ModelProvider<?, ? extends ImportFile> selectedProvider;
    private StatueModel selectedModel;
    private ModelBase selectedRenderModel;

    private TextureProvider<?, ? extends ImportFile> textureProvider;
    private StatueTexture texture;

    private StatueBlockEntity entity;

    private StatueModelGUI parent;

    public SelectModelGUI(StatueBlockEntity entity, StatueModelGUI parent) {
        this.entity = entity;
        this.parent = parent;
    }

    @Override
    protected void initElements() {
        this.selectedModel = null;
        this.selectedProvider = null;

        if (this.texture != null) {
            this.texture.delete(this.mc.world);
        }

        this.textureProvider = null;
        this.texture = null;

        this.clearElements();

        List<String> providers = new ArrayList<>();

        for (ModelProvider<?, ? extends ImportFile> provider : ProviderHandler.getModelProviders()) {
            String name = provider.getName();
            providers.add(name);
        }

        int width = this.width - 175;
        int height = (int) (width / 1.893);

        this.addElement(new ModelViewElement<>(this, (this.width - 175.0F) / 2 - width / 2 + 175.0F, (this.height - 32) / 2 - height / 2 + 14, width, height));

        this.addElement(new ListElement<>(this, 0.0F, 14.0F, 85, this.height - 32, providers, (list) -> {
            ModelProvider<?, ? extends ImportFile> provider = ProviderHandler.get(list.getSelectedIndex());
            if (provider != null) {
                this.selectedProvider = provider;
                this.createModelList();
                return true;
            }
            return false;
        }));

        this.addElement(new ButtonElement<>(this, "Cancel", 0.0F, this.height - 18, 50, 18, (button) -> {
            this.mc.displayGuiScreen(this.parent);
            return true;
        }));

        this.addElement(new ButtonElement<>(this, "Select", this.width - 50, this.height - 18, 50, 18, (button) -> {
            if (this.selectedModel != null) {
                this.entity.set(this.selectedProvider, this.selectedModel);
                this.entity.setTexture(this.textureProvider, this.texture);
                this.mc.displayGuiScreen(this.parent);
                return true;
            }
            return false;
        }));

        this.createModelList();
    }

    private void createModelList() {
        this.models.clear();
        this.modelNames.clear();

        if (this.selectedProvider != null) {
            Map<String, ? extends ImportFile> models = this.selectedProvider.getModels();
            for (Map.Entry<String, ? extends ImportFile> model : models.entrySet()) {
                String name = model.getKey();
                this.models.put(name, model.getValue());
                this.modelNames.add(name);
            }
            this.modelNames.sort(String::compareTo);
        }

        if (this.modelList != null) {
            this.removeElement(this.modelList);
        }

        this.addElement(this.modelList = new ListElement<>(this, 90.0F, 14.0F, 85, this.height - 32, this.modelNames, (list) -> {
            String name = list.getSelectedEntry();
            ImportFile file = this.models.get(name);
            StatueModel model = this.selectedProvider.getModelBase(file, name);
            if (model != null) {
                this.selectedModel = model;
                this.selectedRenderModel = model.create();
                Tuple<BufferedImage, TextureProvider<?, ?>> modelTexture = this.selectedProvider.getTextureBase(file, name);
                if (modelTexture != null) {
                    BufferedImage image = modelTexture.getFirst();
                    TextureProvider<?, ?> provider = modelTexture.getSecond();
                    this.texture = provider.create(image, name);
                    this.textureProvider = provider;
                } else {
                    if (this.texture != null) {
                        this.texture.delete(this.mc.world);
                    }
                    this.textureProvider = null;
                    this.texture = null;
                }
                return true;
            }
            return false;
        }));

        this.modelList.setEnabled(this.selectedProvider != null);
    }

    @Override
    public void drawScreen(float mouseX, float mouseY, float partialTicks) {
        this.drawRectangle(0, 0, this.width, 14, LLibrary.CONFIG.getPrimaryColor());
        this.drawRectangle(0, this.height - 18, this.width, 18, LLibrary.CONFIG.getPrimaryColor());

        this.fontRenderer.drawString("Model Types", 4, 3, LLibrary.CONFIG.getTextColor());
        this.fontRenderer.drawString("Models", 92, 3, LLibrary.CONFIG.getTextColor());
    }

    protected void drawRectangle(double x, double y, double width, double height, int color) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        builder.pos(x, y + height, 0.0).color(r, g, b, a).endVertex();
        builder.pos(x + width, y + height, 0.0).color(r, g, b, a).endVertex();
        builder.pos(x + width, y, 0.0).color(r, g, b, a).endVertex();
        builder.pos(x, y, 0.0).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public StatueModel getSelectedModel() {
        return this.selectedModel;
    }

    @Override
    public ModelBase getSelectedRenderModel() {
        return this.selectedRenderModel;
    }

    @Override
    public StatueTexture getSelectedTexture() {
        return this.texture;
    }

    @Override
    public ResourceLocation getSelectedRenderTexture() {
        return this.texture != null ? this.texture.get(this.mc.world) : null;
    }

    @Override
    public float getProperty(StatueProperty property) {
        return property.getDefaultValue();
    }

    @Override
    public float getInterpolatedProperty(StatueProperty property, float partialTicks) {
        return property.getDefaultValue();
    }
}
