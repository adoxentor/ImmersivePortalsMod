package com.qouteall.immersive_portals.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.SecondaryFrameBuffer;
import com.qouteall.immersive_portals.render.ShaderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL13;

public class RendererDebugWithShader extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean shouldSkipClearing() {
        return false;
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        if (Shaders.isShadowPass) {
            assert false;
            //ViewAreaRenderer.drawPortalViewTriangle(portal);
        }
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void prepareRendering() {
        if (CGlobal.shaderManager == null) {
            CGlobal.shaderManager = new ShaderManager();
        }
        
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
        
        OFInterface.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    protected void doRenderPortal(Portal portal, MatrixStack matrixStack) {
        if (MyRenderHelper.getRenderedPortalNum() >= 1) {
            return;
        }
        
        portalLayers.push(portal);
        
        manageCameraAndRenderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
        
        portalLayers.pop();
        
        deferredBuffer.fb.beginWrite(true);
        
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        mc.getFramebuffer().draw(
            deferredBuffer.fb.viewportWidth,
            deferredBuffer.fb.viewportHeight
        );
        
        OFInterface.bindToShaderFrameBuffer.run();
    }
    
    @Override
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        OFGlobal.shaderContextManager.switchContextAndRun(
            () -> {
                OFInterface.bindToShaderFrameBuffer.run();
                super.renderPortalContentWithContextSwitched(portal, oldCameraPos, oldWorld);
            }
        );
    }
    
    @Override
    public void onRenderCenterEnded(MatrixStack matrixStack) {
        if (isRendering()) {
            return;
        }
        
        if (MyRenderHelper.getRenderedPortalNum() == 0) {
            return;
        }
        
        GlStateManager.enableAlphaTest();
        Framebuffer mainFrameBuffer = mc.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        deferredBuffer.fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
    }
}
