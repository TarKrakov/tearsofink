// Made with Blockbench 5.2.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class hunter<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "hunter"), "main");
	private final ModelPart head;
	private final ModelPart gear_hat_1;
	private final ModelPart gear_hat_2;
	private final ModelPart gear_hat_3;
	private final ModelPart body;
	private final ModelPart gear_body_1;
	private final ModelPart gear_body_2;
	private final ModelPart gear_body_3;
	private final ModelPart right_arm;
	private final ModelPart gear_r_arm_1;
	private final ModelPart gear_r_arm_2;
	private final ModelPart gear_r_arm_3;
	private final ModelPart left_arm;
	private final ModelPart gear_l_arm_1;
	private final ModelPart gear_l_arm_2;
	private final ModelPart gear_l_arm_3;
	private final ModelPart right_leg;
	private final ModelPart gear_r_leg_1;
	private final ModelPart gear_r_leg_2;
	private final ModelPart gear_r_leg_3;
	private final ModelPart left_leg;
	private final ModelPart gear_l_leg_1;
	private final ModelPart gear_l_leg_2;
	private final ModelPart gear_l_leg_3;

	public hunter(ModelPart root) {
		this.head = root.getChild("head");
		this.gear_hat_1 = this.head.getChild("gear_hat_1");
		this.gear_hat_2 = this.head.getChild("gear_hat_2");
		this.gear_hat_3 = this.head.getChild("gear_hat_3");
		this.body = root.getChild("body");
		this.gear_body_1 = this.body.getChild("gear_body_1");
		this.gear_body_2 = this.body.getChild("gear_body_2");
		this.gear_body_3 = this.body.getChild("gear_body_3");
		this.right_arm = root.getChild("right_arm");
		this.gear_r_arm_1 = this.right_arm.getChild("gear_r_arm_1");
		this.gear_r_arm_2 = this.right_arm.getChild("gear_r_arm_2");
		this.gear_r_arm_3 = this.right_arm.getChild("gear_r_arm_3");
		this.left_arm = root.getChild("left_arm");
		this.gear_l_arm_1 = this.left_arm.getChild("gear_l_arm_1");
		this.gear_l_arm_2 = this.left_arm.getChild("gear_l_arm_2");
		this.gear_l_arm_3 = this.left_arm.getChild("gear_l_arm_3");
		this.right_leg = root.getChild("right_leg");
		this.gear_r_leg_1 = this.right_leg.getChild("gear_r_leg_1");
		this.gear_r_leg_2 = this.right_leg.getChild("gear_r_leg_2");
		this.gear_r_leg_3 = this.right_leg.getChild("gear_r_leg_3");
		this.left_leg = root.getChild("left_leg");
		this.gear_l_leg_1 = this.left_leg.getChild("gear_l_leg_1");
		this.gear_l_leg_2 = this.left_leg.getChild("gear_l_leg_2");
		this.gear_l_leg_3 = this.left_leg.getChild("gear_l_leg_3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_hat_1 = head.addOrReplaceChild("gear_hat_1", CubeListBuilder.create().texOffs(13, 13).addBox(5.0F, -2.0F, -5.0F, 10.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(12, 61).addBox(3.0F, 4.0F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(12, 61).addBox(3.0F, 4.0F, -3.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(13, 62).addBox(3.0F, 4.0F, -4.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 62).addBox(3.0F, 4.0F, -5.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 62).addBox(3.0F, 4.0F, -6.0F, 14.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 62).addBox(6.0F, 4.0F, -5.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 62).addBox(6.0F, 4.0F, 4.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 62).addBox(5.0F, 4.0F, -7.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 62).addBox(5.0F, 4.0F, 6.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(10, 62).addBox(3.0F, 4.0F, 5.0F, 14.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 62).addBox(3.0F, 4.0F, 4.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(13, 62).addBox(3.0F, 4.0F, 3.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(12, 61).addBox(3.0F, 4.0F, 1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(10, 9).addBox(1.0F, 0.5F, -3.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(12, 11).addBox(1.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(14, 13).addBox(2.0F, -1.5F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(6, 5).addBox(2.0F, 3.5F, -5.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(8, 7).addBox(1.5F, 2.5F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-10.0F, -9.0F, 0.0F));

		PartDefinition cube_r1 = gear_hat_1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(11, 62).addBox(-4.0F, -1.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, 5.0F, -4.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r2 = gear_hat_1.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(11, 62).addBox(-4.0F, -1.0F, 0.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 5.0F, -3.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r3 = gear_hat_1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(9, 61).addBox(-5.0F, -1.0F, -1.0F, 7.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 5.0F, -2.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r4 = gear_hat_1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0F, 4.0F, 6.0F, 0.0F, 0.0F, 0.0F));

		PartDefinition cube_r5 = gear_hat_1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, -5.0F, -6.0F, -1.5708F, 0.0F, -1.5708F));

		PartDefinition cube_r6 = gear_hat_1.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, -5.0F, 5.0F, -1.5708F, 0.0F, -1.5708F));

		PartDefinition cube_r7 = gear_hat_1.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(12, 62).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 1.0F, 6.0F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r8 = gear_hat_1.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(12, 62).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, 1.0F, -5.0F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r9 = gear_hat_1.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, -2.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r10 = gear_hat_1.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.0F, -2.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r11 = gear_hat_1.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0F, 4.0F, -5.0F, 0.0F, 0.0F, 0.0F));

		PartDefinition cube_r12 = gear_hat_1.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 4.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r13 = gear_hat_1.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(7, 62).addBox(-8.0F, -1.0F, -1.0F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 4.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r14 = gear_hat_1.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(8, 61).addBox(-6.0F, -1.0F, -1.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 5.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r15 = gear_hat_1.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(9, 61).addBox(-5.0F, -1.0F, -1.0F, 7.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 5.0F, 2.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r16 = gear_hat_1.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(11, 62).addBox(-4.0F, -1.0F, 0.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 5.0F, 4.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition cube_r17 = gear_hat_1.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(10, 62).addBox(-5.0F, -1.0F, 0.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, 5.0F, 5.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition gear_hat_2 = head.addOrReplaceChild("gear_hat_2", CubeListBuilder.create().texOffs(-9, -6).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-4.0F, -7.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-4.0F, -6.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-5.0F, -4.0F, -5.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(2.0F, -6.0F, -5.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(3.0F, -4.0F, -5.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(2.0F, -7.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-9, -6).addBox(5.0F, -4.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(-9, -6).addBox(-6.0F, -4.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(-11, -8).addBox(-6.0F, -1.0F, -5.0F, 2.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(-12, -8).addBox(4.0F, -1.0F, -5.0F, 2.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(-5, 0).addBox(3.0F, 0.0F, -4.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(-4, 1).addBox(3.0F, 1.0F, -3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-4, 1).addBox(-5.0F, 1.0F, -3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-5, 0).addBox(-6.0F, 0.0F, -4.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(-9, -6).addBox(-5.0F, -8.0F, -4.0F, 1.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(-9, -6).addBox(4.0F, -8.0F, -4.0F, 1.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r18 = gear_hat_2.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(3, -1).addBox(-5.0F, -5.0F, 1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(3, -1).addBox(4.0F, -5.0F, 1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r19 = gear_hat_2.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.0F, -1.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r20 = gear_hat_2.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, -1.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r21 = gear_hat_2.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition cube_r22 = gear_hat_2.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(-8, -5).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition gear_hat_3 = head.addOrReplaceChild("gear_hat_3", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-4.0F, -7.0F, 4.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r23 = gear_hat_3.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r24 = gear_hat_3.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(-10, -6).addBox(-5.0F, -1.0F, -4.0F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r25 = gear_hat_3.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(-10, -6).addBox(-5.0F, -1.0F, -4.0F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -7.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r26 = gear_hat_3.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(-3, 0).addBox(-4.0F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -8.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r27 = gear_hat_3.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -5.0F, -6.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r28 = gear_hat_3.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -5.0F, -1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r29 = gear_hat_3.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -5.0F, -6.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r30 = gear_hat_3.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r31 = gear_hat_3.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r32 = gear_hat_3.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -7.0F, 1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r33 = gear_hat_3.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -7.0F, 1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r34 = gear_hat_3.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -5.0F, -1.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r35 = gear_hat_3.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, -5.0F, 0.0F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_body_1 = body.addOrReplaceChild("gear_body_1", CubeListBuilder.create().texOffs(1, 1).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-4.0F, 8.0F, 2.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-4.0F, 10.0F, 2.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-4.0F, 10.0F, -3.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r36 = gear_body_1.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(3, 1).addBox(1.0F, -2.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 1).addBox(1.0F, -4.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 12.0F, -3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r37 = gear_body_1.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(3, 1).addBox(1.0F, -2.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(3, 1).addBox(1.0F, -4.0F, 0.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 12.0F, -3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition gear_body_2 = body.addOrReplaceChild("gear_body_2", CubeListBuilder.create().texOffs(1, 1).addBox(-3.0F, 1.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-2.0F, 2.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.0F, 3.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(3.0F, 8.0F, -3.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(4.0F, 8.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(4.0F, 9.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-5.0F, 8.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-5.0F, 9.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, 3.0F, -3.0F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition cube_r38 = gear_body_2.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(-6, -6).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 10.0F, 3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r39 = gear_body_2.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(-6, -6).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 10.0F, -2.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r40 = gear_body_2.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(-2, -2).addBox(-1.0F, -1.0F, 3.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 9.0F, -2.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r41 = gear_body_2.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(1, 1).addBox(-1.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 8.0F, 3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r42 = gear_body_2.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(1, 1).addBox(-1.0F, -7.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 8.0F, 3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r43 = gear_body_2.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(-6, -6).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 9.0F, 3.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition gear_body_3 = body.addOrReplaceChild("gear_body_3", CubeListBuilder.create().texOffs(-2, 1).addBox(-4.0F, 9.0F, -3.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-1.0F, 1.0F, 2.0F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-4.0F, 1.0F, 2.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(3.0F, 1.0F, 2.0F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-1.0F, 1.0F, -3.0F, 2.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, 1).addBox(-4.0F, 9.0F, 2.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, -2).addBox(-5.0F, 9.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, -2).addBox(4.0F, 9.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-6.0F, 2.0F, 0.0F));

		PartDefinition gear_r_arm_1 = right_arm.addOrReplaceChild("gear_r_arm_1", CubeListBuilder.create().texOffs(-1, -2).addBox(-4.0F, 1.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-1, -2).addBox(1.0F, 1.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r44 = gear_r_arm_1.addOrReplaceChild("cube_r44", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 3.0F, 3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r45 = gear_r_arm_1.addOrReplaceChild("cube_r45", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 3.0F, -2.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition gear_r_arm_2 = right_arm.addOrReplaceChild("gear_r_arm_2", CubeListBuilder.create().texOffs(1, 0).addBox(-4.0F, 1.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(-4.0F, 5.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_r_arm_3 = right_arm.addOrReplaceChild("gear_r_arm_3", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, 0.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-3, -4).addBox(-4.0F, -2.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-3.0F, -2.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-3.0F, -2.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-5.0F, 7.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-4.0F, 4.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-4.0F, 3.0F, 1.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r46 = gear_r_arm_3.addOrReplaceChild("cube_r46", CubeListBuilder.create().texOffs(-3, -4).addBox(0.0F, -2.0F, -3.0F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -3.0F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(24, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(6.0F, 2.0F, 0.0F));

		PartDefinition gear_l_arm_1 = left_arm.addOrReplaceChild("gear_l_arm_1", CubeListBuilder.create().texOffs(-1, -2).addBox(3.0F, 1.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-1, -2).addBox(-2.0F, 1.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r47 = gear_l_arm_1.addOrReplaceChild("cube_r47", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 3.0F, 3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r48 = gear_l_arm_1.addOrReplaceChild("cube_r48", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -2.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 3.0F, -2.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition gear_l_arm_2 = left_arm.addOrReplaceChild("gear_l_arm_2", CubeListBuilder.create().texOffs(-2, -2).addBox(3.0F, 1.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-2.0F, 1.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.0F, 1.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.0F, 1.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(3.0F, 9.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.0F, 9.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-2.0F, 9.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(2, 1).addBox(-1.0F, 9.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(3.0F, 2.0F, 0.0F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_l_arm_3 = left_arm.addOrReplaceChild("gear_l_arm_3", CubeListBuilder.create().texOffs(1, 1).addBox(0.0F, -2.0F, -3.0F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-1.0F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, -2.0F, 2.0F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, 5.0F, 2.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r49 = gear_l_arm_3.addOrReplaceChild("cube_r49", CubeListBuilder.create().texOffs(-2, -2).addBox(-1.0F, -1.0F, 0.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.0F, 7.0F, 0.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.0F, 0.0F, 0.0F, 4.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -2.0F, -1.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-2.15F, 12.0F, 0.0F));

		PartDefinition gear_r_leg_1 = right_leg.addOrReplaceChild("gear_r_leg_1", CubeListBuilder.create().texOffs(0, -1).addBox(-3.0F, 8.0F, 0.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(-3.0F, 9.0F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(-3.0F, 7.0F, 1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(-3.0F, 6.0F, 2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r50 = gear_r_leg_1.addOrReplaceChild("cube_r50", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -1.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, 3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r51 = gear_r_leg_1.addOrReplaceChild("cube_r51", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -1.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, -2.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition gear_r_leg_2 = right_leg.addOrReplaceChild("gear_r_leg_2", CubeListBuilder.create().texOffs(0, 1).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-2.0F, 10.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, -2).addBox(-3.0F, 10.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-2.0F, 7.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 1).addBox(-2.0F, 7.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, -2).addBox(-3.0F, 7.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_r_leg_3 = right_leg.addOrReplaceChild("gear_r_leg_3", CubeListBuilder.create().texOffs(-1, 1).addBox(-2.0F, 1.0F, -3.0F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 1.0F, 2.0F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-3.0F, 1.0F, -2.0F, 1.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 9.0F, 2.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 9.0F, -3.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(-3.0F, 9.0F, -2.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(2.15F, 12.0F, 0.0F));

		PartDefinition gear_l_leg_1 = left_leg.addOrReplaceChild("gear_l_leg_1", CubeListBuilder.create().texOffs(1, 0).addBox(2.0F, 7.0F, 1.5F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, -1).addBox(2.0F, 8.0F, 0.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(2.0F, 6.0F, 2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(1, 0).addBox(2.0F, 9.0F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r52 = gear_l_leg_1.addOrReplaceChild("cube_r52", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -1.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, 3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition cube_r53 = gear_l_leg_1.addOrReplaceChild("cube_r53", CubeListBuilder.create().texOffs(-1, -2).addBox(0.0F, -1.0F, 1.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 10.0F, -2.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition gear_l_leg_2 = left_leg.addOrReplaceChild("gear_l_leg_2", CubeListBuilder.create().texOffs(1, 1).addBox(-2.0F, 3.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-2.0F, 3.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(2.0F, 3.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-2.0F, 8.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-2.0F, 8.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-2, -2).addBox(2.0F, 8.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition gear_l_leg_3 = left_leg.addOrReplaceChild("gear_l_leg_3", CubeListBuilder.create().texOffs(-1, 1).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 10.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-4, -2).addBox(2.0F, 10.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 5.0F, -3.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-1, 1).addBox(-2.0F, 5.0F, 2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(-4, -2).addBox(2.0F, 5.0F, -2.0F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}