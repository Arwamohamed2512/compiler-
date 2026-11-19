import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SemanticAnalyzer {

    // أسماء المتغيرات التي تم تعريفها أثناء المرور على البرنامج.
    private final Set<String> declared = new HashSet<>();

    // يتأكد من أن كل اسم متغير مستخدم في التعبير قد تم تعريفه.
    private void checkExpr(parser.Expr expr) {
        if (expr == null) return;

        if (expr.kind == parser.ExprKind.NAME) {
            if (!declared.contains(expr.value)) {
                throw new RuntimeException("Semantic error: variable '" +
                        expr.value + "' is not declared");
            }
        } else if (expr.kind == parser.ExprKind.BINARY) {
            checkExpr(expr.left);
            checkExpr(expr.right);
        }
    }

    // يتحقق من صحة كل جملة، ويتابع تعريف المتغيرات داخل البرنامج.
    private void checkStatements(List<parser.Stmt> statements) {
        if (statements == null) return;

        for (parser.Stmt s : statements) {
            switch (s.kind) {
                case LET:
                    // نتحقق من التعبير قبل تسجيل المتغير الجديد، ونمنع إعادة تعريفه.
                    checkExpr(s.expr);
                    if (declared.contains(s.name)) {
                        throw new RuntimeException("Semantic error: variable '" +
                                s.name + "' is declared twice");
                    }
                    declared.add(s.name);
                    break;

                case ASSIGN:
                    // لا يمكن إسناد قيمة إلى متغير لم يتم تعريفه مسبقًا.
                    if (!declared.contains(s.name)) {
                        throw new RuntimeException("Semantic error: variable '" +
                                s.name + "' is not declared");
                    }
                    checkExpr(s.expr);
                    break;

                case PRINT:
                    // يجب أن تكون كل المتغيرات المستخدمة للطباعة معرفة.
                    checkExpr(s.expr);
                    break;

                case IF:
                    // نتحقق من الشرط ومن الجمل في كلا الفرعين.
                    checkExpr(s.condition);
                    checkStatements(s.body);
                    checkStatements(s.elseBody);
                    break;

                case WHILE:
                    // نتحقق من شرط الحلقة ومن الجمل التي تنفذ داخلها.
                    checkExpr(s.condition);
                    checkStatements(s.body);
                    break;

                default:
                    throw new IllegalStateException("Unknown statement kind");
            }
        }
    }

    // يبدأ التحليل الدلالي من قائمة تعريفات فارغة.
    void analyze(List<parser.Stmt> program) {
        declared.clear();
        checkStatements(program);
    }

    // ================= PIPELINE TEST =================
    public static void main(String[] args) {
        String[] testCases = {
            // 1. Valid program
            "let age = 20\nlet score = age + 10\nprint score\n",

            // 2. Valid program: if/else and while
            "let x = 1\nif x = 1\nlet y = 2\nprint y\nelse\nlet z = 3\nprint z\nend\n",

            // 3. Error: variable used before declaration
            "print total\n",

            // 4. Error: variable assigned before declaration
            "count = 5\n",

            // 5. Error: duplicate declaration
            "let a = 10\nlet a = 20\n"
        };

        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        for (int i = 0; i < testCases.length; i++) {
            String source = testCases[i];
            System.out.println("=== Test " + (i + 1) + " ===");
            System.out.println(source.trim());
            try {
                List<lexer.Token> tokens = new lexer.Lexer(source).scan();
                List<parser.Stmt> ast = new parser(tokens).parseProgram();
                analyzer.analyze(ast);
                System.out.println("Result: Success!\n");
            } catch (RuntimeException e) {
                System.out.println("Result: " + e.getMessage() + "\n");
            }
        }
    }
}
