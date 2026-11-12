// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pale_soldier_spearman<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pale_soldier_spearman"), "main");
	private final ModelPart hand_r;
	private final ModelPart hand_r2;
	private final ModelPart hand_r3;
	private final ModelPart spear;
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
	private final ModelPart bb_main;

	public pale_soldier_spearman(ModelPart root) {
		this.hand_r = root.getChild("hand_r");
		this.hand_r2 = this.hand_r.getChild("hand_r2");
		this.hand_r3 = this.hand_r.getChild("hand_r3");
		this.spear = this.hand_r3.getChild("spear");
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
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(-4, -2).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, 4.0F, -8.0F));

		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition spear = hand_r3.addOrReplaceChild("spear", CubeListBuilder.create().texOffs(2, 1).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 31.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.5F, 21.0F, -0.5F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.5F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(-4, -2).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, 4.0F, -8.0F));

		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(-2, -1).addBox(-1.75F, 0.0F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 10.0F, -8.0F));

		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(-2, -1).addBox(-1.75F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 18.0F, -7.5F));

		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(-2, -1).addBox(-1.25F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.5F, 18.0F, -7.5F));

		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(-2, -1).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.5F, 11.0F, -8.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(-12, -6).addBox(-4.0F, -5.5F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 5.5F, -8.0F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(-2, -2).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.5F, 6.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(-8, -4).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(-20, -6).addBox(-8.0F, -32.0F, 22.0F, 16.0F, 32.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

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
		bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}