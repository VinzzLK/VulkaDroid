package net.vulkadroid.vulkan;

import net.vulkadroid.render.PipelineManager;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import net.vulkadroid.vulkan.shader.PipelineState;
import net.vulkadroid.vulkan.texture.VTextureSelector;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.vulkan.VK10.*;

public class VRenderSystem {

    private static final PipelineState currentState = new PipelineState();
    private static boolean stateDirty = true;

    private static float[] projectionMatrix = new float[16];
    private static float[] modelViewMatrix  = new float[16];

    private static boolean colorMaskR = true, colorMaskG = true, colorMaskB = true, colorMaskA = true;

    private static boolean fogEnabled = false;
    private static float fogStart = 0, fogEnd = 1;

    private static String currentShader = null;
    private static int activeTextureUnit = 0;

    // Clear color stored here so Renderer can read it
    private static float clearR = 0, clearG = 0, clearB = 0, clearA = 1;
    private static float clearDepth = 1.0f;

    // --- CLEAR ---
    public static void clear(int mask, boolean checkError) {
        // Handled by render pass LOAD_OP_CLEAR
    }

    public static void clearColor(float r, float g, float b, float a) {
        clearR = r; clearG = g; clearB = b; clearA = a;
    }

    public static void clearDepth(double depth) {
        clearDepth = (float) depth;
    }

    // --- DEPTH ---
    public static void enableDepthTest() {
        if (!currentState.depthTestEnabled) { currentState.depthTestEnabled = true; stateDirty = true; }
    }

    public static void disableDepthTest() {
        if (currentState.depthTestEnabled) { currentState.depthTestEnabled = false; stateDirty = true; }
    }

    public static void depthFunc(int func) {
        int vk = glToVkCompareOp(func);
        if (currentState.depthCompareOp != vk) { currentState.depthCompareOp = vk; stateDirty = true; }
    }

    public static void depthMask(boolean flag) {
        if (currentState.depthWriteEnabled != flag) { currentState.depthWriteEnabled = flag; stateDirty = true; }
    }

    // --- BLEND ---
    public static void enableBlend() {
        if (!currentState.blendEnabled) { currentState.blendEnabled = true; stateDirty = true; }
    }

