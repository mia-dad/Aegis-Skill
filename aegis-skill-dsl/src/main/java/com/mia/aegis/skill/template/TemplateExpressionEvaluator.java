package com.mia.aegis.skill.template;

import com.mia.aegis.skill.template.expr.*;

import java.util.List;
import java.util.Map;

/**
 * 模板表达式求值器。
 *
 * <p>对解析后的 AST 节点进行求值，从上下文 Map 中解析变量值。</p>
 *
 * @since 0.3.0
 */
public class TemplateExpressionEvaluator {

    /**
     * 求值表达式。
     *
     * @param expr    AST 节点
     * @param context 变量上下文
     * @return 求值结果
     */
    public Object evaluate(TemplateExpression expr, Map<String, Object> context) {
        if (expr instanceof NumberLiteralExpr) {
            return ((NumberLiteralExpr) expr).getValue();
        }

        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        }

        if (expr instanceof CurrentElementExpr) {
            return context.get("_");
        }

        if (expr instanceof VariableExpr) {
            return evaluateVariable((VariableExpr) expr, context);
        }

        if (expr instanceof ArrayAccessExpr) {
            return evaluateArrayAccess((ArrayAccessExpr) expr, context);
        }

        if (expr instanceof ArithmeticExpr) {
            return evaluateArithmetic((ArithmeticExpr) expr, context);
        }

        return null;
    }

    /**
     * 将求值结果转换为字符串。
     *
     * <p>规则：null→""，整数不带小数点，其他调用 toString。</p>
     *
     * @param value 求值结果
     * @return 字符串表示
     */
    public String toDisplayString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        if (value instanceof Float) {
            float f = (Float) value;
            if (f == Math.floor(f) && !Float.isInfinite(f)) {
                return String.valueOf((long) f);
            }
            return String.valueOf(f);
        }
        return value.toString();
    }

    private Object evaluateVariable(VariableExpr expr, Map<String, Object> context) {
        List<String> segments = expr.getPathSegments();
        if (segments.isEmpty() || (segments.size() == 1 && segments.get(0).isEmpty())) {
            return null;
        }

        Object current = context.get(segments.get(0));
        for (int i = 1; i < segments.size(); i++) {
            current = resolveField(current, segments.get(i));
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private Object evaluateArrayAccess(ArrayAccessExpr expr, Map<String, Object> context) {
        Object arrayObj = evaluate(expr.getArrayExpr(), context);
        if (!(arrayObj instanceof List)) {
            return null;
        }

        List<Object> list = (List<Object>) arrayObj;

        int index;
        if (expr.isVariableIndex()) {
            // 变量索引 [#var]
            Object idxVal = context.get(expr.getVariableIndex());
            if (idxVal == null) {
                return null;
            }
            index = toInt(idxVal);
        } else {
            index = expr.getLiteralIndex();
        }

        if (index < 0 || index >= list.size()) {
            return null;
        }

        Object element = list.get(index);

        // 处理 trailing path (e.g., arr[0].field)
        for (String field : expr.getTrailingPath()) {
            element = resolveField(element, field);
            if (element == null) {
                return null;
            }
        }

        return element;
    }

    private Object evaluateArithmetic(ArithmeticExpr expr, Map<String, Object> context) {
        Object leftVal = evaluate(expr.getLeft(), context);
        Object rightVal = evaluate(expr.getRight(), context);

        ArithmeticExpr.Op op = expr.getOp();

        // + 特殊处理：任一侧为 String 则字符串拼接
        if (op == ArithmeticExpr.Op.ADD) {
            if (leftVal instanceof String || rightVal instanceof String) {
                return toDisplayString(leftVal) + toDisplayString(rightVal);
            }
        }

        // 数值运算
        double l = toDouble(leftVal);
        double r = toDouble(rightVal);

        switch (op) {
            case ADD: return l + r;
            case SUB: return l - r;
            case MUL: return l * r;
            case DIV:
                if (r == 0) return 0.0;
                return l / r;
            default:
                return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolveField(Object target, String field) {
        if (target == null) {
            return null;
        }
        if (target instanceof Map) {
            return ((Map<String, Object>) target).get(field);
        }
        // POJO getter 访问（反射）
        String capitalized = Character.toUpperCase(field.charAt(0)) + field.substring(1);
        try {
            java.lang.reflect.Method getter = target.getClass().getMethod("get" + capitalized);
            return getter.invoke(target);
        } catch (NoSuchMethodException e) {
            // 尝试 isXxx
        } catch (Exception e) {
            return null;
        }
        try {
            java.lang.reflect.Method isGetter = target.getClass().getMethod("is" + capitalized);
            return isGetter.invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
