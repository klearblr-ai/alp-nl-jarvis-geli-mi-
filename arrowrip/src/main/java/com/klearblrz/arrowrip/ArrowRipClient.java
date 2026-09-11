package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ArrowRipClient implements ClientModInitializer {
    private static KeyBinding pullKey, stabKey, biteKey;
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("arrowrip", "main"));
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x7A0202, 1.15f);
    private static final DustParticleEffect DARK_BLOOD = new DustParticleEffect(0x380000, 1.35f);

    private static int holdTicks, animationTicks, visualArrowCount, bleedTicks, dripCooldown;
    private static int groundBloodTicks;
    private static double groundBloodX, groundBloodY, groundBloodZ;
    private static int suppressedArrowServerCount = -1;
    private static int throwAnimationTicks;
    private static Vec3d savedThrowLook = new Vec3d(0,0,1);
    private static ArrowEntity bodyArrowEntity;
    private static ArrowEntity groundArrowEntity;

    private static UUID stabbedTargetUuid;
    private static ItemEntity stabbedWeaponEntity;
    private static int stabbedWeaponTicks, stabbedBloodCooldown;
    private static float stabbedYawOffset;

    private static UUID biteTargetUuid;
    private static int biteTicks, biteBloodCooldown;
    private static float biteOldYaw, biteOldPitch;
    private static boolean bitePoseActive;

    @Override public void onInitializeClient() {
        pullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arrowrip.pull", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
        stabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arrowrip.stab", InputUtil.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY));
        biteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arrowrip.bite", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(ArrowRipClient::tick);
    }

    private static Vec3d pos(Entity e) { return new Vec3d(e.getX(), e.getY(), e.getZ()); }

    private static void tick(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (p == null || client.world == null) { reset(); return; }

        int tracked = p.getStuckArrowCount();
        if (suppressedArrowServerCount >= 0) {
            if (tracked == suppressedArrowServerCount || tracked == visualArrowCount) {
                p.setStuckArrowCount(visualArrowCount);
            } else {
                visualArrowCount = tracked;
                suppressedArrowServerCount = -1;
            }
        } else {
            visualArrowCount = tracked;
        }
        syncBodyArrowVisual(client,p);

        if (animationTicks > 0) { animationTicks--; animatePull(client,p,animationTicks); }
        if (throwAnimationTicks > 0) {
            throwAnimationTicks--;
            animateThrow(client,p,throwAnimationTicks);
            if (throwAnimationTicks == 0) dropBloodyArrow(client,p,savedThrowLook);
        }
        if (bleedTicks > 0) bleedTicks--;
        if (dripCooldown > 0) dripCooldown--;
        if ((visualArrowCount > 0 || bleedTicks > 0) && dripCooldown <= 0) {
            spawnBloodDrip(client,p,visualArrowCount > 0 ? 3 : 2);
            dripCooldown = visualArrowCount > 0 ? 4 + client.world.random.nextInt(5) : 7 + client.world.random.nextInt(7);
        }
        if (groundBloodTicks > 0) { groundBloodTicks--; if (groundBloodTicks % 2 == 0) spawnGroundBlood(client); }

        tickStabbedWeapon(client);
        tickDemonBite(client,p);

        while (stabKey.wasPressed()) tryStab(client,p);
        while (biteKey.wasPressed()) tryBite(client,p);

        if (pullKey.isPressed() && visualArrowCount > 0 && throwAnimationTicks <= 0) {
            holdTicks++;
            if (holdTicks == 1 || holdTicks == 6 || holdTicks == 12) p.swingHand(Hand.MAIN_HAND);
            if (holdTicks == 12) p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER,0.16f,1.25f);
            if (holdTicks >= 20) {
                int before = Math.max(1, p.getStuckArrowCount());
                visualArrowCount = Math.max(0, visualArrowCount - 1);
                suppressedArrowServerCount = before;
                p.setStuckArrowCount(visualArrowCount);
                syncBodyArrowVisual(client,p);
                animationTicks=16;
                bleedTicks=Math.max(bleedTicks,100);
                holdTicks=0;
                savedThrowLook = p.getRotationVec(1f);
                throwAnimationTicks=10;
                ripArrow(client,p);
            }
        } else holdTicks=0;
    }

    private static void syncBodyArrowVisual(MinecraftClient client, PlayerEntity p) {
        if (visualArrowCount <= 0) {
            if (bodyArrowEntity != null) bodyArrowEntity.discard();
            bodyArrowEntity = null;
            return;
        }
        double yaw = Math.toRadians(p.getYaw());
        double sideX = Math.cos(yaw) * 0.24;
        double sideZ = Math.sin(yaw) * 0.24;
        double x = p.getX() + sideX;
        double y = p.getY() + p.getHeight() * 0.62;
        double z = p.getZ() + sideZ;
        if (bodyArrowEntity == null || !bodyArrowEntity.isAlive()) {
            bodyArrowEntity = new ArrowEntity(client.world,x,y,z,new ItemStack(Items.ARROW),null);
            bodyArrowEntity.setNoGravity(true);
            bodyArrowEntity.setVelocity(Vec3d.ZERO);
            client.world.addEntity(bodyArrowEntity);
        }
        bodyArrowEntity.setPosition(x,y,z);
        bodyArrowEntity.setVelocity(Vec3d.ZERO);
        bodyArrowEntity.setYaw(p.getYaw()+90f);
        bodyArrowEntity.setPitch(4f);
    }

    private static void tryStab(MinecraftClient client, PlayerEntity attacker) {
        if (!(client.targetedEntity instanceof PlayerEntity target) || target == attacker || attacker.distanceTo(target) > 4.5f) return;
        ItemStack held = attacker.getMainHandStack();
        String id = Registries.ITEM.getId(held.getItem()).getPath();
        boolean sword = id.endsWith("_sword");
        boolean spear = id.contains("spear") || id.contains("trident");
        if (!sword && !spear) return;
        attacker.swingHand(Hand.MAIN_HAND);
        lodgeWeapon(client,attacker,target,held,spear);
    }

    private static void lodgeWeapon(MinecraftClient client, PlayerEntity attacker, PlayerEntity target, ItemStack stack, boolean spear) {
        clearStabVisual();
        stabbedTargetUuid=target.getUuid(); stabbedWeaponTicks=spear?110:95; stabbedBloodCooldown=0; stabbedYawOffset=target.getYaw();
        double y=target.getY()+target.getHeight()*0.47;
        stabbedWeaponEntity=new ItemEntity(client.world,target.getX(),y,target.getZ(),stack.copyWithCount(1));
        stabbedWeaponEntity.setPickupDelayInfinite(); stabbedWeaponEntity.setNoGravity(true); stabbedWeaponEntity.setVelocity(Vec3d.ZERO);
        stabbedWeaponEntity.setYaw(stabbedYawOffset+90f); stabbedWeaponEntity.setPitch(0f); client.world.addEntity(stabbedWeaponEntity);
        target.playSound(SoundEvents.ENTITY_PLAYER_HURT,0.62f,0.72f); target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.36f,0.56f);
        spawnTargetBlood(client,target,spear?38:30);
    }

    private static void tickStabbedWeapon(MinecraftClient client) {
        if (stabbedWeaponTicks<=0 || stabbedTargetUuid==null || stabbedWeaponEntity==null || client.world==null) { if(stabbedWeaponTicks<=0) clearStabVisual(); return; }
        Entity e=client.world.getPlayerByUuid(stabbedTargetUuid); if(!(e instanceof PlayerEntity target)||!target.isAlive()){clearStabVisual();return;}
        stabbedWeaponTicks--; if(stabbedBloodCooldown>0) stabbedBloodCooldown--;
        stabbedWeaponEntity.setPosition(target.getX(),target.getY()+target.getHeight()*0.47,target.getZ());
        stabbedWeaponEntity.setVelocity(Vec3d.ZERO); stabbedWeaponEntity.setYaw(stabbedYawOffset+90f); stabbedWeaponEntity.setPitch(0f);
        if(stabbedBloodCooldown<=0){spawnAbdomenDrip(client,target,5);stabbedBloodCooldown=3+client.world.random.nextInt(4);}
        if(stabbedWeaponTicks==1) spawnTargetBlood(client,target,14);
    }

    private static void tryBite(MinecraftClient client, PlayerEntity demon) {
        if (!(client.targetedEntity instanceof PlayerEntity target) || target == demon || demon.distanceTo(target) > 2.2f) return;
        biteTargetUuid = target.getUuid(); biteTicks = 72; biteBloodCooldown = 0;
        biteOldYaw = demon.getYaw(); biteOldPitch = demon.getPitch(); bitePoseActive = true;
        faceNeck(demon,target,22f);
        demon.setSneaking(true);
        demon.getHungerManager().add(2, 0.6f);
        target.playSound(SoundEvents.ENTITY_PLAYER_HURT,0.58f,0.52f);
        target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.52f,0.42f);
        spawnBiteBurst(client,target,demon,42);
    }

    private static void tickDemonBite(MinecraftClient client, PlayerEntity demon) {
        if (biteTicks <= 0 || biteTargetUuid == null || client.world == null) { endBitePose(demon); return; }
        Entity e = client.world.getPlayerByUuid(biteTargetUuid);
        if (!(e instanceof PlayerEntity target) || !target.isAlive() || demon.distanceTo(target) > 3.0f) { biteTicks=0; biteTargetUuid=null; endBitePose(demon); return; }

        biteTicks--; if (biteBloodCooldown > 0) biteBloodCooldown--;
        faceNeck(demon,target,18f + (float)Math.sin(biteTicks*0.55)*4f);
        demon.setSneaking(true);

        if (biteTicks == 64 || biteTicks == 60) {
            demon.swingHand(Hand.MAIN_HAND);
            target.playSound(SoundEvents.ENTITY_PLAYER_HURT,0.46f,0.48f);
            spawnBiteBurst(client,target,demon,biteTicks==64?34:22);
        }
        if (biteBloodCooldown <= 0) {
            spawnNeckBlood(client,target,6);
            spawnBloodStreamToDemon(client,target,demon,9);
            biteBloodCooldown = 2;
        }
        if (biteTicks % 12 == 0) {
            demon.getHungerManager().add(1, 0.45f);
            target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.16f,0.54f);
        }
        if (biteTicks == 1) {
            demon.getHungerManager().add(2, 0.8f);
            spawnNeckBlood(client,target,18);
            endBitePose(demon);
        }
    }

    private static void faceNeck(PlayerEntity demon, PlayerEntity target, float pitch) {
        Vec3d d = new Vec3d(target.getX()-demon.getX(), (target.getY()+target.getHeight()*0.78)-demon.getEyeY(), target.getZ()-demon.getZ());
        double horizontal = Math.sqrt(d.x*d.x + d.z*d.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(-d.x,d.z)));
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(d.y,Math.max(0.001,horizontal))));
        demon.setYaw(yaw);
        demon.setPitch(targetPitch + pitch);
    }

    private static void endBitePose(PlayerEntity demon) {
        if (bitePoseActive && demon != null) {
            demon.setYaw(biteOldYaw); demon.setPitch(biteOldPitch); demon.setSneaking(false);
        }
        bitePoseActive=false; biteTicks=0; biteTargetUuid=null; biteBloodCooldown=0;
    }

    private static void spawnBiteBurst(MinecraftClient c, PlayerEntity target, PlayerEntity demon, int n) {
        Vec3d from = new Vec3d(target.getX(), target.getY()+target.getHeight()*0.79, target.getZ());
        Vec3d toward = new Vec3d(demon.getX()-target.getX(), demon.getEyeY()-(target.getY()+target.getHeight()*0.79), demon.getZ()-target.getZ());
        if (toward.lengthSquared()<0.0001) toward=new Vec3d(0,0,1);
        toward=toward.normalize();
        for(int i=0;i<n;i++) {
            double speed=.035+c.world.random.nextDouble()*.09;
            c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,
                    from.x+(c.world.random.nextDouble()-.5)*.16,
                    from.y+(c.world.random.nextDouble()-.5)*.12,
                    from.z+(c.world.random.nextDouble()-.5)*.16,
                    toward.x*speed+(c.world.random.nextDouble()-.5)*.045,
                    toward.y*speed-.015+c.world.random.nextDouble()*.035,
                    toward.z*speed+(c.world.random.nextDouble()-.5)*.045);
        }
    }

    private static void spawnNeckBlood(MinecraftClient c, PlayerEntity t, int n) {
        double y=t.getY()+t.getHeight()*0.79;
        for(int i=0;i<n;i++) c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,t.getX()+(c.world.random.nextDouble()-.5)*.20,y+(c.world.random.nextDouble()-.5)*.16,t.getZ()+(c.world.random.nextDouble()-.5)*.20,(c.world.random.nextDouble()-.5)*.025,-.025-c.world.random.nextDouble()*.025,(c.world.random.nextDouble()-.5)*.025);
    }

    private static void spawnBloodStreamToDemon(MinecraftClient c, PlayerEntity target, PlayerEntity demon, int n) {
        Vec3d from = new Vec3d(target.getX(), target.getY()+target.getHeight()*0.79, target.getZ());
        Vec3d to = new Vec3d(demon.getX(), demon.getEyeY()-0.10, demon.getZ());
        Vec3d delta = to.subtract(from);
        for (int i=0;i<n;i++) {
            double t=(i+1.0)/(n+1.0); Vec3d q=from.add(delta.multiply(t));
            c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,q.x+(c.world.random.nextDouble()-.5)*.035,q.y+(c.world.random.nextDouble()-.5)*.035,q.z+(c.world.random.nextDouble()-.5)*.035,delta.x*.04,delta.y*.04,delta.z*.04);
        }
    }

    private static void spawnAbdomenDrip(MinecraftClient c, PlayerEntity t, int n){
        double y=t.getY()+t.getHeight()*0.46;
        for(int i=0;i<n;i++) c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,t.getX()+(c.world.random.nextDouble()-.5)*.18,y,t.getZ()+(c.world.random.nextDouble()-.5)*.18,(c.world.random.nextDouble()-.5)*.012,-.06-c.world.random.nextDouble()*.035,(c.world.random.nextDouble()-.5)*.012);
    }

    private static void clearStabVisual(){if(stabbedWeaponEntity!=null)stabbedWeaponEntity.discard();stabbedWeaponEntity=null;stabbedTargetUuid=null;stabbedWeaponTicks=0;stabbedBloodCooldown=0;}
    private static void reset(){holdTicks=animationTicks=visualArrowCount=bleedTicks=dripCooldown=groundBloodTicks=throwAnimationTicks=0;suppressedArrowServerCount=-1;biteTicks=biteBloodCooldown=0;biteTargetUuid=null;bitePoseActive=false;if(bodyArrowEntity!=null)bodyArrowEntity.discard();bodyArrowEntity=null;if(groundArrowEntity!=null)groundArrowEntity.discard();groundArrowEntity=null;clearStabVisual();}

    private static void spawnTargetBlood(MinecraftClient c,PlayerEntity t,int n){double y=t.getY()+t.getHeight()*0.52;for(int i=0;i<n;i++)c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,t.getX()+(c.world.random.nextDouble()-.5)*.32,y+(c.world.random.nextDouble()-.5)*.20,t.getZ()+(c.world.random.nextDouble()-.5)*.32,(c.world.random.nextDouble()-.5)*.035,-.035-c.world.random.nextDouble()*.03,(c.world.random.nextDouble()-.5)*.035);}

    private static void ripArrow(MinecraftClient c,PlayerEntity p){
        p.swingHand(Hand.MAIN_HAND);
        p.playSound(SoundEvents.ENTITY_PLAYER_HURT,.55f,.72f);
        p.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,.38f,.58f);
        p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER,.32f,.82f);
        double y=p.getY()+p.getHeight()*.64;
        for(int i=0;i<34;i++)c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.42,y+(c.world.random.nextDouble()-.5)*.38,p.getZ()+(c.world.random.nextDouble()-.5)*.42,(c.world.random.nextDouble()-.5)*.09,-.025-c.world.random.nextDouble()*.05,(c.world.random.nextDouble()-.5)*.09);
        spawnBloodDrip(c,p,16);
    }

    private static void animateThrow(MinecraftClient c,PlayerEntity p,int left){
        if(left==8||left==4)p.swingHand(Hand.MAIN_HAND);
        if(left==8)p.playSound(SoundEvents.ENTITY_ARROW_SHOOT,.28f,.68f);
        if(left<=6&&left>0&&c.world.random.nextInt(3)==0) spawnBloodDrip(c,p,2);
    }

    private static void dropBloodyArrow(MinecraftClient c,PlayerEntity p,Vec3d look){
        Vec3d l=look.lengthSquared()<0.001?new Vec3d(0,0,1):look.normalize();
        double x=p.getX()+l.x*.72;
        double y=p.getY()+.11;
        double z=p.getZ()+l.z*.72;

        ItemStack bloodyArrow = new ItemStack(Items.TIPPED_ARROW);
        bloodyArrow.set(DataComponentTypes.POTION_CONTENTS,
                new PotionContentsComponent(Optional.empty(), Optional.of(0x7A0202), List.of(), Optional.empty()));

        if(groundArrowEntity!=null) groundArrowEntity.discard();
        groundArrowEntity=new ArrowEntity(c.world,x,y,z,bloodyArrow,null);
        groundArrowEntity.setNoGravity(true);
        groundArrowEntity.setVelocity(Vec3d.ZERO);
        groundArrowEntity.setYaw((float)Math.toDegrees(Math.atan2(-l.x,l.z)));
        groundArrowEntity.setPitch(55.0f);
        c.world.addEntity(groundArrowEntity);

        groundBloodX=x;groundBloodY=p.getY()+.018;groundBloodZ=z;groundBloodTicks=360;
        for(int i=0;i<22;i++)c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,x+(c.world.random.nextDouble()-.5)*.18,p.getY()+.03,z+(c.world.random.nextDouble()-.5)*.18,(c.world.random.nextDouble()-.5)*.012,-.012-c.world.random.nextDouble()*.018,(c.world.random.nextDouble()-.5)*.012);
        spawnGroundBlood(c);
    }

    private static void spawnGroundBlood(MinecraftClient c){int n=groundBloodTicks>250?6:3;for(int i=0;i<n;i++){double a=c.world.random.nextDouble()*Math.PI*2,r=c.world.random.nextDouble()*.42;c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,groundBloodX+Math.cos(a)*r,groundBloodY,groundBloodZ+Math.sin(a)*r,0,.001,0);}}
    private static void spawnBloodDrip(MinecraftClient c,PlayerEntity p,int n){double y=p.getY()+p.getHeight()*(.48+c.world.random.nextDouble()*.22);for(int i=0;i<n;i++)c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.34,y,p.getZ()+(c.world.random.nextDouble()-.5)*.34,(c.world.random.nextDouble()-.5)*.025,-.045-c.world.random.nextDouble()*.035,(c.world.random.nextDouble()-.5)*.025);}
    private static void animatePull(MinecraftClient c,PlayerEntity p,int left){if(left==14||left==9||left==4)p.swingHand(Hand.MAIN_HAND);if(left<=12&&left>=2&&c.world.random.nextBoolean())c.world.addParticleClient(BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.28,p.getY()+.15+c.world.random.nextDouble()*.35,p.getZ()+(c.world.random.nextDouble()-.5)*.28,0,-.035,0);}
}
