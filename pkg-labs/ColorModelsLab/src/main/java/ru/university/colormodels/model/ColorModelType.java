package ru.university.colormodels.model;

public enum ColorModelType {
    RGB("RGB", new ChannelSpec[] {
            channel("R", 0, 255, 1, 0, ""),
            channel("G", 0, 255, 1, 0, ""),
            channel("B", 0, 255, 1, 0, "")
    }),
    CMYK("CMYK", new ChannelSpec[] {
            channel("C", 0, 100, 0.1, 1, "%"),
            channel("M", 0, 100, 0.1, 1, "%"),
            channel("Y", 0, 100, 0.1, 1, "%"),
            channel("K", 0, 100, 0.1, 1, "%")
    }),
    HSV("HSV", new ChannelSpec[] {
            channel("H", 0, 360, 1, 0, "°"),
            channel("S", 0, 100, 0.1, 1, "%"),
            channel("V", 0, 100, 0.1, 1, "%")
    }),
    HLS("HLS", new ChannelSpec[] {
            channel("H", 0, 360, 1, 0, "°"),
            channel("L", 0, 100, 0.1, 1, "%"),
            channel("S", 0, 100, 0.1, 1, "%")
    }),
    XYZ("XYZ", new ChannelSpec[] {
            channel("X", 0, 120, 0.1, 1, ""),
            channel("Y", 0, 120, 0.1, 1, ""),
            channel("Z", 0, 120, 0.1, 1, "")
    }),
    LAB("LAB", new ChannelSpec[] {
            channel("L", 0, 100, 0.1, 1, ""),
            channel("a", -128, 127, 0.1, 1, ""),
            channel("b", -128, 127, 0.1, 1, "")
    });

    private final String displayName;
    private final ChannelSpec[] channels;

    ColorModelType(String displayName, ChannelSpec[] channels) {
        this.displayName = displayName;
        this.channels = channels;
    }

    public ChannelSpec[] channels() {
        return channels.clone();
    }

    public int channelCount() {
        return channels.length;
    }

    private static ChannelSpec channel(
            String name, double min, double max, double step, int decimals, String unit) {
        return new ChannelSpec(name, min, max, step, decimals, unit);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
