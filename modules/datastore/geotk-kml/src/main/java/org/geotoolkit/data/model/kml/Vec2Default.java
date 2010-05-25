package org.geotoolkit.data.model.kml;

/**
 *
 * @author Samuel Andrés
 */
public class Vec2Default implements Vec2 {

    private double x;
    private double y;
    private Units xUnit;
    private Units yUnit;

    public Vec2Default(double x, double y, Units xUnit, Units yUnit){
        this.x = x;
        this.y = y;
        this.xUnit = xUnit;
        this.yUnit = yUnit;
    }

    @Override
    public double getX() {return this.x;}

    @Override
    public double getY() {return this.y;}

    @Override
    public Units getXUnits() {return this.xUnit;}

    @Override
    public Units getYUnits() {return this.yUnit;}

    @Override
    public String toString() {
        return "Vec2Default : " +
                "\n\tx : " +this.x+
                "\n\ty : " +this.y+
                "\n\txUnit : " +this.xUnit+
                "\n\tyUnit : "+this.yUnit;
    }

}
