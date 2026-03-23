package dk.ashlan.agent.chapters.chapter03;

import dk.ashlan.agent.tools.CalculatorTool;

public class CalculatorToolDemo {
    private final CalculatorTool calculatorTool = new CalculatorTool();

    public String run() {
        return calculatorTool.calculate("25 * 4");
    }
}
