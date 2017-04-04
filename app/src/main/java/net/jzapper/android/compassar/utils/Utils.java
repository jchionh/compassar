package net.jzapper.android.compassar.utils;

/**
 * Created by jchionh
 * Date: 4/3/17
 * Time: 10:26 PM
 */

public class Utils
{
    public static float lowPass(float newValue, float previousValue, float alpha) {
        return previousValue + (alpha * (newValue - previousValue));
    }
}
