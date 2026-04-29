package net.vulkadroid.vulkan.shader;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineState {
    // Depth
    public boolean depthTestEnabled  = true;
    public boolean depthWriteEnabled = true;
    public int     depthCompareOp    = VK_COMPARE_OP_LESS_OR_EQUAL;

    // Blend
    public boolean blendEnabled        = false;
    public int     blendSrcFactor      = VK_BLEND_FACTOR_SRC_ALPHA;
    public int     blendDstFactor      = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    public int     blendSrcAlphaFactor = VK_BLEND_FACTOR_ONE;
    public int     blendDstAlphaFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    public int     blendOp             = VK_BLEND_OP_ADD;
    public int     blendAlphaOp        = VK_BLEND_OP_ADD;

    // Cull
    public boolean cullEnabled = true;
    public int     cullMode    = VK_CULL_MODE_BACK_BIT;
    public int     frontFace   = VK_FRONT_FACE_COUNTER_CLOCKWISE;

    // Color write
    public int colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;

    // Polygon offset
    public boolean polygonOffsetEnabled = false;
    public float   polygonOffsetFactor  = 0;
    public float   polygonOffsetUnits   = 0;

    // Scissor
    public boolean scissorEnabled = false;

    // Stencil
    public boolean stencilTestEnabled = false;
    public int     stencilFunc        = VK_COMPARE_OP_ALWAYS;
    public int     stencilRef         = 0;
    public int     stencilCompareMask = 0xFF;
    public int     stencilWriteMask   = 0xFF;
    public int     stencilFailOp      = VK_STENCIL_OP_KEEP;
    public int     stencilDepthFailOp = VK_STENCIL_OP_KEEP;
    public int     stencilPassOp      = VK_STENCIL_OP_REPLACE;

    // Logic op
    public boolean colorLogicOpEnabled = false;
    public int     logicOp             = 0;

    // Key for pipeline cache lookup
    public long computeKey() {
        long key = 0;
        key |= (depthTestEnabled  ? 1L : 0L) << 0;
        key |= (depthWriteEnabled ? 1L : 0L) << 1;
        key |= ((long) depthCompareOp & 0x7L) << 2;
        key |= (blendEnabled ? 1L : 0L) << 5;
        key |= ((long) blendSrcFactor & 0xFL) << 6;
        key |= ((long) blendDstFactor & 0xFL) << 10;
        key |= ((long) blendOp       & 0x7L) << 14;
        key |= (cullEnabled ? 1L : 0L) << 17;
        key |= ((long) cullMode   & 0x3L) << 18;
        key |= ((long) frontFace  & 0x1L) << 20;
        key |= ((long) colorWriteMask & 0xFL) << 21;
        key |= (polygonOffsetEnabled ? 1L : 0L) << 25;
        key |= (stencilTestEnabled   ? 1L : 0L) << 26;
        return key;
    }

    public PipelineState copy() {
        PipelineState s = new PipelineState();
        s.depthTestEnabled     = this.depthTestEnabled;
        s.depthWriteEnabled    = this.depthWriteEnabled;
        s.depthCompareOp       = this.depthCompareOp;
        s.blendEnabled         = this.blendEnabled;
        s.blendSrcFactor       = this.blendSrcFactor;
        s.blendDstFactor       = this.blendDstFactor;
        s.blendSrcAlphaFactor  = this.blendSrcAlphaFactor;
        s.blendDstAlphaFactor  = this.blendDstAlphaFactor;
        s.blendOp              = this.blendOp;
        s.blendAlphaOp         = this.blendAlphaOp;
        s.cullEnabled          = this.cullEnabled;
        s.cullMode             = this.cullMode;
        s.frontFace            = this.frontFace;
        s.colorWriteMask       = this.colorWriteMask;
        s.polygonOffsetEnabled = this.polygonOffsetEnabled;
        s.polygonOffsetFactor  = this.polygonOffsetFactor;
        s.polygonOffsetUnits   = this.polygonOffsetUnits;
        s.scissorEnabled       = this.scissorEnabled;
        s.stencilTestEnabled   = this.stencilTestEnabled;
        s.stencilFunc          = this.stencilFunc;
        s.stencilRef           = this.stencilRef;
        s.colorLogicOpEnabled  = this.colorLogicOpEnabled;
        s.logicOp              = this.logicOp;
        return s;
    }
}
