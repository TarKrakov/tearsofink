import com.tarkrakov.tearsofink.ink.FoodBalance;

public class FoodBalanceTest {
    private static void near(float a, float b) {
        if (Math.abs(a-b)>0.00001f) throw new AssertionError(a+" != "+b);
    }
    private static void check(int base, float sat, float soul, boolean lands, boolean vanilla, int nutrition, float saturation) {
        float m=FoodBalance.multiplier(soul,lands,vanilla);
        if (FoodBalance.nutrition(base,m)!=nutrition) throw new AssertionError("nutrition: "+base+" x "+m);
        near(FoodBalance.saturation(sat,m), saturation);
    }
    public static void main(String[] args) {
        near(FoodBalance.originMultiplier(true,true),.7f);
        near(FoodBalance.originMultiplier(false,true),1);
        near(FoodBalance.originMultiplier(true,false),1);
        near(FoodBalance.originMultiplier(false,false),1);
        near(2*FoodBalance.ROASTED_FLESHBERRY_NUTRITION*FoodBalance.ROASTED_FLESHBERRY_SATURATION_MODIFIER,6);
        check(8,12.8f,1,true,true,6,8.96f);
        check(8,12.8f,1,true,false,8,12.8f);
        check(8,12.8f,1,false,true,8,12.8f);
        check(8,12.8f,.6f,true,true,3,5.376f);
        check(8,12.8f,.8f,true,true,4,7.168f);
        check(8,12.8f,1.4f,true,true,8,12.544f);
        check(FoodBalance.CORRUPTED_MEAT_NUTRITION,FoodBalance.CORRUPTED_MEAT_SATURATION,.76f,true,false,6,10.64f);
        check(8,14,.6f,true,false,5,8.4f);
        check(8,14,.8f,true,false,6,11.2f);
        check(5,6,1,true,false,5,6);
        check(1,.2f,.6f,true,true,1,.084f);
        check(0,0,.6f,true,true,0,0);
        System.out.println("FoodBalance: origin, rounding, Soul/solace composition and food values OK");
    }
}
