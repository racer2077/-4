package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) {
        task11Printable();
        task12Predicates();
        task13PredicateStringCheck();
        task14ConsumerHeavyBox();
        task15FunctionNumberSign();
        task16SupplierRandom();

        DeprecatedExHandler.printWarnings(OldService.class);
        DeprecatedExHandler.printWarnings(LegacyClass.class);

        Person person = new Person("Иван", "Иванов", 27);
        String json = JsonSerializer.serialize(person);
        System.out.println(json);
    }

    private static void task11Printable() {
        Printable printable = () -> System.out.println("Печать из лямбда-выражения");
        printable.print();
    }

    private static void task12Predicates() {
        Predicate<String> isNotNull = str -> str != null;
        Predicate<String> isNotEmpty = str -> !str.isEmpty();
        Predicate<String> isNotNullAndNotEmpty = isNotNull.and(isNotEmpty);

        System.out.println("isNotNull(null): " + isNotNull.test(null));
        System.out.println("isNotNull(\"text\"): " + isNotNull.test("text"));
        System.out.println("isNotEmpty(\"\"): " + isNotEmpty.test(""));
        System.out.println("isNotNullAndNotEmpty(\"\"): " + isNotNullAndNotEmpty.test(""));
        System.out.println("isNotNullAndNotEmpty(\"Java\"): " + isNotNullAndNotEmpty.test("Java"));
    }

    private static void task13PredicateStringCheck() {
        Predicate<String> startsWithJorN = str -> str.startsWith("J") || str.startsWith("N");
        Predicate<String> endsWithA = str -> str.endsWith("A");
        Predicate<String> fullRule = startsWithJorN.and(endsWithA);

        System.out.println("JAVA -> " + fullRule.test("JAVA"));
        System.out.println("NOVA -> " + fullRule.test("NOVA"));
        System.out.println("KOTLIN -> " + fullRule.test("KOTLIN"));
    }

    private static void task14ConsumerHeavyBox() {
        Consumer<HeavyBox> shipped = box ->
                System.out.println("Отгрузили ящик с весом " + box.getWeight());
        Consumer<HeavyBox> sending = box ->
                System.out.println("Отправляем ящик с весом " + box.getWeight());

        HeavyBox heavyBox = new HeavyBox(54);
        shipped.andThen(sending).accept(heavyBox);
    }

    private static void task15FunctionNumberSign() {
        Function<Integer, String> numberSign = number -> {
            if (number > 0) {
                return "Положительное число";
            }
            if (number < 0) {
                return "Отрицательное число";
            }
            return "Ноль";
        };

        System.out.println("7 -> " + numberSign.apply(7));
        System.out.println("-3 -> " + numberSign.apply(-3));
        System.out.println("0 -> " + numberSign.apply(0));
    }

    private static void task16SupplierRandom() {
        Random random = new Random();
        Supplier<Integer> randomZeroToTen = () -> random.nextInt(11);

        System.out.println("Случайное число [0..10]: " + randomZeroToTen.get());
    }
}

@FunctionalInterface
interface Printable {
    void print();
}

class HeavyBox {
    private final int weight;

    public HeavyBox(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface DeprecatedEx {
    String message();
}

class DeprecatedExHandler {
    public static void printWarnings(Class<?> clazz) {
        if (clazz.isAnnotationPresent(DeprecatedEx.class)) {
            DeprecatedEx annotation = clazz.getAnnotation(DeprecatedEx.class);
            System.out.println("! класс '" + clazz.getSimpleName() + "' устарел – альтернатива: '" + annotation.message() + "'");
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(DeprecatedEx.class)) {
                DeprecatedEx annotation = method.getAnnotation(DeprecatedEx.class);
                System.out.println("! метод '" + method.getName() + "' устарел – альтернатива: '" + annotation.message() + "'");
            }
        }
    }
}

class OldService {
    @DeprecatedEx(message = "newApiMethod()")
    public void oldApiMethod() {
    }

    public void activeMethod() {
    }
}

@DeprecatedEx(message = "ModernService")
class LegacyClass {
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface JsonField {
    String name();
}

class JsonSerializer {
    public static String serialize(Object object) {
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        StringBuilder json = new StringBuilder("{");

        boolean first = true;
        for (Field field : fields) {
            if (!field.isAnnotationPresent(JsonField.class)) {
                continue;
            }

            field.setAccessible(true);
            JsonField annotation = field.getAnnotation(JsonField.class);
            Object value;
            try {
                value = field.get(object);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Ошибка доступа к полю: " + field.getName(), e);
            }

            if (!first) {
                json.append(", ");
            }
            first = false;

            json.append("\"").append(annotation.name()).append("\": ");
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
        }

        json.append("}");
        return json.toString();
    }
}

class Person {
    @JsonField(name = "first_name")
    private final String firstName;

    @JsonField(name = "last_name")
    private final String lastName;

    @JsonField(name = "age")
    private final int age;

    public Person(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }
}
