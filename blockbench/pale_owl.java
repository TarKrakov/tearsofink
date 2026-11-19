// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pale_owl<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pale_owl"), "main");
	private final ModelPart Owl_main;
	private final ModelPart owl_body;
	private final ModelPart Owl_wing_l;
	private final ModelPart Owl_wing_r;
	private final ModelPart Owl_leg_l;
	private final ModelPart Owl_leg_r;

	public pale_owl(ModelPart root) {
		this.Owl_main = root.getChild("Owl_main");
		this.owl_body = this.Owl_main.getChild("owl_body");
		this.Owl_wing_l = this.Owl_main.getChild("Owl_wing_l");
		this.Owl_wing_r = this.Owl_main.getChild("Owl_wing_r");
		this.Owl_leg_l = this.Owl_main.getChild("Owl_leg_l");
		this.Owl_leg_r = this.Owl_main.getChild("Owl_leg_r");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Owl_main = partdefinition.addOrReplaceChild("Owl_main", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 17.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition owl_body = Owl_main.addOrReplaceChild("owl_body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.7667F, -7.8333F, 9.0F, 12.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(32, 29).addBox(-1.0F, -1.8667F, 1.1667F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-0.5F, 0.1333F, 1.1667F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -1.2333F, 2.8333F));

		PartDefinition Owl_wing_l = Owl_main.addOrReplaceChild("Owl_wing_l", CubeListBuilder.create(), PartPose.offset(-4.0F, -2.0F, -0.5F));

		PartDefinition cube_r1 = Owl_wing_l.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 21).addBox(-0.5F, -4.5F, -3.5F, 1.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.5F, 2.5F, 0.0F, 0.0F, 0.0F, 0.0F));

		PartDefinition Owl_wing_r = Owl_main.addOrReplaceChild("Owl_wing_r", CubeListBuilder.create().texOffs(0, 21).addBox(1.0F, -2.0F, -3.5F, 1.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -2.0F, -0.5F));

		PartDefinition Owl_leg_l = Owl_main.addOrReplaceChild("Owl_leg_l", CubeListBuilder.create().texOffs(32, 25).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 32).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 7.0F, 0.0F));

		PartDefinition Owl_leg_r = Owl_main.addOrReplaceChild("Owl_leg_r", CubeListBuilder.create().texOffs(32, 21).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 35).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 7.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Owl_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}