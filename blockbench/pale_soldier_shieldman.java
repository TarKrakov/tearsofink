// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pale_soldier_shieldman<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pale_soldier_shieldman"), "main");
	private final ModelPart hand_r;
	private final ModelPart hand_r2;
	private final ModelPart hand_r3;
	private final ModelPart Shield;
	private final ModelPart hand_l;
	private final ModelPart hand_l2;
	private final ModelPart hand_l3;
	private final ModelPart leg_r;
	private final ModelPart leg_r2;
	private final ModelPart leg_l2;
	private final ModelPart leg_l;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;

	public pale_soldier_shieldman(ModelPart root) {
		this.hand_r = root.getChild("hand_r");
		this.hand_r2 = this.hand_r.getChild("hand_r2");
		this.hand_r3 = this.hand_r.getChild("hand_r3");
		this.Shield = this.hand_r3.getChild("Shield");
		this.hand_l = root.getChild("hand_l");
		this.hand_l2 = this.hand_l.getChild("hand_l2");
		this.hand_l3 = this.hand_l.getChild("hand_l3");
		this.leg_r = root.getChild("leg_r");
		this.leg_r2 = root.getChild("leg_r2");
		this.leg_l2 = root.getChild("leg_l2");
		this.leg_l = root.getChild("leg_l");
		this.body = root.getChild("body");
		this.neck = this.body.getChild("neck");
		this.head = this.neck.getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(0, 40).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 2.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(56, 42).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 2.0F, -1.0F));

		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(56, 52).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 10.0F, -1.0F));

		PartDefinition Shield = hand_r3.addOrReplaceChild("Shield", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -16.0F, -7.0F, 3.0F, 25.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(36, 32).addBox(-4.0F, -5.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 1.5F, 1.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(20, 46).addBox(-2.0F, -5.0F, -3.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 4.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(58, 20).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 0.0F, -1.0F));

		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(30, 58).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 8.0F, -1.0F));

		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(54, 32).addBox(-2.25F, 0.0F, -1.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 10.0F, 0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(40, 46).addBox(-2.25F, -1.0F, -3.5F, 3.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 18.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(0, 50).addBox(-1.25F, -1.0F, -3.5F, 3.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 18.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(16, 56).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 11.0F, 0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(36, 0).addBox(-5.0F, -6.5F, -4.0F, 9.0F, 11.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(20, 40).addBox(-2.0F, -3.0F, -3.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.5F, 1.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 20).addBox(-3.0F, -4.5F, -2.25F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		hand_r.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		hand_l.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_r.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_r2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_l2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg_l.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}