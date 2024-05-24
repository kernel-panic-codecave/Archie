package com.withertech.archie.mixin.neoforge;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(StructureTemplateManager.class)
public abstract class StructureTemplateManagerMixin {
    @Unique
    private static final String GAMETEST_STRUCTURE_PATH = "gametest/structures";
    @Unique
    private static final FileToIdConverter GAMETEST_STRUCTURE_FINDER = new FileToIdConverter(GAMETEST_STRUCTURE_PATH, ".snbt");

    @Shadow
    private ResourceManager resourceManager;

    @Shadow public abstract StructureTemplate readStructure(CompoundTag nbt);


    @Shadow public List<StructureTemplateManager.Source> sources;

    @Unique
    private Optional<StructureTemplate> archie_loadSnbtFromResource(ResourceLocation id) {
        ResourceLocation path = GAMETEST_STRUCTURE_FINDER.idToFile(id);
        Optional<Resource> resource = this.resourceManager.getResource(path);

        if (resource.isPresent()) {
            try {
                String snbt = IOUtils.toString(resource.get().openAsReader());
                CompoundTag nbt = NbtUtils.snbtToStructure(snbt);
                return Optional.of(this.readStructure(nbt));
            } catch (IOException | CommandSyntaxException e) {
                throw new RuntimeException("Failed to load GameTest structure " + id, e);
            }
        }

        return Optional.empty();
    }

    @Unique
    private Stream<ResourceLocation> archie_streamTemplatesFromResource() {
        FileToIdConverter finder = GAMETEST_STRUCTURE_FINDER;
        return finder.listMatchingResources(this.resourceManager).keySet().stream().map(finder::fileToId);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void addFabricTemplateProvider(ResourceManager resourceManager, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer fixerUpper, HolderGetter<Block> blockLookup, CallbackInfo ci, @Local ImmutableList.Builder<StructureTemplateManager.Source> builder) {
        builder.add(new StructureTemplateManager.Source(this::archie_loadSnbtFromResource, this::archie_streamTemplatesFromResource));
        this.sources = builder.build();
    }
}