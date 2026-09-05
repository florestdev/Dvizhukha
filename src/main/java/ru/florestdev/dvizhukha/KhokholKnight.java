package ru.florestdev.dvizhukha;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

import java.util.List;

public class KhokholKnight extends Item {

    public KhokholKnight(Item.Settings settings) {
        super(settings
                .fireproof()
                .rarity(Rarity.EPIC)
                // Делаем предмет мечом на базе незеритового материала
                .sword(
                        ToolMaterial.NETHERITE,
                        4.0F,
                        -2.4F
                )
                .maxDamage(15)
                .component(DataComponentTypes.ENCHANTABLE, null)
        );
    }

    @Override
    public Text getName(ItemStack stack) {
        String owner = getOwnerName(stack);
        return Text.literal("Нож " + owner)
                .formatted(Formatting.GOLD);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player) {
            // Дополнительные 5 урона
            if (!player.getEntityWorld().isClient()) {
                target.damage(
                        (ServerWorld) player.getEntityWorld(),
                        target.getDamageSources().playerAttack(player),
                        5.0F
                );
            }

            // Уменьшаем прочность на 1
            stack.damage(1, player);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    private String getOwnerName(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
            if (name != null) {
                return name.getString();
            }
        }
        return "Аноним";
    }

    public void setOwnerName(ItemStack stack, String name) {
        stack.set(
                DataComponentTypes.CUSTOM_NAME,
                Text.literal(name)
        );

        stack.set(
                DataComponentTypes.LORE,
                new LoreComponent(
                        List.of(
                                Text.literal("Принадлежит " + name)
                                        .formatted(Formatting.GRAY)
                        )
                )
        );
    }

    public ItemStack createWithOwner(String ownerName) {
        ItemStack stack = new ItemStack(this);
        setOwnerName(stack, ownerName);
        return stack;
    }

    public void giveToPlayer(ServerPlayerEntity player, String ownerName) {
        ItemStack stack = createWithOwner(ownerName);
        player.getInventory().offerOrDrop(stack);
    }

    // Статические методы для удобства использования
    public static KhokholKnight getInstance() {
        return Dvizhukha.getKhokholKnight();
    }

    public static void giveToPlayerStatic(ServerPlayerEntity player, String ownerName) {
        getInstance().giveToPlayer(player, ownerName);
    }

    public static ItemStack createStatic(String ownerName) {
        return getInstance().createWithOwner(ownerName);
    }

    // Дополнительные методы для улучшения функциональности

    /**
     * Проверяет, принадлежит ли нож указанному игроку
     */
    public boolean isOwnedBy(ItemStack stack, String playerName) {
        String owner = getOwnerName(stack);
        return owner.equals(playerName);
    }

    /**
     * Удаляет владельца с ножа (делает его "бесхозным")
     */
    public void removeOwner(ItemStack stack) {
        stack.remove(DataComponentTypes.CUSTOM_NAME);
        stack.remove(DataComponentTypes.LORE);
    }

    /**
     * Добавляет случайное зачарование при создании
     */
    public ItemStack createWithRandomEnchant(String ownerName) {
        ItemStack stack = createWithOwner(ownerName);
        // Здесь можно добавить случайные зачарования
        // Например: stack.addEnchantment(Enchantments.SHARPNESS, 3);
        return stack;
    }
}