// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class blood_pump<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "blood_pump"), "main");
	private final ModelPart roof;
	private final ModelPart piston;
	private final ModelPart base;

	public blood_pump(ModelPart root) {
		this.roof = root.getChild("roof");
		this.piston = root.getChild("piston");
		this.base = root.getChild("base");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition roof = partdefinition.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -4.0F, -8.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition piston = partdefinition.addOrReplaceChild("piston", CubeListBuilder.create().texOffs(32, 52).addBox(-2.0F, -18.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.0F));

		PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(-4.0F, -14.0F, -16.0F, 8.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(48, 52).addBox(-11.0F, -14.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F))
		.texOffs(64, 0).addBox(-11.0F, -14.0F, -6.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F))
		.texOffs(56, 52).addBox(-11.0F, -14.0F, 4.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		roof.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		piston.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}