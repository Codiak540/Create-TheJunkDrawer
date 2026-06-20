package com.code.tama.core.registries;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.impl.registrate.CreateRegistrateRegistrationCallbackImpl;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class JunkRegistrateRegistrationCallback {
	public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
		JunkRegistrateRegistrationCallback.<R, T>register(registry, id, callback);
	}

	public static void provideRegistrate(JunkRegistrate registrate) {
		JunkRegistrateRegistrationCallbackImpl.provideRegistrate(registrate);
	}

	private JunkRegistrateRegistrationCallback() {
		throw new AssertionError("This class should not be instantiated");
	}
}
