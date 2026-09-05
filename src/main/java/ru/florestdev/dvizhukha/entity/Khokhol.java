package ru.florestdev.dvizhukha.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import ru.florestdev.dvizhukha.KhokholKnight;

public class Khokhol extends PigEntity {
    private float scale = 1.5f;
    private int attackCooldown = 0;

    public Khokhol(EntityType<? extends PigEntity> entityType, World world) {
        super(entityType, world);

        this.setCustomName(Text.literal("§c§lХохол"));
        this.setCustomNameVisible(true);
        this.setPersistent();
    }

    @Override
    protected void initGoals() {
        super.initGoals();

        this.targetSelector.add(0, new ActiveTargetGoal<>(this, PlayerEntity.class, true, true));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PathAwareEntity.class, true, true));

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.add(2, new WanderAroundGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) attackCooldown--;

        if (!this.getEntityWorld().isClient()) {
            if (this.getTarget() == null || !this.getTarget().isAlive()) {
                findNewTarget();
            }

            // Автоматическая атака
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double distance = this.squaredDistanceTo(target);
                if (distance < 4.0 && attackCooldown == 0) {
                    performAttack(target);
                }
            }
        }
    }

    private void findNewTarget() {
        PlayerEntity nearestPlayer = this.getEntityWorld().getClosestPlayer(this, 20.0);
        if (nearestPlayer != null) {
            this.setTarget(nearestPlayer);
        }
    }

    private void performAttack(LivingEntity target) {
        if (attackCooldown > 0) return;

        attackCooldown = 10;

        // Наносим урон
        float damage = 6.0f;
        var damageAttr = this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damage = (float)damageAttr.getValue();
        }

        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            target.damage(
                    serverWorld,
                    this.getDamageSources().mobAttack(this),
                    damage
            );
        }

        // Отбрасываем врага
        target.takeKnockback(0.5, target.getX() - this.getX(), target.getZ() - this.getZ());

        // Звук атаки
        this.playSound(net.minecraft.sound.SoundEvents.ENTITY_PIG_AMBIENT, 1.0F, 1.0F);
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (damageSource.getAttacker() instanceof ServerPlayerEntity player) {

            var maxHealth = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            var attackDamage = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);

            // +3 сердца = +6 HP
            if (maxHealth != null) {
                maxHealth.addTemporaryModifier(
                        new EntityAttributeModifier(
                                Identifier.of("dvizhukha", "khokhol_health"),
                                6.0,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )
                );

                // Восстанавливаем здоровье с учётом новых 3 сердец
                player.setHealth(player.getMaxHealth());
            }

            // +2 урона
            if (attackDamage != null) {
                attackDamage.addTemporaryModifier(
                        new EntityAttributeModifier(
                                Identifier.of("dvizhukha", "khokhol_damage"),
                                2.0,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )
                );
            }

            // Сообщение всем игрокам
            if (player.getEntityWorld() instanceof ServerWorld world) {
                MinecraftServer server = world.getServer();

                server.getPlayerManager().broadcast(
                        Text.literal(
                                "§c§lХОХОЛ ПОВЕРЖЕН! §f"
                                        + player.getName().getString()
                                        + " §7получил §c+3 сердца §7и §4+2 урона§7!"
                        ),
                        false
                );
                player.sendMessage(Text.literal("§cДержи свой клинок на 15 ударов, но с мощностью в -9 сердец за один удар!"));
                KhokholKnight.giveToPlayerStatic(player, player.getName().getString());
            }
        }

        super.onDeath(damageSource);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.getAttacker() instanceof LivingEntity attacker) {
            this.setTarget(attacker);
        }

        return super.damage(world, source, amount);
    }

    @Override
    public float getScaleFactor() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(5.0f, scale));
        this.calculateDimensions();
    }

    public static DefaultAttributeContainer.Builder createKhokholAttributes() {
        return PigEntity.createPigAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.ATTACK_SPEED, 1.2)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0);
    }
}