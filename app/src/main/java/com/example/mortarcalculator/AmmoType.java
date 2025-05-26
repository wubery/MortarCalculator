package com.example.mortarcalculator;

public class AmmoType {
    private final String name;
    private final double weight; // вес в кг
    private final double dragCoefficient;
    private final double explosiveWeight; // вес ВВ в кг
    private final double fragmentationRadius; // радиус разлета осколков в метрах
    private final int fragmentCount; // количество осколков

    public AmmoType(String name, double weight, double dragCoefficient, 
                   double explosiveWeight, double fragmentationRadius, int fragmentCount) {
        this.name = name;
        this.weight = weight;
        this.dragCoefficient = dragCoefficient;
        this.explosiveWeight = explosiveWeight;
        this.fragmentationRadius = fragmentationRadius;
        this.fragmentCount = fragmentCount;
    }

    // Предопределенные типы боеприпасов для 82мм миномета
    public static final AmmoType[] PREDEFINED_AMMO_82MM = {
        new AmmoType(
            "О-832ДУ (Осколочный)",
            3.1,
            0.45,
            0.4,
            15.0,
            1500
        ),
        new AmmoType(
            "ОФ-832 (Осколочно-фугасный)",
            3.1,
            0.45,
            0.9, // 0.4 + 0.5 кг
            18.0,
            2000
        )
    };

    // Предопределенные типы боеприпасов для 120мм миномета
    public static final AmmoType[] PREDEFINED_AMMO_120MM = {
        new AmmoType(
            "О-843А (Осколочный)",
            16.5,
            0.48,
            1.4,
            25.0,
            2500
        ),
        new AmmoType(
            "ОФ-843Б (Осколочно-фугасный)",
            16.5,
            0.48,
            1.9, // 1.4 + 0.5 кг
            30.0,
            3000
        )
    };

    // Геттеры
    public String getName() { return name; }
    public double getWeight() { return weight; }
    public double getDragCoefficient() { return dragCoefficient; }
    public double getExplosiveWeight() { return explosiveWeight; }
    public double getFragmentationRadius() { return fragmentationRadius; }
    public int getFragmentCount() { return fragmentCount; }

    /**
     * Рассчитывает поправку к углу возвышения в зависимости от типа боеприпаса
     * @param baseAngle базовый угол возвышения в градусах
     * @return скорректированный угол возвышения в градусах
     */
    public double getAngleCorrection(double baseAngle) {
        // Для осколочно-фугасных (ОФ) добавляем поправку в 0.7 градуса
        if (name.contains("ОФ-")) {
            android.util.Log.d("AmmoType", String.format(
                "Applying angle correction for %s: %.1f° -> %.1f° (+0.7°)",
                name, baseAngle, baseAngle + 0.7));
            return baseAngle + 0.7;
        }
        // Для осколочных (О) оставляем угол без изменений
        android.util.Log.d("AmmoType", String.format(
            "No angle correction for %s: %.1f°", 
            name, baseAngle));
        return baseAngle;
    }

    @Override
    public String toString() {
        return name;
    }
} 