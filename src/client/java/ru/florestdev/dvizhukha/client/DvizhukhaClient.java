package ru.florestdev.dvizhukha.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.PigEntityRenderer;
import ru.florestdev.dvizhukha.Dvizhukha;

public class DvizhukhaClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("Dvizhukha Client initialized!");

        EntityRendererRegistry.register(
                Dvizhukha.KHOKHOL,
                PigEntityRenderer::new
        );
    }
}