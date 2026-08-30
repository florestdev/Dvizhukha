package ru.florestdev.dvizhukha;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import ru.florestdev.dvizhukha.entity.Khokhol;

import java.util.List;

public class Dvizhukha implements ModInitializer {
    public static final String MOD_ID = "dvizhukha";
    private final Random random = Random.create();

    public static final EntityType<Khokhol> KHOKHOL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "khokhol"),
            EntityType.Builder.<Khokhol>create(Khokhol::new, SpawnGroup.CREATURE)
                    .dimensions(1.5f, 1.5f)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "khokhol")))
    );

    private int tickCounter = 0;
    private static final int SPAWN_INTERVAL = 6000; // 10 секунд для теста

    @Override
    public void onInitialize() {
        System.out.println("Dvizhukha mod initialized!");

        FabricDefaultAttributeRegistry.register(KHOKHOL, Khokhol.createKhokholAttributes());

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("khokhol")
                            .executes(context -> {

                                ServerCommandSource source = context.getSource();

                                // Консоль
                                if (source.getEntity() == null) {
                                    pigCycle(source.getServer());

                                    source.sendMessage(
                                            Text.literal("§aЦикл появления хохлов успешно запущен.")
                                    );

                                    return 1;
                                }

                                if (source.getEntity() instanceof ServerPlayerEntity player) {

                                    MinecraftServer server = source.getServer();

                                    // Singleplayer — разрешаем всем
                                    if (!server.isDedicated()) {
                                        pigCycle(server);

                                        source.sendMessage(
                                                Text.literal("§aЦикл появления хохлов успешно запущен.")
                                        );

                                        return 1;
                                    }

                                    // Dedicated — проверяем OP
                                    if (!server.getPlayerManager()
                                            .isOperator(player.getPlayerConfigEntry())) {

                                        source.sendError(
                                                Text.literal("§cУ тебя нет прав на эту команду.")
                                        );

                                        return 0;
                                    }

                                    pigCycle(server);

                                    source.sendMessage(
                                            Text.literal("§aЦикл появления хохлов успешно запущен.")
                                    );

                                    return 1;
                                }

                                return 0;
                            })
            );
        });
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter >= SPAWN_INTERVAL) {
            tickCounter = 0;
            pigCycle(server);
        }
    }

    private void pigCycle(MinecraftServer server) {
        System.out.println("§e[PigCycle] Так, спавним хохла...");

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) return;

        ServerPlayerEntity player = players.get(random.nextInt(players.size()));
        if (player == null || !player.isAlive()) return;

        World world = player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();

        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.setPosition(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5);
        world.spawnEntity(lightning);

        player.sendMessage(Text.literal("§c⚡ Молния ударила рядом с вами!"), false);

        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            server.execute(() -> {
                BlockPos spawnPos = playerPos.add(
                        random.nextInt(5) - 2,
                        0,
                        random.nextInt(5) - 2
                );

                if (!world.getBlockState(spawnPos).isAir()) {
                    spawnPos = playerPos.up(2);
                }

                Khokhol khokhol = new Khokhol(KHOKHOL, world);
                khokhol.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                khokhol.setScale(1.5f);
                khokhol.setPersistent();
                khokhol.setTarget(player);

                world.spawnEntity(khokhol);

                player.sendMessage(Text.literal("§cИз молнии появился ВСУшник-хохол! ЗАМОЧИ ЕГО"), false);
                System.out.println("§a[PigCycle] Хохол заспавнен!");
            });
        }).start();
    }
}