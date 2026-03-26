import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main {

    @FunctionalInterface
    interface Printable {
        void print();
    }

    static class HeavyBox {
        private final int weight;

        HeavyBox(int weight) {
            this.weight = weight;
        }

        int getWeight() {
            return weight;
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface DeprecatedEx {
        String message();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface JsonField {
        String name();
    }

    @DeprecatedEx(message = "Используйте класс ModernService")
    static class LegacyService {

        @DeprecatedEx(message = "Используйте метод runV2()")
        public void runOld() {
            System.out.println("Старый метод");
        }

        public void runV2() {
            System.out.println("Новый метод");
        }
    }

    static class Person {
        @JsonField(name = "first_name")
        private final String firstName;

        @JsonField(name = "age")
        private final int age;

        private final String ignoredField;

        Person(String firstName, int age, String ignoredField) {
            this.firstName = firstName;
            this.age = age;
            this.ignoredField = ignoredField;
        }
    }

    static class DeprecatedExProcessor {
        static void process(Class<?> clazz) {
            if (clazz.isAnnotationPresent(DeprecatedEx.class)) {
                DeprecatedEx annotation = clazz.getAnnotation(DeprecatedEx.class);
                System.out.printf("! класс '%s' устарел – альтернатива: '%s'%n",
                        clazz.getSimpleName(), annotation.message());
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(DeprecatedEx.class)) {
                    DeprecatedEx annotation = method.getAnnotation(DeprecatedEx.class);
                    System.out.printf("! метод '%s' устарел – альтернатива: '%s'%n",
                            method.getName(), annotation.message());
                }
            }
        }
    }

    static class JsonSerializer {
        static String toJson(Object object) {
            Field[] fields = object.getClass().getDeclaredFields();
            String body = Arrays.stream(fields)
                    .filter(field -> field.isAnnotationPresent(JsonField.class))
                    .map(field -> {
                        field.setAccessible(true);
                        JsonField jsonField = field.getAnnotation(JsonField.class);
                        try {
                            Object value = field.get(object);
                            return String.format("\"%s\": %s", jsonField.name(), formatValue(value));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Ошибка сериализации поля " + field.getName(), e);
                        }
                    })
                    .collect(Collectors.joining(", "));

            return "{" + body + "}";
        }

        private static String formatValue(Object value) {
            if (value == null) {
                return "null";
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            return "\"" + value + "\"";
        }
    }

    public static void main(String[] args) {
        System.out.println("1.1 Printable");
        Printable printable = () -> System.out.println("Печать из лямбда-выражения");
        printable.print();

        System.out.println("\n1.2 Predicate: null и пустая строка");
        Predicate<String> notNull = s -> s != null;
        Predicate<String> notEmpty = s -> !s.isEmpty();
        Predicate<String> validString = notNull.and(notEmpty);

        String value1 = "Java";
        String value2 = "";
        String value3 = null;

        System.out.println("\"Java\" -> " + validString.test(value1));
        System.out.println("\"\" -> " + validString.test(value2));
        System.out.println("null -> " + validString.test(value3));

        System.out.println("\n1.3 Predicate: начинается с J/N и заканчивается A");
        Predicate<String> startsWithJorN = s -> s.startsWith("J") || s.startsWith("N");
        Predicate<String> endsWithA = s -> s.endsWith("A");
        Predicate<String> rule = startsWithJorN.and(endsWithA);

        System.out.println("JAVA -> " + rule.test("JAVA"));
        System.out.println("NOVA -> " + rule.test("NOVA"));
        System.out.println("KOTLIN -> " + rule.test("KOTLIN"));

        System.out.println("\n1.4 Consumer + andThen для HeavyBox");
        HeavyBox heavyBox = new HeavyBox(25);
        Consumer<HeavyBox> ship = box -> System.out.println("Отгрузили ящик с весом " + box.getWeight());
        Consumer<HeavyBox> send = box -> System.out.println("Отправляем ящик с весом " + box.getWeight());
        ship.andThen(send).accept(heavyBox);

        System.out.println("\n1.5 Function для числа");
        Function<Integer, String> numberType = n -> {
            if (n > 0) {
                return "Положительное число";
            } else if (n < 0) {
                return "Отрицательное число";
            }
            return "Ноль";
        };
        System.out.println("5 -> " + numberType.apply(5));
        System.out.println("-7 -> " + numberType.apply(-7));
        System.out.println("0 -> " + numberType.apply(0));

        System.out.println("\n1.6 Supplier случайного числа [0..10]");
        Supplier<Integer> random0to10 = () -> new Random().nextInt(11);
        System.out.println("Случайное число: " + random0to10.get());

        System.out.println("\n2.1 Обработка @DeprecatedEx");
        DeprecatedExProcessor.process(LegacyService.class);

        System.out.println("\n2.2 Сериализация @JsonField");
        Person person = new Person("Иван", 21, "ignored");
        System.out.println(JsonSerializer.toJson(person));
    }
}
