// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.impl;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaProvider;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;

public final class MediaProviders {
    private static final Map<Item, MediaProvider> itemProviders = new HashMap<>();
    private static final Set<MediaProvider> providers = new LinkedHashSet<>();

    private MediaProviders() {
    }

    public static synchronized void register(MediaProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        providers.add(provider);
    }

    public static @Nullable IMedia get(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // Try the per-item provider first.
        var itemProvider = itemProviders.get(stack.getItem());
        if (itemProvider != null) {
            var media = itemProvider.getMedia(stack);
            if (media != null) return media;
        }

        // Try the handlers in order:
        for (var mediaProvider : providers) {
            var media = mediaProvider.getMedia(stack);
            if (media != null) return media;
        }
        return null;
    }

    public static synchronized void registerDefault() {
        ModRegistry.registerMedia(new ModRegistry.ItemComponent<>() {
            @Override
            public void registerForItems(BiFunction<ItemStack, @Nullable Void, @Nullable IMedia> provider, ItemLike... items) {
                MediaProvider wrappedProvider = s -> provider.apply(s, null);
                for (var item : items) itemProviders.put(item.asItem(), wrappedProvider);
            }

            @Override
            public void registerFallback(BiFunction<ItemStack, @Nullable Void, @Nullable IMedia> provider) {
                register(s -> provider.apply(s, null));
            }
        });
    }
}
