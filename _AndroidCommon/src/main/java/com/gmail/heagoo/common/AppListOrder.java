package com.gmail.heagoo.common;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Stable preference values and one ordering policy for every app list. */
public enum AppListOrder {
    BY_NAME("ByAlpha"),
    BY_INSTALL_TIME("ByInstallTime");

    private final String preferenceValue;

    AppListOrder(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public static AppListOrder fromIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : BY_NAME;
    }

    /** Reads canonical values and migrates labels saved by older versions. */
    public static AppListOrder fromPreference(String stored, String[] localizedLabels) {
        if (stored == null) return BY_NAME;
        String value = stored.trim();
        for (AppListOrder order : values()) {
            if (order.preferenceValue.equals(value)) return order;
        }
        if ("By App Name".equalsIgnoreCase(value)) return BY_NAME;
        if ("By Install Time".equalsIgnoreCase(value)
                || "By Installation Time".equalsIgnoreCase(value)) {
            return BY_INSTALL_TIME;
        }
        if (localizedLabels != null) {
            AppListOrder[] orders = values();
            for (int i = 0; i < orders.length && i < localizedLabels.length; i++) {
                String label = localizedLabels[i];
                if (label != null && value.equalsIgnoreCase(label.trim())) return orders[i];
            }
        }
        return BY_NAME;
    }

    /**
     * New installations first for the install-time option. Names use natural
     * locale order; package names and timestamps make ties deterministic.
     */
    public <T> Comparator<T> comparator(Function<T, String> name,
                                        ToLongFunction<T> installTime,
                                        Function<T, String> packageName) {
        NaturalOrderComparator names = new NaturalOrderComparator();
        return (a, b) -> {
            int result = this == BY_INSTALL_TIME
                    ? Long.compare(installTime.applyAsLong(b), installTime.applyAsLong(a)) : 0;
            if (result == 0) result = names.compare(name.apply(a), name.apply(b));
            if (result == 0) result = names.compare(packageName.apply(a), packageName.apply(b));
            if (result == 0) result = Long.compare(installTime.applyAsLong(a), installTime.applyAsLong(b));
            return result;
        };
    }
}
