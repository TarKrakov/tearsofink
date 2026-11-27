"""Exercise the actual dependency-free Awakening price method under Java 21+."""
import re
import subprocess
import tempfile
from pathlib import Path

root = Path("src/main/java/com/tarkrakov/tearsofink/weapon/forge")
awakening = (root / "Awakening.java").read_text()
constants = re.search(r"public static final int BASE_COST = \d+, COST_STEP = \d+, MAX_COST = \d+;", awakening).group()
method = re.search(r"public static int rollCost\(int previousRolls\) \{[^}]+\}", awakening).group()
assert "return rollCost(rolls(stack));" in awakening
menu = (root / "CursedAltarMenu.java").read_text()
assert "int cost = Awakening.cost(weapon);" in menu
assert "reagent.shrink(cost);" in menu and "Awakening.addRoll(weapon);" in menu
assert "cat.shrink(1);" in menu
screen = Path("src/client/java/com/tarkrakov/tearsofink/client/forge/CursedAltarScreen.java").read_text()
assert "Awakening.cost(w)" in screen
with tempfile.TemporaryDirectory() as directory:
    java = Path(directory) / "AltarCostTest.java"
    java.write_text("public class AltarCostTest {\n" + constants + "\n" + method + """
    public static void main(String[] args) {
        int[] expected = {2,3,4,5,6,7,8,8,8,8};
        for (int i=0;i<expected.length;i++) {
            if (rollCost(i)!=expected[i] || rollCost(i)*2!=Math.min(16,4+2*i))
                throw new AssertionError("roll " + i);
        }
        if (rollCost(-1)!=2 || rollCost(Integer.MAX_VALUE)!=8) throw new AssertionError();
    }
}
""")
    subprocess.run(["java", str(java)], check=True)
print("Cursed Altar: first roll 2, +1 each, cap 8; client and server share cost")
