import java.util.ArrayList;
import java.util.List;


public class parser {

    // ================= EXPRESSIONS =================

    enum ExprKind {
        NUMBER, NAME, BINARY
    }

    static final class Expr {
        final ExprKind kind;
        final String value;
        final Expr left;
        final Expr right;

        Expr(ExprKind kind, String value, Expr left, Expr right) {
            this.kind = kind;
            this.value = value;
            this.left = left;
            this.right = right;
        }

        static Expr number(String value) {
            return new Expr(ExprKind.NUMBER, value, null, null);
        }

        static Expr name(String value) {
            return new Expr(ExprKind.NAME, value, null, null);
        }

        static Expr binary(String op, Expr left, Expr right) {
            return new Expr(ExprKind.BINARY, op, left, right);
        }
    }

    // ================= STATEMENTS =================

    enum StmtKind {
        LET, ASSIGN, PRINT, IF, WHILE
    }

    static final class Stmt {
        final StmtKind kind;
        final String name;
        final Expr expr;
        final Expr condition;
        final List<Stmt> body;
        final List<Stmt> elseBody;

        Stmt(StmtKind kind, String name, Expr expr, Expr condition,
             List<Stmt> body, List<Stmt> elseBody) {

            this.kind = kind;
            this.name = name;
            this.expr = expr;
            this.condition = condition;
            this.body = body == null ? new ArrayList<>() : body;
            this.elseBody = elseBody == null ? new ArrayList<>() : elseBody;
        }

        static Stmt let(String name, Expr expr) {
            return new Stmt(StmtKind.LET, name, expr, null, null, null);
        }

        static Stmt assign(String name, Expr expr) {
            return new Stmt(StmtKind.ASSIGN, name, expr, null, null, null);
        }

        static Stmt print(Expr expr) {
            return new Stmt(StmtKind.PRINT, null, expr, null, null, null);
        }

        static Stmt ifStmt(Expr condition, List<Stmt> thenBody,
                           List<Stmt> elseBody) {
            return new Stmt(StmtKind.IF, null, null, condition,
                    thenBody, elseBody);
        }

        static Stmt whileStmt(Expr condition, List<Stmt> body) {
            return new Stmt(StmtKind.WHILE, null, null, condition,
                    body, null);
        }
    }

    // ================= PARSER =================

    private final List<lexer.Token>tokens;
    private int pos = 0;

    parser(List<lexer.Token> tokens) {
        this.tokens = tokens;
    }

    private lexer.Token current() {
        if (pos < tokens.size()) return tokens.get(pos);
        return tokens.get(tokens.size() - 1);
    }

    private lexer.Token lookAhead() {
        if (pos + 1 < tokens.size()) return tokens.get(pos + 1);
        return tokens.get(tokens.size() - 1);
    }

    private boolean match(lexer.TokenType type) {
        if (current().type == type) {
            pos++;
            return true;
        }
        return false;
    }

    private lexer.Token consume(lexer.TokenType type, String message) {
        lexer.Token t = current();
        if (t.type != type) {
            error(message);
        }
        pos++;
        return t;
    }

    private void error(String message) {
        lexer.Token t = current();
        throw new RuntimeException("Parse Error at line " + t.line
                + ", column " + t.column + ": " + message
                + " (found " + t.type + ")");
    }

    private List<Stmt> parseBlock(boolean stopAtElse) {
        List<Stmt> stmts = new ArrayList<>();
        while (true) {
            while (match(lexer.TokenType.NEWLINE)) { }
            lexer.TokenType t = current().type;
            if (t == lexer.TokenType.EOF || t == lexer.TokenType.End
                    || (stopAtElse && t == lexer.TokenType.Else)) {
                break;
            }
            stmts.add(parseStatement());
        }
        return stmts;
    }

