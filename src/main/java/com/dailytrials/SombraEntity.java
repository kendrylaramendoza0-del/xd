package com.dailytrials;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/** Nuevo mob: la Sombra. Zombi rápido, inmune al fuego, que ciega al golpear. */
public class SombraEntity extends ZombieEntity {

    /** true = hoy es "día de Sombras" y pueden aparecer solas. Lo controla DailyTrials. */
    public static boolean activas = false;

    public SombraEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }

    public static boolean puedeAparecer(EntityType<SombraEntity> type, ServerWorldAccess world,
                                        SpawnReason reason, BlockPos pos, Random random) {
        return activas && HostileEntity.canSpawnInDark(type, world, reason, pos, random);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0);
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60));
        }
        return hit;
    }
}
