// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class pale_deer<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "pale_deer"), "main");
	private final ModelPart pale_deer_main;
	private final ModelPart pale_deer_neck;
	private final ModelPart pale_deer_head;
	private final ModelPart pale_deer_face;
	private final ModelPart pale_deer_leg_l;
	private final ModelPart pale_deer_leg_l2;
	private final ModelPart pale_deer_leg_r;
	private final ModelPart pale_deer_leg_r2;
	private final ModelPart pale_deer_body;
	private final ModelPart pale_deer_tail;

	public pale_deer(ModelPart root) {
		this.pale_deer_main = root.getChild("pale_deer_main");
		this.pale_deer_neck = this.pale_deer_main.getChild("pale_deer_neck");
		this.pale_deer_head = this.pale_deer_neck.getChild("pale_deer_head");
		this.pale_deer_face = this.pale_deer_head.getChild("pale_deer_face");
		this.pale_deer_leg_l = this.pale_deer_main.getChild("pale_deer_leg_l");
		this.pale_deer_leg_l2 = this.pale_deer_main.getChild("pale_deer_leg_l2");
		this.pale_deer_leg_r = this.pale_deer_main.getChild("pale_deer_leg_r");
		this.pale_deer_leg_r2 = this.pale_deer_main.getChild("pale_deer_leg_r2");
		this.pale_deer_body = this.pale_deer_main.getChild("pale_deer_body");
		this.pale_deer_tail = this.pale_deer_main.getChild("pale_deer_tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition pale_deer_main = partdefinition.addOrReplaceChild("pale_deer_main", CubeListBuilder.create(), PartPose.offset(-0.2857F, 10.0F, -3.7857F));

		PartDefinition pale_deer_neck = pale_deer_main.addOrReplaceChild("pale_deer_neck", CubeListBuilder.create().texOffs(42, 33).addBox(-4.4667F, 1.6833F, -1.3333F, 9.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7476F, -28.6833F, -15.881F));

		PartDefinition pale_deer_head = pale_deer_neck.addOrReplaceChild("pale_deer_head", CubeListBuilder.create().texOffs(42, 50).addBox(-3.5F, -1.875F, -4.0F, 7.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0333F, -4.4417F, -0.3333F));

		PartDefinition cube_r1 = pale_deer_head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(8, 73).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, 4.375F, -2.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r2 = pale_deer_head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 73).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 4.375F, -2.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r3 = pale_deer_head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(72, 72).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.5F, -9.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r4 = pale_deer_head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(72, 67).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.5F, -7.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r5 = pale_deer_head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(72, 30).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, -5.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r6 = pale_deer_head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(68, 56).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.5F, -5.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r7 = pale_deer_head.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(72, 41).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.0F, -7.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r8 = pale_deer_head.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(72, 46).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.5F, -9.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r9 = pale_deer_head.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(72, 36).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 1.375F, 0.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r10 = pale_deer_head.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(68, 62).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 1.375F, 0.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r11 = pale_deer_head.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(68, 50).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, -1.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r12 = pale_deer_head.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(68, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, -1.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r13 = pale_deer_head.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(68, 8).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.5F, -3.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r14 = pale_deer_head.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(68, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5F, -3.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition pale_deer_face = pale_deer_head.addOrReplaceChild("pale_deer_face", CubeListBuilder.create(), PartPose.offset(0.0F, 1.875F, -8.0F));

		PartDefinition cube_r15 = pale_deer_face.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 55).addBox(-2.5F, -5.5F, -3.5F, 5.0F, 11.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition pale_deer_leg_l = pale_deer_main.addOrReplaceChild("pale_deer_leg_l", CubeListBuilder.create().texOffs(48, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7857F, 1.0F, -9.7143F));

		PartDefinition pale_deer_leg_l2 = pale_deer_main.addOrReplaceChild("pale_deer_leg_l2", CubeListBuilder.create().texOffs(24, 55).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7857F, 1.0F, 9.2857F));

		PartDefinition pale_deer_leg_r = pale_deer_main.addOrReplaceChild("pale_deer_leg_r", CubeListBuilder.create().texOffs(60, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.2143F, 1.0F, -9.7143F));

		PartDefinition pale_deer_leg_r2 = pale_deer_main.addOrReplaceChild("pale_deer_leg_r2", CubeListBuilder.create().texOffs(36, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.2143F, 1.0F, 9.2857F));

		PartDefinition pale_deer_body = pale_deer_main.addOrReplaceChild("pale_deer_body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -7.0F, -11.25F, 12.0F, 11.0F, 22.0F, new CubeDeformation(0.0F))
		.texOffs(0, 33).addBox(-4.0F, -3.0F, -6.25F, 8.0F, 9.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7143F, -17.0F, 0.0357F));

		PartDefinition pale_deer_tail = pale_deer_main.addOrReplaceChild("pale_deer_tail", CubeListBuilder.create().texOffs(68, 16).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7143F, -20.5F, 12.7857F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		pale_deer_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}