    private Stmt parseStatement() {
        lexer.Token t = current();

        switch (t.type) {
            case let: {
                pos++;
                lexer.Token name = consume(lexer.TokenType.Var, "Expected variable name after 'let'");
                consume(lexer.TokenType.EQUALS, "Expected '=' in let statement");
                return Stmt.let(name.text, parseExpression());
            }
            case print: {
                pos++;
                return Stmt.print(parseExpression());
            }
            case IF: {
                pos++;
                Expr cond = parseCondition();
                List<Stmt> thenBody = parseBlock(true);
                List<Stmt> elseBody = null;
                if (match(lexer.TokenType.Else)) {
                    elseBody = parseBlock(false);
                }
                consume(lexer.TokenType.End, "Expected 'end' after if statement");
                return Stmt.ifStmt(cond, thenBody, elseBody);
            }
            case While: {
                pos++;
                Expr cond = parseCondition();
                List<Stmt> body = parseBlock(false);
                consume(lexer.TokenType.End, "Expected 'end' after while statement");
                return Stmt.whileStmt(cond, body);
            }
            case Var: {
                pos++;
                consume(lexer.TokenType.EQUALS, "Expected '=' in assignment");
                return Stmt.assign(t.text, parseExpression());
            }
            default:
                error("Unexpected token at start of statement");
                return null;
        }
    }

    private Expr parseCondition() {
        Expr left = parseExpression();
        if (current().type == lexer.TokenType.EQUALS) {
            String op = current().text;
            pos++;
            return Expr.binary(op, left, parseExpression());
        }
        return left;
    }

    private Expr parseExpression() {
        Expr left = parseTerm();
        while (current().type == lexer.TokenType.PLUS
                || current().type == lexer.TokenType.MINUS) {
            String op = current().text;
            pos++;
            left = Expr.binary(op, left, parseTerm());
        }
        return left;
    }

    private Expr parseTerm() {
        Expr left = parseFactor();
        while (current().type == lexer.TokenType.STAR
                || current().type == lexer.TokenType.SLASH) {
            String op = current().text;
            pos++;
            left = Expr.binary(op, left, parseFactor());
        }
        return left;
    }

    private Expr parseFactor() {
        lexer.Token t = current();
        switch (t.type) {
            case Number:
                pos++;
                return Expr.number(t.text);
            case Var:
                pos++;
                return Expr.name(t.text);
            default:
                error("Expected number or variable");
                return null;
        }
    }

    List<Stmt> parseProgram() {
        List<Stmt> program = new ArrayList<>();
        while (true) {
            while (match(lexer.TokenType.NEWLINE)) { }
            if (current().type == lexer.TokenType.EOF) break;
            program.add(parseStatement());
        }
        return program;
    }
     public static void main(String[] args) {
        String[] programs = {
            "let age = 20\nlet score = age + 10\nprint score\n",
            "x = 1 + 2 * 3",
            "if x = 1\nprint x\nelse\nprint 0\nend",
            "while x = 0\nx = x + 1\nend",
            "let = 5",
            "if x = 1\nprint x\n"
        };
 
        for (String source : programs) {
            System.out.println("---- source ----");
            System.out.println(source);
            try {
                List<lexer.Token> tokens = new lexer.Lexer(source).scan();
                List<Stmt> program = new parser(tokens).parseProgram();
 
                for (Stmt s : program) {
                    System.out.print(s.kind);
                    if (s.name != null) System.out.print(" name=" + s.name);
                    if (s.expr != null) {
                        System.out.print(" expr=" + s.expr.kind + "(" + s.expr.value + ")");
                        if (s.expr.kind == ExprKind.BINARY) {
                            System.out.print(" left=" + s.expr.left.value
                                    + " right=" + s.expr.right.kind + "(" + s.expr.right.value + ")");
                        }
                    }
                    if (s.condition != null) System.out.print(" condition=" + s.condition.value);
                    if (s.kind == StmtKind.IF || s.kind == StmtKind.WHILE) {
                        System.out.print(" body=" + s.body.size() + " stmts, else=" + s.elseBody.size() + " stmts");
                    }
                    System.out.println();
                }
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}