// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class sunctuary_boss_orion<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "sunctuary_boss_orion"), "main");
	private final ModelPart main;
	private final ModelPart body;
	private final ModelPart head_main;
	private final ModelPart head;
	private final ModelPart head_mane_main;
	private final ModelPart head_mane1;
	private final ModelPart head_mane2;
	private final ModelPart head_mane3;
	private final ModelPart hand_l;
	private final ModelPart hand_l_2;
	private final ModelPart hand_l_1;
	private final ModelPart hand_r;
	private final ModelPart hand_r_2;
	private final ModelPart sword;
	private final ModelPart hand_r_1;
	private final ModelPart leg_r;
	private final ModelPart leg_r_1;
	private final ModelPart leg_r_2;
	private final ModelPart leg_l;
	private final ModelPart leg_l_1;
	private final ModelPart leg_l_2;

	public sunctuary_boss_orion(ModelPart root) {
		this.main = root.getChild("main");
		this.body = this.main.getChild("body");
		this.head_main = this.main.getChild("head_main");
		this.head = this.head_main.getChild("head");
		this.head_mane_main = this.head.getChild("head_mane_main");
		this.head_mane1 = this.head_mane_main.getChild("head_mane1");
		this.head_mane2 = this.head_mane1.getChild("head_mane2");
		this.head_mane3 = this.head_mane2.getChild("head_mane3");
		this.hand_l = this.main.getChild("hand_l");
		this.hand_l_2 = this.hand_l.getChild("hand_l_2");
		this.hand_l_1 = this.hand_l.getChild("hand_l_1");
		this.hand_r = this.main.getChild("hand_r");
		this.hand_r_2 = this.hand_r.getChild("hand_r_2");
		this.sword = this.hand_r_2.getChild("sword");
		this.hand_r_1 = this.hand_r.getChild("hand_r_1");
		this.leg_r = this.main.getChild("leg_r");
		this.leg_r_1 = this.leg_r.getChild("leg_r_1");
		this.leg_r_2 = this.leg_r.getChild("leg_r_2");
		this.leg_l = this.main.getChild("leg_l");
		this.leg_l_1 = this.leg_l.getChild("leg_l_1");
		this.leg_l_2 = this.leg_l.getChild("leg_l_2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition body = main.addOrReplaceChild("body", CubeListBuilder.create().texOffs(66, 65).addBox(-2.5F, -2.048F, -2.833F, 5.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -33.952F, 1.833F));

		PartDefinition cube_r1 = body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(48, 30).addBox(-8.0F, -3.0F, -4.0F, 11.0F, 2.0F, 13.0F, new CubeDeformation(0.0F))
		.texOffs(0, 54).addBox(-7.0F, -4.0F, -3.0F, 9.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 8.952F, -1.833F, 0.1309F, 0.0F, 0.0F));

		PartDefinition cube_r2 = body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(72, 79).addBox(-6.5F, -3.5F, 7.5F, 10.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 72).addBox(-6.5F, -3.5F, -6.5F, 10.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(-7.5F, -4.5F, -4.5F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.5F, -6.548F, -2.333F, 0.2182F, 0.0F, 0.0F));

		PartDefinition head_main = main.addOrReplaceChild("head_main", CubeListBuilder.create(), PartPose.offset(0.0F, -44.5F, -2.0F));

		PartDefinition cube_r3 = head_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(96, 36).addBox(-1.5F, 0.5F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0031F, 0.5283F, 0.0436F, 0.0F, 0.0F));

		PartDefinition head = head_main.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -1.5F, 1.0F));

		PartDefinition cube_r4 = head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(40, 65).addBox(-3.0F, -3.5F, -3.5F, 6.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(62, 20).addBox(-3.0F, -2.5F, -4.5F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.5031F, -0.4717F, 0.0436F, 0.0F, 0.0F));

		PartDefinition cube_r5 = head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(76, 20).addBox(-3.0F, -2.5F, -0.5F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, -3.5031F, -0.4717F, 1.5708F, -1.5272F, -1.5708F));

		PartDefinition cube_r6 = head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(24, 72).addBox(-3.0F, -2.5F, -0.5F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, -3.5031F, -0.4717F, 1.5708F, -1.5272F, -1.5708F));

		PartDefinition head_mane_main = head.addOrReplaceChild("head_mane_main", CubeListBuilder.create().texOffs(90, 74).addBox(-2.0F, -0.5F, -1.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.5638F, 0.8088F));

		PartDefinition head_mane1 = head_mane_main.addOrReplaceChild("head_mane1", CubeListBuilder.create(), PartPose.offset(0.0F, -2.8402F, 2.8077F));

		PartDefinition cube_r7 = head_mane1.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(96, 79).addBox(-1.5F, -2.0F, 0.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5272F, 0.0F, 0.0F));

		PartDefinition head_mane2 = head_mane1.addOrReplaceChild("head_mane2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r8 = head_mane2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(102, 15).addBox(-1.0F, -3.0F, -2.5F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.2987F, 1.4556F, -1.8326F, 0.0F, 0.0F));

		PartDefinition head_mane3 = head_mane2.addOrReplaceChild("head_mane3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r9 = head_mane3.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(104, 98).addBox(-0.5F, -2.0F, -0.75F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.8152F, 5.6962F, -2.3562F, 0.0F, 0.0F));

		PartDefinition hand_l = main.addOrReplaceChild("hand_l", CubeListBuilder.create(), PartPose.offset(8.0F, -44.0F, 0.5F));

		PartDefinition hand_l_2 = hand_l.addOrReplaceChild("hand_l_2", CubeListBuilder.create(), PartPose.offset(4.0F, 15.0F, 3.5F));

		PartDefinition cube_r10 = hand_l_2.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(34, 91).addBox(-2.0F, -4.5F, -2.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.6167F, -0.4351F, -0.1309F, 0.0F, 0.0F));

		PartDefinition hand_l_1 = hand_l.addOrReplaceChild("hand_l_1", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, -1.0F));

		PartDefinition cube_r11 = hand_l_1.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(24, 79).addBox(-1.5F, -7.5F, -0.5F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(48, 45).addBox(-5.5F, -4.5F, -4.5F, 9.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(16, 91).addBox(-3.5F, 4.5F, -0.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -0.5F, 0.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition hand_r = main.addOrReplaceChild("hand_r", CubeListBuilder.create(), PartPose.offset(-8.0F, -44.0F, 0.5F));

		PartDefinition hand_r_2 = hand_r.addOrReplaceChild("hand_r_2", CubeListBuilder.create(), PartPose.offset(-5.0F, 15.0F, 3.5F));

		PartDefinition cube_r12 = hand_r_2.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(52, 91).addBox(-2.0F, -4.5F, -2.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.6167F, -0.4351F, -0.1309F, 0.0F, 0.0F));

		PartDefinition sword = hand_r_2.addOrReplaceChild("sword", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -1.0F, -30.0F, 1.0F, 6.0F, 24.0F, new CubeDeformation(0.0F))
		.texOffs(16, 84).addBox(-0.5F, -1.0F, -36.0F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(40, 54).addBox(-0.5F, -1.0F, -33.0F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(102, 22).addBox(-0.5F, -1.0F, -39.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(92, 15).addBox(-0.5F, -1.0F, -42.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(62, 26).addBox(-0.5F, -1.0F, -45.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(50, 20).addBox(-2.0F, -1.0F, -6.0F, 4.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(90, 63).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(0, 102).addBox(-2.0F, -1.0F, 5.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 7.0F, -1.0F));

		PartDefinition hand_r_1 = hand_r.addOrReplaceChild("hand_r_1", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 1.0F));

		PartDefinition cube_r13 = hand_r_1.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(48, 79).addBox(-32.5F, -7.5F, -0.5F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(50, 0).addBox(-29.5F, -4.5F, -4.5F, 9.0F, 8.0F, 12.0F, new CubeDeformation(0.0F))
		.texOffs(70, 91).addBox(-27.5F, 4.5F, -0.5F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.5F, -1.5F, -2.0F, 0.2182F, 0.0F, 0.0F));

		PartDefinition leg_r = main.addOrReplaceChild("leg_r", CubeListBuilder.create(), PartPose.offset(-7.0F, -26.0F, 3.0F));

		PartDefinition leg_r_1 = leg_r.addOrReplaceChild("leg_r_1", CubeListBuilder.create(), PartPose.offset(3.5F, 5.0F, -1.0F));

		PartDefinition cube_r14 = leg_r_1.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(90, 45).addBox(-5.0F, -4.0F, -3.0F, 3.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2618F, 0.0F, 0.0F));

		PartDefinition leg_r_2 = leg_r.addOrReplaceChild("leg_r_2", CubeListBuilder.create().texOffs(88, 91).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(96, 28).addBox(-3.5F, 1.0F, -1.5F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 92).addBox(-4.5F, 1.5F, -1.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, -4.0F));

		PartDefinition leg_l = main.addOrReplaceChild("leg_l", CubeListBuilder.create(), PartPose.offset(7.0F, -26.0F, 3.0F));

		PartDefinition leg_l_1 = leg_l.addOrReplaceChild("leg_l_1", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, 0.0F));

		PartDefinition cube_r15 = leg_l_1.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 84).addBox(-1.5F, 0.6055F, -2.8012F, 3.0F, 13.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2618F, 0.0F, 0.0F));

		PartDefinition leg_l_2 = leg_l.addOrReplaceChild("leg_l_2", CubeListBuilder.create().texOffs(92, 0).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 10.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(90, 20).addBox(1.5F, 1.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(104, 86).addBox(3.5F, 1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, -3.5F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}