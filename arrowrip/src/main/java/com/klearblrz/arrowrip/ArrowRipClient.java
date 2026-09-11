package com.klearblrz.arrowrip;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class ArrowRipClient implements ClientModInitializer {
    private static KeyBinding pullKey, stabKey;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("arrowrip", "main"));
    private static final DustParticleEffect BLOOD = new DustParticleEffect(0x7A0202, 1.15f);
    private static final DustParticleEffect DARK_BLOOD = new DustParticleEffect(0x380000, 1.35f);
    private static int holdTicks, animationTicks, visualArrowCount, bleedTicks, dripCooldown;
    private static int groundBloodTicks;
    private static double groundBloodX, groundBloodY, groundBloodZ;
    private static UUID stabbedTargetUuid;
    private static ItemEntity stabbedWeaponEntity;
    private static int stabbedWeaponTicks, stabbedBloodCooldown;

    @Override public void onInitializeClient() {
        pullKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arrowrip.pull", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
        stabKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.arrowrip.stab", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(ArrowRipClient::tick);
    }

    private static Vec3d pos(Entity e) { return new Vec3d(e.getX(), e.getY(), e.getZ()); }

    private static void tick(MinecraftClient client) {
        PlayerEntity p = client.player;
        if (p == null || client.world == null) { reset(); return; }

        int tracked = p.getStuckArrowCount();
        if (tracked > 0) { visualArrowCount = Math.max(visualArrowCount, tracked); p.setStuckArrowCount(0); }
        if (animationTicks > 0) { animationTicks--; animatePull(client,p,animationTicks); }
        if (bleedTicks > 0) bleedTicks--;
        if (dripCooldown > 0) dripCooldown--;
        if ((visualArrowCount > 0 || bleedTicks > 0) && dripCooldown <= 0) {
            spawnBloodDrip(client,p,visualArrowCount > 0 ? 3 : 2);
            dripCooldown = visualArrowCount > 0 ? 4 + client.world.random.nextInt(5) : 7 + client.world.random.nextInt(7);
        }
        if (groundBloodTicks > 0) { groundBloodTicks--; if (groundBloodTicks % 3 == 0) spawnGroundBlood(client); }
        tickStabbedWeapon(client);

        while (stabKey.wasPressed()) tryStab(client,p);

        if (pullKey.isPressed() && visualArrowCount > 0) {
            holdTicks++;
            if (holdTicks == 1 || holdTicks == 6 || holdTicks == 12) p.swingHand(Hand.MAIN_HAND);
            if (holdTicks == 12) p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER,0.16f,1.25f);
            if (holdTicks >= 20) {
                visualArrowCount--; animationTicks=16; bleedTicks=Math.max(bleedTicks,90); holdTicks=0; ripArrow(client,p);
            }
        } else holdTicks=0;
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
        stabbedTargetUuid=target.getUuid(); stabbedWeaponTicks=spear?90:65; stabbedBloodCooldown=0;
        Vec3d dir=pos(attacker).subtract(pos(target)); if(dir.lengthSquared()<0.0001) dir=new Vec3d(0,0,1); dir=dir.normalize();
        double x=target.getX()+dir.x*0.20, y=target.getY()+target.getHeight()*0.52, z=target.getZ()+dir.z*0.20;
        stabbedWeaponEntity=new ItemEntity(client.world,x,y,z,stack.copyWithCount(1));
        stabbedWeaponEntity.setPickupDelayInfinite(); stabbedWeaponEntity.setNoGravity(true); stabbedWeaponEntity.setVelocity(Vec3d.ZERO);
        stabbedWeaponEntity.setYaw(target.getYaw()+90f); client.world.addEntity(stabbedWeaponEntity);
        target.playSound(SoundEvents.ENTITY_PLAYER_HURT,0.55f,0.76f); target.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,0.30f,0.62f);
        spawnTargetBlood(client,target,spear?34:26);
    }

    private static void tickStabbedWeapon(MinecraftClient client) {
        if (stabbedWeaponTicks<=0 || stabbedTargetUuid==null || stabbedWeaponEntity==null || client.world==null) { if(stabbedWeaponTicks<=0) clearStabVisual(); return; }
        Entity e=client.world.getPlayerByUuid(stabbedTargetUuid); if(!(e instanceof PlayerEntity target)||!target.isAlive()){clearStabVisual();return;}
        stabbedWeaponTicks--; if(stabbedBloodCooldown>0) stabbedBloodCooldown--;
        Vec3d d=client.player!=null?pos(client.player).subtract(pos(target)):new Vec3d(0,0,1); if(d.lengthSquared()<0.0001)d=new Vec3d(0,0,1); d=d.normalize();
        stabbedWeaponEntity.setPosition(target.getX()+d.x*0.20,target.getY()+target.getHeight()*0.52,target.getZ()+d.z*0.20);
        stabbedWeaponEntity.setVelocity(Vec3d.ZERO); stabbedWeaponEntity.setYaw(target.getYaw()+90f);
        if(stabbedBloodCooldown<=0){spawnTargetBlood(client,target,3);stabbedBloodCooldown=5+client.world.random.nextInt(5);}
    }

    private static void clearStabVisual(){if(stabbedWeaponEntity!=null)stabbedWeaponEntity.discard();stabbedWeaponEntity=null;stabbedTargetUuid=null;stabbedWeaponTicks=0;stabbedBloodCooldown=0;}
    private static void reset(){holdTicks=animationTicks=visualArrowCount=bleedTicks=dripCooldown=groundBloodTicks=0;clearStabVisual();}

    private static void spawnTargetBlood(MinecraftClient c,PlayerEntity t,int n){
        double y=t.getY()+t.getHeight()*0.52; for(int i=0;i<n;i++) c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,t.getX()+(c.world.random.nextDouble()-.5)*.32,y+(c.world.random.nextDouble()-.5)*.20,t.getZ()+(c.world.random.nextDouble()-.5)*.32,(c.world.random.nextDouble()-.5)*.035,-.035-c.world.random.nextDouble()*.03,(c.world.random.nextDouble()-.5)*.035);
    }

    private static void ripArrow(MinecraftClient c,PlayerEntity p){
        p.swingHand(Hand.MAIN_HAND); p.playSound(SoundEvents.ENTITY_PLAYER_HURT,.55f,.72f); p.playSound(SoundEvents.ENTITY_SLIME_SQUISH_SMALL,.38f,.58f); p.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER,.32f,.82f);
        double y=p.getY()+p.getHeight()*.64; for(int i=0;i<28;i++) c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.42,y+(c.world.random.nextDouble()-.5)*.38,p.getZ()+(c.world.random.nextDouble()-.5)*.42,(c.world.random.nextDouble()-.5)*.09,-.025-c.world.random.nextDouble()*.05,(c.world.random.nextDouble()-.5)*.09);
        spawnBloodDrip(c,p,14); dropBloodyArrow(c,p);
    }

    private static void dropBloodyArrow(MinecraftClient c,PlayerEntity p){
        Vec3d l=p.getRotationVec(1f);
        double x=p.getX()+l.x*.42;
        double y=p.getY()+.055;
        double z=p.getZ()+l.z*.42;
        ItemEntity a=new ItemEntity(c.world,x,y,z,new ItemStack(Items.ARROW));
        a.setPickupDelayInfinite();
        a.setNoGravity(true);
        a.setVelocity(Vec3d.ZERO);
        a.setYaw((float)Math.toDegrees(Math.atan2(-l.x,l.z)));
        a.setPitch(90.0f);
        c.world.addEntity(a);
        groundBloodX=x;groundBloodY=p.getY()+.025;groundBloodZ=z;groundBloodTicks=240;
        for(int i=0;i<14;i++) c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,x+(c.world.random.nextDouble()-.5)*.16,y+.02,z+(c.world.random.nextDouble()-.5)*.16,0,-.01,0);
        spawnGroundBlood(c);
    }

    private static void spawnGroundBlood(MinecraftClient c){int n=groundBloodTicks>180?5:2;for(int i=0;i<n;i++){double a=c.world.random.nextDouble()*Math.PI*2,r=c.world.random.nextDouble()*.38;c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,groundBloodX+Math.cos(a)*r,groundBloodY,groundBloodZ+Math.sin(a)*r,0,.001,0);}}
    private static void spawnBloodDrip(MinecraftClient c,PlayerEntity p,int n){double y=p.getY()+p.getHeight()*(.48+c.world.random.nextDouble()*.22);for(int i=0;i<n;i++)c.world.addParticleClient(i%3==0?DARK_BLOOD:BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.34,y,p.getZ()+(c.world.random.nextDouble()-.5)*.34,(c.world.random.nextDouble()-.5)*.025,-.045-c.world.random.nextDouble()*.035,(c.world.random.nextDouble()-.5)*.025);}
    private static void animatePull(MinecraftClient c,PlayerEntity p,int left){if(left==14||left==9||left==4)p.swingHand(Hand.MAIN_HAND);if(left<=12&&left>=2&&c.world.random.nextBoolean())c.world.addParticleClient(BLOOD,p.getX()+(c.world.random.nextDouble()-.5)*.28,p.getY()+.15+c.world.random.nextDouble()*.35,p.getZ()+(c.world.random.nextDouble()-.5)*.28,0,-.035,0);}
}
