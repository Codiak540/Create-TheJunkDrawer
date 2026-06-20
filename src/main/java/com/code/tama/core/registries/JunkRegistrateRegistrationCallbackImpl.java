package com.code.tama.core.registries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Either;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class JunkRegistrateRegistrationCallbackImpl {
	private static final Map<String, Either<List<CallbackImpl<?, ?>>, JunkRegistrate>> CALLBACKS = new HashMap<>();

	public static void provideRegistrate(JunkRegistrate registrate) {
		synchronized (CALLBACKS) {
			String modid = registrate.getModid();

			var either = CALLBACKS.remove(modid);
			if (either != null) {
				var optionalCallbacks = either.left();
				if (optionalCallbacks.isEmpty()) {
					throw new IllegalArgumentException("Tried to register a duplicate JunkRegistrate instance for mod ID: " + modid);
				}

				for (CallbackImpl<?, ?> callback : optionalCallbacks.get()) {
					callback.addToRegistrate(registrate);
				}
			}

			CALLBACKS.put(modid, Either.right(registrate));
		}
	}

	public static <R, T extends R> void register(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
		CallbackImpl<R, T> callbackImpl = new CallbackImpl<>(registry, id, callback);

		Either<List<CallbackImpl<?, ?>>, JunkRegistrate> either;
		synchronized (CALLBACKS) {
			either = CALLBACKS.computeIfAbsent(id.getNamespace(),
				k -> Either.left(new ArrayList<>()));
			either.ifLeft(callbacks -> callbacks.add(callbackImpl));
		}

		either.ifRight(callbackImpl::addToRegistrate);
	}

	private record CallbackImpl<R, T extends R>(ResourceKey<? extends Registry<R>> registry, ResourceLocation id, NonNullConsumer<? super T> callback) {
		public void addToRegistrate(JunkRegistrate registrate) {
			registrate.<R, T>addRegisterCallback(id.getPath(), registry, callback);
		}
	}
}
