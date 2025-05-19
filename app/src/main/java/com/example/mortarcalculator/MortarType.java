package com.example.mortarcalculator;

public class MortarType {
    private final String name;
    private final int caliber; // в миллиметрах
    private final double muzzleVelocity; // начальная скорость м/с
    private final double minRange; // минимальная дальность в метрах
    private final double maxRange; // максимальная дальность в метрах
    private final double minElevation; // минимальный угол возвышения
    private final double maxElevation; // максимальный угол возвышения
    private final double barrelLength; // длина ствола в мм
    private final double projectileWeight; // вес мины в кг
    private final double dragCoefficient; // коэффициент лобового сопротивления

    public MortarType(String name, int caliber, double muzzleVelocity, double minRange, 
                     double maxRange, double minElevation, double maxElevation, 
                     double barrelLength, double projectileWeight, double dragCoefficient) {
        this.name = name;
        this.caliber = caliber;
        this.muzzleVelocity = muzzleVelocity;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
        this.barrelLength = barrelLength;
        this.projectileWeight = projectileWeight;
        this.dragCoefficient = dragCoefficient;
    }

    // Предопределенные типы минометов
    public static final MortarType[] PREDEFINED_MORTARS = {
        new MortarType(
            "2Б14 Поднос (82мм)",
            82,
            211.0,
            50.0,
            4000.0,
            45.0,
            85.0,
            1220.0,
            3.1,
            0.295
        ),
        new MortarType(
            "M252 (81мм)",
            81,
            200.0,
            70.0,
            5935.0,
            45.0,
            85.0,
            1310.0,
            4.1,
            0.290
        ),
        new MortarType(
            "2Б11 Сани (120мм)",
            120,
            272.0,
            100.0,
            7100.0,
            45.0,
            85.0,
            1862.0,
            16.0,
            0.310
        )
    };

    // Геттеры
    public String getName() { return name; }
    public int getCaliber() { return caliber; }
    public double getMuzzleVelocity() { return muzzleVelocity; }
    public double getMinRange() { return minRange; }
    public double getMaxRange() { return maxRange; }
    public double getMinElevation() { return minElevation; }
    public double getMaxElevation() { return maxElevation; }
    public double getBarrelLength() { return barrelLength; }
    public double getProjectileWeight() { return projectileWeight; }
    public double getDragCoefficient() { return dragCoefficient; }

    @Override
    public String toString() {
        return name;
    }
} 