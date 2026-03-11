package com.example.custom_calculator_application;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final double CUSTOM_FACTOR = 1.60;

    private TextView tvDisplay;
    private StringBuilder currentInput = new StringBuilder("0");
    private Double operandA = null;
    private char pendingOp = 0;
    private boolean isError = false;

    private final DecimalFormat df = new DecimalFormat("0.##########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = findViewById(R.id.tvDisplay);
        setupButtons();

        if (savedInstanceState != null) {
            currentInput = new StringBuilder(savedInstanceState.getString("display", "0"));
            operandA = (Double) savedInstanceState.getSerializable("operandA");
            pendingOp = savedInstanceState.getChar("pendingOp");
            isError = savedInstanceState.getBoolean("isError");
        }
        render();
    }

    private void setupButtons() {
        int[] numIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());
        findViewById(R.id.btnSign).setOnClickListener(v -> toggleSign());
        findViewById(R.id.btnAdd).setOnClickListener(v -> chooseOperator('+'));
        findViewById(R.id.btnSub).setOnClickListener(v -> chooseOperator('-'));
        findViewById(R.id.btnMul).setOnClickListener(v -> chooseOperator('*'));
        findViewById(R.id.btnDiv).setOnClickListener(v -> chooseOperator('/'));
        findViewById(R.id.btnEq).setOnClickListener(v -> evaluate());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnDel).setOnClickListener(v -> deleteOne());
        findViewById(R.id.btnCustom).setOnClickListener(v -> applyCustomOperator());
    }

    private void appendDigit(String d) {
        if (isError) clearAll();
        if (currentInput.toString().equals("0")) currentInput = new StringBuilder(d);
        else currentInput.append(d);
        render();
    }

    private void appendDot() {
        if (isError) clearAll();
        if (!currentInput.toString().contains(".")) currentInput.append(".");
        render();
    }

    private void toggleSign() {
        if (isError) return;
        String s = currentInput.toString();
        if (s.equals("0")) return;
        if (s.startsWith("-")) currentInput = new StringBuilder(s.substring(1));
        else currentInput = new StringBuilder("-" + s);
        render();
    }

    private void chooseOperator(char op) {
        if (isError) return;
        operandA = parseDisplay();
        pendingOp = op;
        currentInput = new StringBuilder("0");
        render();
    }

    private void evaluate() {
        if (isError || operandA == null || pendingOp == 0) return;
        double b = parseDisplay();
        double a = operandA;
        double result = 0;

        if (pendingOp == '/' && b == 0) {
            isError = true;
            tvDisplay.setText("Error");
            return;
        }

        switch (pendingOp) {
            case '+': result = a + b; break;
            case '-': result = a - b; break;
            case '*': result = a * b; break;
            case '/': result = a / b; break;
        }

        operandA = null;
        pendingOp = 0;
        currentInput = new StringBuilder(df.format(result));
        render();
    }

    private void applyCustomOperator() {
        if (isError) return;
        double result = parseDisplay() * CUSTOM_FACTOR;
        currentInput = new StringBuilder(df.format(result));
        render();
    }

    private void deleteOne() {
        if (isError) { clearAll(); return; }
        if (currentInput.length() > 1) currentInput.deleteCharAt(currentInput.length() - 1);
        else currentInput = new StringBuilder("0");
        render();
    }

    private void clearAll() {
        operandA = null;
        pendingOp = 0;
        isError = false;
        currentInput = new StringBuilder("0");
        render();
    }

    private double parseDisplay() {
        try { return Double.parseDouble(currentInput.toString()); }
        catch (Exception e) { return 0; }
    }

    private void render() {
        if (!isError) tvDisplay.setText(currentInput.toString());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("display", currentInput.toString());
        outState.putSerializable("operandA", operandA);
        outState.putChar("pendingOp", pendingOp);
        outState.putBoolean("isError", isError);
    }
}