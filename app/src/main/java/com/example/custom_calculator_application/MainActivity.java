package com.example.custom_calculator_application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    // ====== Custom operator factor (change to match your last 3 digits) ======
    // Example in prompt: ID ends 456 -> multiply by 4.56
    private static final double CUSTOM_FACTOR = 4.56;

    // ====== State keys ======
    private static final String KEY_DISPLAY = "state_display";
    private static final String KEY_EXPRESSION = "state_expression";
    private static final String KEY_HISTORY = "state_history";
    private static final String KEY_OPERAND_A = "state_operand_a";
    private static final String KEY_PENDING_OP = "state_pending_op";
    private static final String KEY_TYPING_B = "state_typing_b";
    private static final String KEY_ERROR = "state_error";

    // ====== Views ======
    private TextView tvDisplay;
    private TextView tvExpression;
    private TextView tvHistory;
    private ScrollView svHistory;
    private Switch switchTheme;

    // ====== Calculator state ======
    private StringBuilder currentInput = new StringBuilder("0");
    private Double operandA = null;
    private char pendingOp = 0; // '+', '-', '*', '/'
    private boolean isTypingSecond = false;
    private boolean isError = false;

    private final DecimalFormat df = new DecimalFormat("0.##########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupThemeToggle();
        setupButtons();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        } else {
            render();
        }
    }

    private void bindViews() {
        tvDisplay = findViewById(R.id.tvDisplay);
        tvExpression = findViewById(R.id.tvExpression);
        tvHistory = findViewById(R.id.tvHistory);
        svHistory = findViewById(R.id.svHistory);
        switchTheme = findViewById(R.id.switchTheme);
    }

    private void setupThemeToggle() {
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark_mode", true);

        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        switchTheme.setChecked(dark);

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void setupButtons() {
        // Numbers
        int[] numIds = new int[]{
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };
        for (int id : numIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
        }

        // Dot
        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());

        // Sign
        findViewById(R.id.btnSign).setOnClickListener(v -> toggleSign());

        // Operators
        findViewById(R.id.btnAdd).setOnClickListener(v -> chooseOperator('+'));
        findViewById(R.id.btnSub).setOnClickListener(v -> chooseOperator('-'));
        findViewById(R.id.btnMul).setOnClickListener(v -> chooseOperator('*'));
        findViewById(R.id.btnDiv).setOnClickListener(v -> chooseOperator('/'));

        // Equals
        findViewById(R.id.btnEq).setOnClickListener(v -> evaluate());

        // Clear / Delete
        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
        findViewById(R.id.btnDel).setOnClickListener(v -> deleteOne());

        // Custom operator
        findViewById(R.id.btnCustom).setOnClickListener(v -> applyCustomOperator());
    }

    private void appendDigit(String d) {
        if (isError) clearAll();

        if (currentInput.toString().equals("0")) {
            currentInput = new StringBuilder(d);
        } else {
            currentInput.append(d);
        }
        render();
    }

    private void appendDot() {
        if (isError) clearAll();

        String s = currentInput.toString();
        if (!s.contains(".")) {
            currentInput.append(".");
        }
        render();
    }

    private void toggleSign() {
        if (isError) return;

        String s = currentInput.toString();
        if (s.equals("0") || s.equals("0.")) return;

        if (s.startsWith("-")) currentInput = new StringBuilder(s.substring(1));
        else currentInput = new StringBuilder("-" + s);

        render();
    }

    private void chooseOperator(char op) {
        if (isError) return;

        double value = parseDisplay();
        if (operandA == null) {
            operandA = value;
            pendingOp = op;
            isTypingSecond = true;
            currentInput = new StringBuilder("0");
        } else {
            // Not required to chain multiple operators; we’ll just replace operator if user changes mind
            pendingOp = op;
        }

        tvExpression.setText(df.format(operandA) + " " + symbolFor(pendingOp));
        renderDisplayOnly(); // keep expression
    }

    private void evaluate() {
        if (isError) return;

        if (operandA == null || pendingOp == 0) {
            render();
            return;
        }

        double b = parseDisplay();
        double a = operandA;

        if (pendingOp == '/' && b == 0.0) {
            // Required: custom message, no crash
            setError("Cannot divide by zero");
            return;
        }

        double result;
        switch (pendingOp) {
            case '+':
                result = a + b;
                break;
            case '-':
                result = a - b;
                break;
            case '*':
                result = a * b;
                break;
            case '/':
                result = a / b;
                break;
            default:
                result = b;
        }

        String expr = df.format(a) + " " + symbolFor(pendingOp) + " " + df.format(b);
        appendHistory(expr + " = " + df.format(result));

        // Reset for next calculation
        operandA = null;
        pendingOp = 0;
        isTypingSecond = false;

        currentInput = new StringBuilder(df.format(result));
        tvExpression.setText("");
        renderDisplayOnly();
    }

    private void applyCustomOperator() {
        if (isError) return;

        double x = parseDisplay();
        double result = x * CUSTOM_FACTOR;

        appendHistory("CUST(" + df.format(x) + ") = " + df.format(result));

        currentInput = new StringBuilder(df.format(result));
        // Keep any pending expression as-is (simple and predictable for 2-operand requirement)
        renderDisplayOnly();
    }

    private void deleteOne() {
        if (isError) {
            clearAll();
            return;
        }

        String s = currentInput.toString();
        if (s.length() <= 1 || (s.length() == 2 && s.startsWith("-"))) {
            currentInput = new StringBuilder("0");
        } else {
            currentInput.deleteCharAt(currentInput.length() - 1);
        }
        render();
    }

    private void clearAll() {
        operandA = null;
        pendingOp = 0;
        isTypingSecond = false;
        isError = false;

        currentInput = new StringBuilder("0");
        tvExpression.setText("");
        renderDisplayOnly();
    }

    private void setError(String msg) {
        isError = true;
        tvDisplay.setText(msg);
        // keep expression visible so the user knows what happened
    }

    private double parseDisplay() {
        String s = currentInput.toString();
        if (s.equals("") || s.equals("-") || s.equals(".")) return 0.0;
        return Double.parseDouble(s);
    }

    private String symbolFor(char op) {
        switch (op) {
            case '+':
                return "+";
            case '-':
                return "−";
            case '*':
                return "×";
            case '/':
                return "÷";
            default:
                return "";
        }
    }

    private void appendHistory(String line) {
        String old = tvHistory.getText().toString();
        String updated = old.isEmpty() ? line : (old + "\n" + line);
        tvHistory.setText(updated);

        // Scroll to bottom
        svHistory.post(() -> svHistory.fullScroll(View.FOCUS_DOWN));
    }

    private void render() {
        if (!isError) {
            tvDisplay.setText(currentInput.toString());
            if (operandA != null && pendingOp != 0) {
                tvExpression.setText(df.format(operandA) + " " + symbolFor(pendingOp));
            }
        }
    }

    private void renderDisplayOnly() {
        if (!isError) {
            tvDisplay.setText(currentInput.toString());
        }
    }

    // ====== Milestone 3 requirement: onSaveInstanceState ======
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_DISPLAY, currentInput.toString());
        outState.putString(KEY_EXPRESSION, tvExpression.getText().toString());
        outState.putString(KEY_HISTORY, tvHistory.getText().toString());
        outState.putSerializable(KEY_OPERAND_A, operandA);
        outState.putChar(KEY_PENDING_OP, pendingOp);
        outState.putBoolean(KEY_TYPING_B, isTypingSecond);
        outState.putBoolean(KEY_ERROR, isError);
    }

    private void restoreState(@NonNull Bundle state) {
        String display = state.getString(KEY_DISPLAY, "0");
        String expression = state.getString(KEY_EXPRESSION, "");
        String history = state.getString(KEY_HISTORY, "");

        Object oa = state.getSerializable(KEY_OPERAND_A);
        operandA = (oa instanceof Double) ? (Double) oa : null;

        pendingOp = state.getChar(KEY_PENDING_OP, (char) 0);
        isTypingSecond = state.getBoolean(KEY_TYPING_B, false);
        isError = state.getBoolean(KEY_ERROR, false);

        currentInput = new StringBuilder(display);
        tvExpression.setText(expression);
        tvHistory.setText(history);

        if (isError) {
            // If error happened, keep display text as whatever it was (usually message).
            tvDisplay.setText(display);
        } else {
            renderDisplayOnly();
        }
    }
}