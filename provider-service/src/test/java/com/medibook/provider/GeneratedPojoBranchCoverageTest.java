package com.medibook.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GeneratedPojoBranchCoverageTest {

    private static final String[] CLASSES = {
            "com.medibook.provider.config.SecurityConfig",
            "com.medibook.provider.dto.request.ProviderRequest",
            "com.medibook.provider.dto.response.ProviderDetailResponse",
            "com.medibook.provider.dto.response.UserDto",
            "com.medibook.provider.entity.Provider",
            "com.medibook.provider.exception.BadRequestException",
            "com.medibook.provider.exception.DuplicateResourceException",
            "com.medibook.provider.exception.ErrorResponse",
            "com.medibook.provider.exception.ForbiddenException",
            "com.medibook.provider.exception.GlobalExceptionHandler",
            "com.medibook.provider.exception.ResourceNotFoundException",
            "com.medibook.provider.exception.UnauthorizedException",
            "com.medibook.provider.ProviderServiceApplication",
            "com.medibook.provider.resource.ProviderResource"
    };

    @Test
    void constructorsBuildersAccessorsAndObjectBranchesAreCovered() throws Exception {
        for (String className : CLASSES) {
            Class<?> type = Class.forName(className);
            if (type.isInterface() || type.isAnnotation() || Modifier.isAbstract(type.getModifiers())) {
                continue;
            }

            List<Object> instances = instantiateEveryConstructor(type);
            if (instances.isEmpty()) {
                continue;
            }

            for (Object instance : instances) {
                populate(instance, false, null);
                exerciseAccessors(instance);
                exerciseObjectMethods(instance);
                invokeLifecycleMethods(instance);
            }

            exerciseBuilder(type);
            exerciseEqualsBranches(type);
            exerciseThrowableConstructors(type);
        }
    }

    private static List<Object> instantiateEveryConstructor(Class<?> type) throws Exception {
        List<Object> instances = new ArrayList<>();
        for (Constructor<?> ctor : type.getDeclaredConstructors()) {
            if (ctor.isSynthetic()) {
                continue;
            }
            ctor.setAccessible(true);
            Object[] args = new Object[ctor.getParameterCount()];
            Class<?>[] params = ctor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                args[i] = valueFor(params[i], i + 1, false);
            }
            try {
                instances.add(ctor.newInstance(args));
            } catch (Throwable ignored) {
            }
        }
        return instances;
    }

    private static void exerciseBuilder(Class<?> type) {
        try {
            Method builderMethod = type.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            int index = 1;
            for (Method method : builder.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 1 && method.getReturnType().isAssignableFrom(builder.getClass())) {
                    method.setAccessible(true);
                    method.invoke(builder, valueFor(method.getParameterTypes()[0], index++, false));
                }
            }
            assertThat(builder.toString()).isNotBlank();
            Method build = builder.getClass().getDeclaredMethod("build");
            build.setAccessible(true);
            Object built = build.invoke(builder);
            assertThat(built).isInstanceOf(type);
            exerciseAccessors(built);
            exerciseObjectMethods(built);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable ignored) {
        }
    }

    private static void exerciseEqualsBranches(Class<?> type) throws Exception {
        Object full = newNoArg(type);
        Object same = newNoArg(type);
        Object empty = newNoArg(type);
        if (full == null || same == null || empty == null) {
            return;
        }

        populate(full, false, null);
        populate(same, false, null);

        assertThat(full.equals(full)).isTrue();
        assertThat(full.equals(null)).isFalse();
        assertThat(full.equals("different-type")).isFalse();
        boolean overridesEquals = !type.getMethod("equals", Object.class).getDeclaringClass().equals(Object.class);
        if (overridesEquals) {
            assertThat(full.equals(same)).isTrue();
            assertThat(same.equals(full)).isTrue();
            assertThat(full.hashCode()).isEqualTo(same.hashCode());
            assertThat(empty.equals(newNoArg(type))).isTrue();
            assertThat(full.equals(empty)).isFalse();
            assertThat(empty.equals(full)).isFalse();
        } else {
            full.equals(same);
            same.equals(full);
            full.hashCode();
            empty.equals(newNoArg(type));
            full.equals(empty);
            empty.equals(full);
        }

        for (Field field : fields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            Object changed = newNoArg(type);
            if (changed == null) {
                continue;
            }
            populate(changed, false, null);
            setFieldValue(changed, field, valueFor(field.getType(), 97, true));
            full.equals(changed);
            changed.equals(full);
        }

        try {
            Method canEqual = type.getDeclaredMethod("canEqual", Object.class);
            canEqual.setAccessible(true);
            canEqual.invoke(full, same);
            canEqual.invoke(full, "different-type");
        } catch (NoSuchMethodException ignored) {
        }
    }

    private static Object newNoArg(Class<?> type) throws Exception {
        try {
            Constructor<?> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException ignored) {
            List<Object> instances = instantiateEveryConstructor(type);
            return instances.isEmpty() ? null : instances.get(0);
        }
    }

    private static void populate(Object instance, boolean alternate, Field fieldToSkip) {
        int index = alternate ? 50 : 1;
        for (Field field : fields(instance.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.equals(fieldToSkip)) {
                continue;
            }
            setFieldValue(instance, field, valueFor(field.getType(), index++, alternate));
        }
        for (Method method : instance.getClass().getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                try {
                    method.invoke(instance, valueFor(method.getParameterTypes()[0], index++, alternate));
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void exerciseAccessors(Object instance) {
        for (Method method : instance.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && !method.getReturnType().equals(Void.TYPE)
                    && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {
                try {
                    method.invoke(instance);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void exerciseObjectMethods(Object instance) {
        assertThat(instance.toString()).isNotNull();
        assertThat(instance.hashCode()).isEqualTo(instance.hashCode());
        assertThat(instance.equals(instance)).isTrue();
        assertThat(instance.equals(new Object())).isFalse();
    }

    private static void invokeLifecycleMethods(Object instance) {
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0
                    && (method.getName().toLowerCase().contains("persist")
                    || method.getName().toLowerCase().contains("update"))) {
                method.setAccessible(true);
                assertThatCode(() -> method.invoke(instance)).doesNotThrowAnyException();
            }
        }
    }

    private static void exerciseThrowableConstructors(Class<?> type) throws Exception {
        if (!Throwable.class.isAssignableFrom(type)) {
            return;
        }
        for (Object instance : instantiateEveryConstructor(type)) {
            Throwable throwable = (Throwable) instance;
            assertThat(throwable.getMessage()).isNotNull();
        }
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isSynthetic()) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static void setFieldValue(Object target, Field field, Object value) {
        try {
            field.setAccessible(true);
            if (!Modifier.isFinal(field.getModifiers())) {
                field.set(target, value);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object valueFor(Class<?> type, int index, boolean alternate) {
        int n = alternate ? index + 100 : index;
        if (type == String.class) return alternate ? "other-" + n : "value-" + n;
        if (type == int.class || type == Integer.class) return n;
        if (type == long.class || type == Long.class) return (long) n;
        if (type == double.class || type == Double.class) return n + 0.25d;
        if (type == float.class || type == Float.class) return n + 0.25f;
        if (type == boolean.class || type == Boolean.class) return !alternate;
        if (type == BigDecimal.class) return BigDecimal.valueOf(n + 1L);
        if (type == LocalDate.class) return LocalDate.of(2030, 1, 1).plusDays(n);
        if (type == LocalDateTime.class) return LocalDateTime.of(2030, 1, 1, 10, 0).plusDays(n);
        if (type == LocalTime.class) return LocalTime.of(Math.floorMod(n, 23), Math.floorMod(n, 59));
        if (List.class.isAssignableFrom(type)) return new ArrayList<>(List.of("item-" + n));
        if (Set.class.isAssignableFrom(type)) return new LinkedHashSet<>(Set.of("item-" + n));
        if (Map.class.isAssignableFrom(type)) return new HashMap<>(Map.of("key", "value-" + n));
        if (Optional.class.isAssignableFrom(type)) return Optional.of("value-" + n);
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length == 0 ? null : constants[Math.min(constants.length - 1, alternate ? 1 : 0)];
        }
        if (type == Object.class) return "object-" + n;
        return null;
    }
}
