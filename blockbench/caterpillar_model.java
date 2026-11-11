// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class caterpillar<T extends CaterpillarEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "caterpillar"), "main");
	private final ModelPart head;
	private final ModelPart segment0;
	private final ModelPart segment1;
	private final ModelPart segment2;
	private final ModelPart segment3;
	private final ModelPart segment4;

	public caterpillar(ModelPart root) {
		this.head = root.getChild("head");
		this.segment0 = root.getChild("segment0");
		this.segment1 = root.getChild("segment1");
		this.segment2 = root.getChild("segment2");
		this.segment3 = root.getChild("segment3");
		this.segment4 = root.getChild("segment4");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 17).addBox(-1.0F, -3.0F, -2.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, -6.0F));

		PartDefinition segment0 = partdefinition.addOrReplaceChild("segment0", CubeListBuilder.create().texOffs(14, 10).addBox(-2.0F, -4.0F, -1.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 24.0F, -4.0F));

		PartDefinition segment1 = partdefinition.addOrReplaceChild("segment1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -6.0F, -1.0F, 5.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 24.0F, -1.0F));

		PartDefinition segment2 = partdefinition.addOrReplaceChild("segment2", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -5.0F, -0.75F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 2.75F));

		PartDefinition segment3 = partdefinition.addOrReplaceChild("segment3", CubeListBuilder.create().texOffs(0, 18).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 24.0F, 5.5F));

		PartDefinition segment4 = partdefinition.addOrReplaceChild("segment4", CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, -2.0F, 1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 24.0F, 6.5F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(CaterpillarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		segment0.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		segment1.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		segment2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		segment3.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		segment4.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}