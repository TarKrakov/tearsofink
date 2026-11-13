// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pale_soldier_crossbowman<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pale_soldier_crossbowman"), "main");
	private final ModelPart hand_r;
	private final ModelPart hand_r2;
	private final ModelPart hand_r3;
	private final ModelPart crossbow;
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

	public pale_soldier_crossbowman(ModelPart root) {
		this.hand_r = root.getChild("hand_r");
		this.hand_r2 = this.hand_r.getChild("hand_r2");
		this.hand_r3 = this.hand_r.getChild("hand_r3");
		this.crossbow = this.hand_r3.getChild("crossbow");
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

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(-3, -2).addBox(-1.0F, -2.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 4.0F, -2.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition crossbow = hand_r3.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(-9, -8).addBox(-3.0F, 2.0F, -3.5F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(-9, -8).addBox(-3.0F, 3.0F, -3.5F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(-10, -8).addBox(0.0F, 3.0F, -3.5F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.5F, -1.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.5F, -0.5F, 1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r1 = crossbow.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(-3, -2).addBox(-5.0F, 4.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(-3, -2).addBox(2.0F, 4.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

		PartDefinition cube_r2 = crossbow.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(-9, -8).addBox(1.0F, 4.0F, -6.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(-5, -2).addBox(-4.0F, -2.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 4.0F, -2.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.75F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 10.0F, -1.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.75F, 0.0F, -1.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 18.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.25F, 0.0F, -1.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 18.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 11.0F, -1.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(-9, -5).addBox(-2.0F, -5.5F, -3.0F, 4.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, -1.0F, 2.8362F, 0.0F, 3.1416F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(-2, -2).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 6.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition cube_r3 = head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(-6, -3).addBox(-2.5F, -2.0F, 0.75F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -3.0F, 0.0436F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
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