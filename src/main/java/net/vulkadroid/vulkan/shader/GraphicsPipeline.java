package net.vulkadroid.vulkan.shader;

import net.vulkadroid.vulkan.device.DeviceManager;
import net.vulkadroid.vulkan.framebuffer.RenderPass;
import net.vulkadroid.vulkan.framebuffer.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline {

    private long pipeline = VK_NULL_HANDLE;
    private long pipelineLayout = VK_NULL_HANDLE;
    private long descriptorSetLayout = VK_NULL_HANDLE;
    private static long pipelineCache = VK_NULL_HANDLE;

    private final String name;
    private final PipelineState state;

    // Cache: state key → pipeline
    private static final Map<Long, GraphicsPipeline> pipelineCache2 = new HashMap<>();

    static {
        createPipelineCache();
    }

    private static void createPipelineCache() {
        try (MemoryStack stack = stackPush()) {
            VkPipelineCacheCreateInfo cacheInfo = VkPipelineCacheCreateInfo.calloc(stack);
            cacheInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);
            LongBuffer pCache = stack.mallocLong(1);
            vkCreatePipelineCache(DeviceManager.getDevice(), cacheInfo, null, pCache);
            pipelineCache = pCache.get(0);
        }
    }

    public GraphicsPipeline(String name, PipelineState state, String vertPath, String fragPath,
                            VkVertexInputBindingDescription.Buffer bindingDesc,
                            VkVertexInputAttributeDescription.Buffer attrDesc) {
        this.name  = name;
        this.state = state.copy();
        create(vertPath, fragPath, bindingDesc, attrDesc);
    }

    private void create(String vertPath, String fragPath,
                        VkVertexInputBindingDescription.Buffer bindingDesc,
                        VkVertexInputAttributeDescription.Buffer attrDesc) {
        try (MemoryStack stack = stackPush()) {
            // Load shader modules
            long vertModule = createShaderModule(loadSpirv(vertPath));
            long fragModule = createShaderModule(loadSpirv(fragPath));

            // Shader stages
            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertStage = shaderStages.get(0);
            vertStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertStage.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertStage.module(vertModule);
            vertStage.pName(stack.UTF8("main"));

            VkPipelineShaderStageCreateInfo fragStage = shaderStages.get(1);
            fragStage.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragStage.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragStage.module(fragModule);
            fragStage.pName(stack.UTF8("main"));

            // Vertex input
            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            if (bindingDesc != null)  vertexInputInfo.pVertexBindingDescriptions(bindingDesc);
            if (attrDesc != null)     vertexInputInfo.pVertexAttributeDescriptions(attrDesc);

            // Input assembly
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // Dynamic state (viewport + scissor — avoids pipeline switches on resize)
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // Rasterization
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(state.cullEnabled ? state.cullMode : VK_CULL_MODE_NONE);
            rasterizer.frontFace(state.frontFace);
            rasterizer.depthBiasEnable(state.polygonOffsetEnabled);
            if (state.polygonOffsetEnabled) {
                rasterizer.depthBiasConstantFactor(state.polygonOffsetUnits);
                rasterizer.depthBiasSlopeFactor(state.polygonOffsetFactor);
            }

            // Multisampling (disabled for mobile — saves bandwidth)
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // Depth/stencil
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(state.depthTestEnabled);
            depthStencil.depthWriteEnable(state.depthWriteEnabled);
            depthStencil.depthCompareOp(state.depthCompareOp);
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.stencilTestEnable(state.stencilTestEnabled);

            // Color blend
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(state.colorWriteMask);
            colorBlendAttachment.blendEnable(state.blendEnabled);
            if (state.blendEnabled) {
                colorBlendAttachment.srcColorBlendFactor(state.blendSrcFactor);
                colorBlendAttachment.dstColorBlendFactor(state.blendDstFactor);
                colorBlendAttachment.colorBlendOp(state.blendOp);
                colorBlendAttachment.srcAlphaBlendFactor(state.blendSrcAlphaFactor);
                colorBlendAttachment.dstAlphaBlendFactor(state.blendDstAlphaFactor);
                colorBlendAttachment.alphaBlendOp(state.blendAlphaOp);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(state.colorLogicOpEnabled);
            colorBlending.pAttachments(colorBlendAttachment);

            // Descriptor set layout (textures + uniforms)
            createDescriptorSetLayout(stack);

            // Push constants for MVP matrix
            VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
            pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
            pushConstants.offset(0);
            pushConstants.size(256); // 4x4 float matrix = 64 bytes, plus extra

            // Pipeline layout
            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));
            pipelineLayoutInfo.pPushConstantRanges(pushConstants);

            LongBuffer pLayout = stack.mallocLong(1);
            int result = vkCreatePipelineLayout(DeviceManager.getDevice(), pipelineLayoutInfo, null, pLayout);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout. VkResult: " + result);
            }
            pipelineLayout = pLayout.get(0);

            // Graphics pipeline
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicState);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.renderPass(RenderPass.getHandle());
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pPipeline = stack.mallocLong(1);
            result = vkCreateGraphicsPipelines(DeviceManager.getDevice(), pipelineCache, pipelineInfo, null, pPipeline);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline '" + name + "'. VkResult: " + result);
            }
            pipeline = pPipeline.get(0);

            // Shader modules no longer needed after pipeline creation
            vkDestroyShaderModule(DeviceManager.getDevice(), vertModule, null);
            vkDestroyShaderModule(DeviceManager.getDevice(), fragModule, null);
        }
    }

    private void createDescriptorSetLayout(MemoryStack stack) {
        // Binding 0: combined image sampler (texture atlas)
        // Binding 1: uniform buffer (matrices, fog params)
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);

        bindings.get(0).binding(0);
        bindings.get(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
        bindings.get(0).descriptorCount(8); // Up to 8 texture slots
        bindings.get(0).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

        bindings.get(1).binding(1);
        bindings.get(1).descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        bindings.get(1).descriptorCount(1);
        bindings.get(1).stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
        layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
        layoutInfo.pBindings(bindings);

        LongBuffer pLayout = stack.mallocLong(1);
        int result = vkCreateDescriptorSetLayout(DeviceManager.getDevice(), layoutInfo, null, pLayout);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor set layout. VkResult: " + result);
        }
        descriptorSetLayout = pLayout.get(0);
    }

    private long createShaderModule(ByteBuffer spirv) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirv);
            LongBuffer pModule = stack.mallocLong(1);
            int result = vkCreateShaderModule(DeviceManager.getDevice(), createInfo, null, pModule);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module. VkResult: " + result);
            }
            return pModule.get(0);
        }
    }

    private ByteBuffer loadSpirv(String path) {
        try (InputStream is = GraphicsPipeline.class.getResourceAsStream("/assets/vulkadroid/shaders/" + path)) {
            if (is == null) throw new RuntimeException("Shader not found: " + path);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    public void bind(VkCommandBuffer cmd) {
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
    }

    public void destroy() {
        if (pipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(DeviceManager.getDevice(), pipeline, null);
            pipeline = VK_NULL_HANDLE;
        }
        if (pipelineLayout != VK_NULL_HANDLE) {
            vkDestroyPipelineLayout(DeviceManager.getDevice(), pipelineLayout, null);
            pipelineLayout = VK_NULL_HANDLE;
        }
        if (descriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(DeviceManager.getDevice(), descriptorSetLayout, null);
            descriptorSetLayout = VK_NULL_HANDLE;
        }
    }

    public static void destroyCache() {
        if (pipelineCache != VK_NULL_HANDLE) {
            vkDestroyPipelineCache(DeviceManager.getDevice(), pipelineCache, null);
            pipelineCache = VK_NULL_HANDLE;
        }
        pipelineCache2.values().forEach(GraphicsPipeline::destroy);
        pipelineCache2.clear();
    }

    public long getHandle() { return pipeline; }
    public long getPipelineLayout() { return pipelineLayout; }
    public long getDescriptorSetLayout() { return descriptorSetLayout; }
    public String getName() { return name; }
}
