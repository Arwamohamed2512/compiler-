
import java.util.ArrayList;
import java.util.List;

public class lexer {
    enum TokenType {
        let, print, IF, Else, End, While,
        Var, Number,
        EQUALS, PLUS, MINUS, STAR, SLASH,
        NEWLINE, EOF,
    }

    static final class Token {
        final TokenType type;
        final String text;
        final int line;
        final int column;

        Token(TokenType type, String text, int line, int column) {
            this.type = type;
            this.text = text;
            this.line = line;
            this.column = column;
        }

        @Override
        public String toString() {
            return type + "(" + text + ")@" + line + ":" + column;
        }
    }

    static final class Lexer {
        private final String source;
        private int pos = 0;
        private int line = 1;
        private int column = 1;
        private final List<Token> tokens = new ArrayList<>();

        Lexer(String source) {
            this.source = source;
        }

        /** Return the current character, or '\0' at end of source. */
        private char current() {
            if (pos >= source.length()) {
                return '\0';
            }

            return source.charAt(pos);
        }

        /** Look at the next character without consuming it. */
        private char peek() {
            if (pos + 1 >= source.length()) {
                return '\0';
            }

            return source.charAt(pos + 1);
        }

        /** Move one character forward and update line/column. */
        private void advance() {
            if (current() == '\0') {
                return;
            }

            if (current() == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }

            pos++;
        }

        /** Add one token with its source location. */
        private void add(TokenType type, String text,
                int atLine, int atColumn) {

            tokens.add(new Token(type, text, atLine, atColumn));
        }

        /** Report a lexical error with line and column. */
        private void error(String message, int atLine, int atColumn) {
            throw new RuntimeException(
                    "Lexical Error at line " + atLine +
                            ", column " + atColumn + ": " + message);
        }

        /** Tokenize the whole source and append the final EOF token. */
        List<Token> scan() {

            while (current() != '\0') {

                char c = current();

                int tokenLine = line;
                int tokenColumn = column;

                // 1. Ignore spaces and tabs
                if (c == ' ' || c == '\t' || c == '\r') {
                    advance();
                    continue;
                }

                // 2. Handle new lines
                if (c == '\n') {
                    add(TokenType.NEWLINE, "\n", tokenLine, tokenColumn);
                    advance();
                    continue;
                }

                // 3. Handle numbers
                if (Character.isDigit(c)) {

                    int start = pos;

                    while (Character.isDigit(current())) {
                        advance();
                    }

                    String number = source.substring(start, pos);

                    add(TokenType.Number, number, tokenLine, tokenColumn);
                    continue;
                }

                // 4. Handle identifiers and keywords
                if (Character.isLetter(c) || c == '_') {

                    int start = pos;

                    while (Character.isLetterOrDigit(current())
                            || current() == '_') {
                        advance();
                    }

                    String word = source.substring(start, pos);

                    TokenType type;

                    switch (word) {
                        case "let":
                            type = TokenType.let;
                            break;

                        case "print":
                            type = TokenType.print;
                            break;

                        case "if":
                            type = TokenType.IF;
                            break;

                        case "else":
                            type = TokenType.Else;
                            break;

                        case "while":
                            type = TokenType.While;
                            break;

                        case "end":
                            type = TokenType.End;
                            break;

                        default:
                            type = TokenType.Var;
                    }

                    add(type, word, tokenLine, tokenColumn);
                    continue;
                }

                // 5. Handle operators and symbols
                switch (c) {
                      case '=':
                       
                            advance();
                            add(TokenType.EQUALS, "=", tokenLine, tokenColumn);
                       
                        break;

                    case '+':
                        advance();
                        add(TokenType.PLUS, "+", tokenLine, tokenColumn);
                        break;

                    case '-':
                        advance();
                        add(TokenType.MINUS, "-", tokenLine, tokenColumn);
                        break;

                    case '*':
                        advance();
                        add(TokenType.STAR, "*", tokenLine, tokenColumn);
                        break;

                  
                    default:
                        error("Unexpected character '" + c + "'",
                                tokenLine, tokenColumn);
                }
            }

            // 6. Add the final EOF token
            add(TokenType.EOF, "", line, column);

            return tokens;
        }

    }

    public static void main(String[] args) {

        String source = "let age = 20\n" +
                "let score = age + 10\n" +
                "print score\n";

        Lexer lexer = new Lexer(source);

        List<Token> tokens = lexer.scan();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }

}