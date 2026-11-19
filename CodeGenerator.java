import java.util.List;

public class CodeGenerator {

    // ينشئ مسافات بادئة وفق عمق الكتلة لتنسيق الشيفرة الناتجة.
    private String indent(int depth) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < depth; i++) s.append("    ");
        return s.toString();
    }

    // يحول عقدة التعبير إلى نص Java، مع وضع أقواس حول العمليات الثنائية.
    private String expression(parser.Expr expr) {
        if (expr.kind == parser.ExprKind.NUMBER || expr.kind == parser.ExprKind.NAME) {
            return expr.value;
        }
        return "(" + expression(expr.left) + " " + expr.value + " " +
                expression(expr.right) + ")";
    }

    // يولد شيفرة Java لكل جملة في القائمة، مع مراعاة مستوى التداخل.
    private void statements(List<parser.Stmt> list, StringBuilder out, int depth) {
        for (parser.Stmt s : list) {
            String pad = indent(depth);
            switch (s.kind) {
                case LET:
                    // تحويل تعريف المتغير في MiniLang إلى تعريف عدد صحيح في Java.
                    out.append(pad).append("int ").append(s.name).append(" = ")
                            .append(expression(s.expr)).append(";\n");
                    break;
                case ASSIGN:
                    // تحويل جملة الإسناد إلى صيغة الإسناد في Java.
                    out.append(pad).append(s.name).append(" = ")
                            .append(expression(s.expr)).append(";\n");
                    break;
                case PRINT:
                    // تحويل الطباعة إلى استدعاء System.out.println.
                    out.append(pad).append("System.out.println(")
                            .append(expression(s.expr)).append(");\n");
                    break;
                case IF:
                    // توليد شرط IF، ثم جسمه وفرع ELSE عند وجود جمل فيه.
                    out.append(pad).append("if (").append(expression(s.condition)).append(") {\n");
                    statements(s.body, out, depth + 1);
                    if (!s.elseBody.isEmpty()) {
                        out.append(pad).append("} else {\n");
                        statements(s.elseBody, out, depth + 1);
                    }
                    out.append(pad).append("}\n");
                    break;
                case WHILE:
                    // توليد حلقة WHILE وجسمها مع زيادة المسافة البادئة.
                    out.append(pad).append("while (").append(expression(s.condition)).append(") {\n");
                    statements(s.body, out, depth + 1);
                    out.append(pad).append("}\n");
                    break;
                default:
                    throw new IllegalStateException("Unknown statement kind");
            }
        }
    }

    // ينشئ هيكل Java الأساسي ثم يضيف إليه جمل البرنامج المترجم.
    public String generate(List<parser.Stmt> program) {
        StringBuilder out = new StringBuilder();
        out.append("class GeneratedProgram {\n")
                .append("    public static void main(String[] args) {\n");
        statements(program, out, 2);
        out.append("    }\n}");
        return out.toString();
    }

    // ================= FULL COMPILER PIPELINE DEMO =================
    public static void main(String[] args) {
        String source = 
            "let age = 20\n" +
            "let score = age + 10\n" +
            "print score\n" +
            "if score = 30\n" +
            "    print 1\n" +
            "else\n" +
            "    print 0\n" +
            "end\n" +
            "while age = 20\n" +
            "    age = age + 1\n" +
            "end\n";

        System.out.println("=== 1. Source Code ===");
        System.out.println(source);

        try {
            // 1. Lexical Analysis
            lexer.Lexer lex = new lexer.Lexer(source);
            List<lexer.Token> tokens = lex.scan();

            // 2. Syntax Analysis (Parsing)
            parser pars = new parser(tokens);
            List<parser.Stmt> ast = pars.parseProgram();

            // 3. Semantic Analysis
            SemanticAnalyzer sema = new SemanticAnalyzer();
            sema.analyze(ast);
            System.out.println("=== 2. Semantic Analysis ===");
            System.out.println("Semantic checks passed successfully!\n");

            // 4. Code Generation
            CodeGenerator codegen = new CodeGenerator();
            String javaCode = codegen.generate(ast);
            System.out.println("=== 3. Generated Java Source Code ===");
            System.out.println(javaCode);

        } catch (Exception e) {
            System.err.println("Compilation Error: " + e.getMessage());
        }
    }
}
