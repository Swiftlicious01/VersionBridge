package com.swiftlicious.versionbridge.nms.shoulder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the server's private shoulder restoration routine across mapping
 * names.
 */
public final class ShoulderPetRelease {

	private static final ConcurrentHashMap<Class<?>, Method> METHODS = new ConcurrentHashMap<>();

	private ShoulderPetRelease() {
	}

	public static boolean release(Object handle, Object tag, Runnable clearAfterSuccess) {
		Method method = METHODS.computeIfAbsent(handle.getClass(), type -> resolve(type, tag.getClass()));
		try {
			Object restored = method.invoke(handle, tag);
			boolean success = method.getReturnType() == boolean.class ? Boolean.TRUE.equals(restored)
					: restored != null;
			if (success)
				clearAfterSuccess.run();
			return success;
		} catch (ReflectiveOperationException failure) {
			throw new IllegalStateException("Could not release shoulder pet; original shoulder data retained", failure);
		}
	}

	private static Method resolve(Class<?> type, Class<?> tag) {
		for (Class<?> parent = type; parent != null; parent = parent.getSuperclass()) {
			Method result = null;
			for (Method method : parent.getDeclaredMethods()) {
				// Spigot returns boolean; Paper returns the restored native entity.
				boolean returnsEntity = method.getReturnType().getName().startsWith("net.minecraft.")
						&& !method.getReturnType().isPrimitive();
				if (!method.isSynthetic() && Modifier.isPrivate(method.getModifiers())
						&& !Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1
						&& method.getParameterTypes()[0] == tag
						&& (method.getReturnType() == boolean.class || returnsEntity)) {
					if (result != null)
						throw new IllegalStateException("Ambiguous shoulder restoration routine; no pet data changed");
					result = method;
				}
			}
			if (result != null) {
				result.setAccessible(true);
				return result;
			}
		}
		throw new IllegalStateException("No safe shoulder restoration routine; no pet data changed");
	}
}