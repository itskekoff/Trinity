package me.f1nal.trinity.decompiler.output.number;

public class NumberDisplayTypeDecimal extends NumberDisplayType {
    @Override
    public String getTextImpl(Number number) {
        if (number instanceof Double) {
            return String.valueOf(number.doubleValue());
        } else if (number instanceof Float) {
            return String.valueOf(number.floatValue());
        } else {
            return Long.toString(number.longValue());
        }
    }

    @Override
    public String getLabel() {
        return "Decimal";
    }
}