    public static void disableBlend() {
        if (currentState.blendEnabled) { currentState.blendEnabled = false; stateDirty = true; }
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        int s = glToVkBlendFactor(srcFactor), d = glToVkBlendFactor(dstFactor);
        if (currentState.blendSrcFactor != s || currentState.blendDstFactor != d) {
            currentState.blendSrcFactor = s; currentState.blendDstFactor = d; stateDirty = true;
        }
    }

    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        currentState.blendSrcFactor      = glToVkBlendFactor(srcRGB);
        currentState.blendDstFactor      = glToVkBlendFactor(dstRGB);
        currentState.blendSrcAlphaFactor = glToVkBlendFactor(srcAlpha);
        currentState.blendDstAlphaFactor = glToVkBlendFactor(dstAlpha);
        stateDirty = true;
    }

    public static void blendEquation(int mode) {
        int vk = glToVkBlendOp(mode);
        if (currentState.blendOp != vk) { currentState.blendOp = vk; stateDirty = true; }
    }

    // --- CULL ---
    public static void enableCull() {
        if (!currentState.cullEnabled) { currentState.cullEnabled = true; stateDirty = true; }
    }

    public static void disableCull() {
        if (currentState.cullEnabled) { currentState.cullEnabled = false; stateDirty = true; }
    }

    public static void cullFace(int face) {
        int vk = face == GL_BACK ? VK_CULL_MODE_BACK_BIT :
                 face == GL_FRONT ? VK_CULL_MODE_FRONT_BIT : VK_CULL_MODE_FRONT_AND_BACK;
        if (currentState.cullMode != vk) { currentState.cullMode = vk; stateDirty = true; }
    }

    // --- POLYGON OFFSET ---
    public static void enablePolygonOffset() {
        if (!currentState.polygonOffsetEnabled) { currentState.polygonOffsetEnabled = true; stateDirty = true; }
    }

    public static void disablePolygonOffset() {
        if (currentState.polygonOffsetEnabled) { currentState.polygonOffsetEnabled = false; stateDirty = true; }
    }

    public static void polygonOffset(float factor, float units) {
        currentState.polygonOffsetFactor = factor;
        currentState.polygonOffsetUnits  = units;
        stateDirty = true;
    }

    // --- SCISSOR ---
    public static void enableScissorTest() {
        currentState.scissorEnabled = true;
    }

    public static void disableScissorTest() {
        currentState.scissorEnabled = false;
        if (Renderer.isFrameStarted()) setScissor(0, 0, SwapChain.getWidth(), SwapChain.getHeight());
    }

    public static void scissorBox(int x, int y, int w, int h) {
        setScissor(x, y, w, h);
    }

    private static void setScissor(int x, int y, int w, int h) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            int fbH = SwapChain.getHeight();
            scissor.offset().set(Math.max(0, x), Math.max(0, fbH - y - h));
            scissor.extent().set(Math.max(0, w), Math.max(0, h));
            vkCmdSetScissor(cmd, 0, scissor);
        }
    }

    // --- VIEWPORT ---
    public static void viewport(int x, int y, int w, int h) {
        VkCommandBuffer cmd = Renderer.getCurrentCommandBuffer();
        if (cmd == null) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer vp = VkViewport.calloc(1, stack);
            vp.x(x).y(y + h).width(w).height(-h).minDepth(0.0f).maxDepth(1.0f);
            vkCmdSetViewport(cmd, 0, vp);
        }
    }

    // --- TEXTURE ---
    public static void activeTexture(int unit) {
        activeTextureUnit = unit - GL_TEXTURE0;
    }

    public static void bindTexture(int id) {
        VTextureSelector.bindTexture(activeTextureUnit, id);
    }

    public static void setShaderTexture(int index, int textureId) {
        VTextureSelector.bindTexture(index, textureId);
    }

    // --- SHADER ---
    public static void setShader(Supplier<net.minecraft.client.renderer.ShaderInstance> shader) {
        if (shader != null && shader.get() != null) {
            currentShader = shader.get().getName();
            PipelineManager.bindPipeline(currentShader);
        }
    }

    // --- COLOR LOGIC OP ---
    public static void enableColorLogicOp() {
        currentState.colorLogicOpEnabled = true; stateDirty = true;
    }

    public static void disableColorLogicOp() {
        currentState.colorLogicOpEnabled = false; stateDirty = true;
    }

    public static void logicOp(int op) {
        currentState.logicOp = op; stateDirty = true;
    }

    // --- COLOR MASK ---
    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        colorMaskR = r; colorMaskG = g; colorMaskB = b; colorMaskA = a;
        currentState.colorWriteMask =
            (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) |
            (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        stateDirty = true;
    }

    // --- STENCIL ---
    public static void enableStencilTest()  { currentState.stencilTestEnabled = true;  stateDirty = true; }
    public static void disableStencilTest() { currentState.stencilTestEnabled = false; stateDirty = true; }

    public static void stencilFunc(int func, int ref, int mask) {
        currentState.stencilFunc        = glToVkCompareOp(func);
        currentState.stencilRef         = ref;
        currentState.stencilCompareMask = mask;
        stateDirty = true;
    }

    public static void stencilMask(int mask) { currentState.stencilWriteMask = mask; stateDirty = true; }

    public static void stencilOp(int sfail, int dpfail, int dppass) {
        currentState.stencilFailOp      = glToVkStencilOp(sfail);
        currentState.stencilDepthFailOp = glToVkStencilOp(dpfail);
        currentState.stencilPassOp      = glToVkStencilOp(dppass);
        stateDirty = true;
    }

    // --- FOG ---
    public static void enableFog()                        { fogEnabled = true; }
    public static void disableFog()                       { fogEnabled = false; }
    public static void fogStart(float s)                  { fogStart = s; }
    public static void fogEnd(float e)                    { fogEnd = e; }
    public static boolean isFogEnabled()                  { return fogEnabled; }
    public static float getFogStart()                     { return fogStart; }
    public static float getFogEnd()                       { return fogEnd; }

    // --- MATRICES ---
    public static void setProjectionMatrix(float[] m) { projectionMatrix = m; }
    public static void setModelViewMatrix(float[] m)  { modelViewMatrix  = m; }
    public static float[] getProjectionMatrix()        { return projectionMatrix; }
    public static float[] getModelViewMatrix()         { return modelViewMatrix; }

    // --- CLEAR COLOR ACCESSORS ---
    public static float getClearR() { return clearR; }
    public static float getClearG() { return clearG; }
    public static float getClearB() { return clearB; }
    public static float getClearA() { return clearA; }
    public static float getClearDepth() { return clearDepth; }

    // --- STATE ---
    public static PipelineState getCurrentState() { return currentState; }
    public static boolean isStateDirty()          { return stateDirty; }
    public static void clearDirtyFlag()           { stateDirty = false; }

    // --- GL → VK CONVERTERS ---
    private static int glToVkCompareOp(int glFunc) {
        return switch (glFunc) {
            case GL_NEVER    -> VK_COMPARE_OP_NEVER;
            case GL_LESS     -> VK_COMPARE_OP_LESS;
            case GL_EQUAL    -> VK_COMPARE_OP_EQUAL;
            case GL_LEQUAL   -> VK_COMPARE_OP_LESS_OR_EQUAL;
            case GL_GREATER  -> VK_COMPARE_OP_GREATER;
            case GL_NOTEQUAL -> VK_COMPARE_OP_NOT_EQUAL;
            case GL_GEQUAL   -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            case GL_ALWAYS   -> VK_COMPARE_OP_ALWAYS;
            default          -> VK_COMPARE_OP_LESS_OR_EQUAL;
        };
    }

    private static int glToVkBlendFactor(int gl) {
        return switch (gl) {
            case GL_ZERO                -> VK_BLEND_FACTOR_ZERO;
            case GL_ONE                 -> VK_BLEND_FACTOR_ONE;
            case GL_SRC_COLOR           -> VK_BLEND_FACTOR_SRC_COLOR;
            case GL_ONE_MINUS_SRC_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case GL_DST_COLOR           -> VK_BLEND_FACTOR_DST_COLOR;
            case GL_ONE_MINUS_DST_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case GL_SRC_ALPHA           -> VK_BLEND_FACTOR_SRC_ALPHA;
            case GL_ONE_MINUS_SRC_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case GL_DST_ALPHA           -> VK_BLEND_FACTOR_DST_ALPHA;
            case GL_ONE_MINUS_DST_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
            default                     -> VK_BLEND_FACTOR_ONE;
        };
    }

    private static int glToVkBlendOp(int gl) {
        return switch (gl) {
            case GL_FUNC_ADD              -> VK_BLEND_OP_ADD;
            case GL_FUNC_SUBTRACT         -> VK_BLEND_OP_SUBTRACT;
            case GL_FUNC_REVERSE_SUBTRACT -> VK_BLEND_OP_REVERSE_SUBTRACT;
            case GL_MIN                   -> VK_BLEND_OP_MIN;
            case GL_MAX                   -> VK_BLEND_OP_MAX;
            default                       -> VK_BLEND_OP_ADD;
        };
    }

    private static int glToVkStencilOp(int gl) {
        return switch (gl) {
            case GL_KEEP      -> VK_STENCIL_OP_KEEP;
            case GL_ZERO      -> VK_STENCIL_OP_ZERO;
            case GL_REPLACE   -> VK_STENCIL_OP_REPLACE;
            case GL_INCR      -> VK_STENCIL_OP_INCREMENT_AND_CLAMP;
            case GL_INCR_WRAP -> VK_STENCIL_OP_INCREMENT_AND_WRAP;
            case GL_DECR      -> VK_STENCIL_OP_DECREMENT_AND_CLAMP;
            case GL_DECR_WRAP -> VK_STENCIL_OP_DECREMENT_AND_WRAP;
            case GL_INVERT    -> VK_STENCIL_OP_INVERT;
            default           -> VK_STENCIL_OP_KEEP;
        };
    }
}
