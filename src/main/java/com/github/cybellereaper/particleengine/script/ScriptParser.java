package com.github.cybellereaper.particleengine.script;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for PEScript. Consumes the token stream produced
 * by {@link ScriptLexer} and returns a {@link ScriptNode.Program} AST root.
 *
 * <p>Grammar (informal):
 * <pre>
 * program        := declaration* EOF
 * declaration    := letDecl | fnDecl | statement
 * letDecl        := "let" IDENT ("=" expression)? ";"
 * fnDecl         := "fn" IDENT "(" params? ")" block
 * statement      := ifStmt | whileStmt | forStmt | returnStmt | waitStmt
 *                 | breakStmt | continueStmt | block | expressionStmt
 * ifStmt         := "if" "(" expression ")" statement ("else" statement)?
 * whileStmt      := "while" "(" expression ")" statement
 * forStmt        := "for" "(" IDENT "in" expression ")" statement
 * returnStmt     := "return" expression? ";"
 * waitStmt       := "wait" expression ";"
 * breakStmt      := "break" ";"
 * continueStmt   := "continue" ";"
 * block          := "{" declaration* "}"
 * expressionStmt := expression ";"
 *
 * expression     := assignment
 * assignment     := (call ".")? IDENT "=" assignment | logicOr
 * logicOr        := logicAnd ("||" logicAnd)*
 * logicAnd       := equality ("&&" equality)*
 * equality       := comparison (("==" | "!=") comparison)*
 * comparison     := term (("&lt;" | "&lt;=" | "&gt;" | "&gt;=") term)*
 * term           := factor (("+" | "-") factor)*
 * factor         := unary (("*" | "/" | "%") unary)*
 * unary          := ("!" | "-") unary | call
 * call           := primary (("(" args? ")") | ("[" expression "]"))*
 * primary        := NUMBER | STRING | IDENT | "true" | "false" | "null"
 *                 | "(" expression ")" | "[" args? "]"
 * </pre>
 */
public final class ScriptParser {
    private final List<ScriptToken> tokens;
    private int current;

    public ScriptParser(List<ScriptToken> tokens) {
        this.tokens = tokens;
    }

    public ScriptNode.Program parse() {
        List<ScriptNode> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return new ScriptNode.Program(List.copyOf(statements));
    }

    private ScriptNode declaration() {
        if (match(ScriptTokenType.LET)) return letDeclaration();
        if (match(ScriptTokenType.FN)) return fnDeclaration();
        return statement();
    }

    private ScriptNode letDeclaration() {
        ScriptToken name = consume(ScriptTokenType.IDENTIFIER, "Expected variable name after 'let'.");
        ScriptNode initializer = null;
        if (match(ScriptTokenType.EQUAL)) {
            initializer = expression();
        }
        consume(ScriptTokenType.SEMICOLON, "Expected ';' after let declaration.");
        return new ScriptNode.Let(name.lexeme(), initializer, name.line());
    }

    private ScriptNode fnDeclaration() {
        ScriptToken name = consume(ScriptTokenType.IDENTIFIER, "Expected function name after 'fn'.");
        consume(ScriptTokenType.LEFT_PAREN, "Expected '(' after function name.");
        List<String> parameters = new ArrayList<>();
        if (!check(ScriptTokenType.RIGHT_PAREN)) {
            do {
                ScriptToken param = consume(ScriptTokenType.IDENTIFIER, "Expected parameter name.");
                parameters.add(param.lexeme());
            } while (match(ScriptTokenType.COMMA));
        }
        consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after function parameters.");
        consume(ScriptTokenType.LEFT_BRACE, "Expected '{' before function body.");
        ScriptNode.Block body = block(name.line());
        return new ScriptNode.Function(name.lexeme(), List.copyOf(parameters), body, name.line());
    }

    private ScriptNode statement() {
        if (match(ScriptTokenType.IF)) return ifStatement();
        if (match(ScriptTokenType.WHILE)) return whileStatement();
        if (match(ScriptTokenType.FOR)) return forStatement();
        if (match(ScriptTokenType.RETURN)) return returnStatement();
        if (match(ScriptTokenType.WAIT)) return waitStatement();
        if (match(ScriptTokenType.BREAK)) {
            ScriptToken token = previous();
            consume(ScriptTokenType.SEMICOLON, "Expected ';' after 'break'.");
            return new ScriptNode.Break(token.line());
        }
        if (match(ScriptTokenType.CONTINUE)) {
            ScriptToken token = previous();
            consume(ScriptTokenType.SEMICOLON, "Expected ';' after 'continue'.");
            return new ScriptNode.Continue(token.line());
        }
        if (match(ScriptTokenType.LEFT_BRACE)) return block(previous().line());
        return expressionStatement();
    }

    private ScriptNode ifStatement() {
        int line = previous().line();
        consume(ScriptTokenType.LEFT_PAREN, "Expected '(' after 'if'.");
        ScriptNode condition = expression();
        consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after if condition.");
        ScriptNode thenBranch = statement();
        ScriptNode elseBranch = null;
        if (match(ScriptTokenType.ELSE)) {
            elseBranch = statement();
        }
        return new ScriptNode.If(condition, thenBranch, elseBranch, line);
    }

    private ScriptNode whileStatement() {
        int line = previous().line();
        consume(ScriptTokenType.LEFT_PAREN, "Expected '(' after 'while'.");
        ScriptNode condition = expression();
        consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after while condition.");
        ScriptNode body = statement();
        return new ScriptNode.While(condition, body, line);
    }

    private ScriptNode forStatement() {
        int line = previous().line();
        consume(ScriptTokenType.LEFT_PAREN, "Expected '(' after 'for'.");
        ScriptToken variable = consume(ScriptTokenType.IDENTIFIER, "Expected loop variable name.");
        consume(ScriptTokenType.IN, "Expected 'in' after for variable.");
        ScriptNode iterable = expression();
        consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after for iterable.");
        ScriptNode body = statement();
        return new ScriptNode.ForEach(variable.lexeme(), iterable, body, line);
    }

    private ScriptNode returnStatement() {
        ScriptToken token = previous();
        ScriptNode value = null;
        if (!check(ScriptTokenType.SEMICOLON)) {
            value = expression();
        }
        consume(ScriptTokenType.SEMICOLON, "Expected ';' after return.");
        return new ScriptNode.Return(value, token.line());
    }

    private ScriptNode waitStatement() {
        ScriptToken token = previous();
        ScriptNode value = expression();
        consume(ScriptTokenType.SEMICOLON, "Expected ';' after wait.");
        return new ScriptNode.Wait(value, token.line());
    }

    private ScriptNode.Block block(int line) {
        List<ScriptNode> statements = new ArrayList<>();
        while (!check(ScriptTokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(ScriptTokenType.RIGHT_BRACE, "Expected '}' after block.");
        return new ScriptNode.Block(List.copyOf(statements), line);
    }

    private ScriptNode expressionStatement() {
        ScriptNode expr = expression();
        consume(ScriptTokenType.SEMICOLON, "Expected ';' after expression.");
        return new ScriptNode.ExpressionStatement(expr, expr.line());
    }

    private ScriptNode expression() {
        return assignment();
    }

    private ScriptNode assignment() {
        ScriptNode expr = logicOr();
        if (match(ScriptTokenType.EQUAL)) {
            ScriptToken equals = previous();
            ScriptNode value = assignment();
            if (expr instanceof ScriptNode.Identifier id) {
                return new ScriptNode.Assign(id.name(), value, id.line());
            }
            if (expr instanceof ScriptNode.Index idx) {
                return new ScriptNode.IndexAssign(idx.target(), idx.index(), value, idx.line());
            }
            throw new ScriptError("Invalid assignment target.", equals.line(), equals.column());
        }
        return expr;
    }

    private ScriptNode logicOr() {
        ScriptNode expr = logicAnd();
        while (match(ScriptTokenType.PIPE_PIPE)) {
            ScriptToken op = previous();
            ScriptNode right = logicAnd();
            expr = new ScriptNode.Logical(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode logicAnd() {
        ScriptNode expr = equality();
        while (match(ScriptTokenType.AMP_AMP)) {
            ScriptToken op = previous();
            ScriptNode right = equality();
            expr = new ScriptNode.Logical(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode equality() {
        ScriptNode expr = comparison();
        while (match(ScriptTokenType.BANG_EQUAL, ScriptTokenType.EQUAL_EQUAL)) {
            ScriptToken op = previous();
            ScriptNode right = comparison();
            expr = new ScriptNode.Binary(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode comparison() {
        ScriptNode expr = term();
        while (match(ScriptTokenType.GREATER, ScriptTokenType.GREATER_EQUAL, ScriptTokenType.LESS, ScriptTokenType.LESS_EQUAL)) {
            ScriptToken op = previous();
            ScriptNode right = term();
            expr = new ScriptNode.Binary(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode term() {
        ScriptNode expr = factor();
        while (match(ScriptTokenType.PLUS, ScriptTokenType.MINUS)) {
            ScriptToken op = previous();
            ScriptNode right = factor();
            expr = new ScriptNode.Binary(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode factor() {
        ScriptNode expr = unary();
        while (match(ScriptTokenType.STAR, ScriptTokenType.SLASH, ScriptTokenType.PERCENT)) {
            ScriptToken op = previous();
            ScriptNode right = unary();
            expr = new ScriptNode.Binary(expr, op.type(), right, op.line());
        }
        return expr;
    }

    private ScriptNode unary() {
        if (match(ScriptTokenType.BANG, ScriptTokenType.MINUS)) {
            ScriptToken op = previous();
            ScriptNode right = unary();
            return new ScriptNode.Unary(op.type(), right, op.line());
        }
        return call();
    }

    private ScriptNode call() {
        ScriptNode expr = primary();
        while (true) {
            if (match(ScriptTokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(ScriptTokenType.LEFT_BRACKET)) {
                ScriptToken bracket = previous();
                ScriptNode index = expression();
                consume(ScriptTokenType.RIGHT_BRACKET, "Expected ']' after index.");
                expr = new ScriptNode.Index(expr, index, bracket.line());
            } else {
                break;
            }
        }
        return expr;
    }

    private ScriptNode finishCall(ScriptNode callee) {
        ScriptToken open = previous();
        List<ScriptNode> arguments = new ArrayList<>();
        if (!check(ScriptTokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(ScriptTokenType.COMMA));
        }
        consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after call arguments.");
        return new ScriptNode.Call(callee, List.copyOf(arguments), open.line());
    }

    private ScriptNode primary() {
        ScriptToken token = peek();
        switch (token.type()) {
            case NUMBER, STRING -> {
                advance();
                return new ScriptNode.Literal(token.literal(), token.line());
            }
            case TRUE -> {
                advance();
                return new ScriptNode.Literal(Boolean.TRUE, token.line());
            }
            case FALSE -> {
                advance();
                return new ScriptNode.Literal(Boolean.FALSE, token.line());
            }
            case NULL -> {
                advance();
                return new ScriptNode.Literal(null, token.line());
            }
            case IDENTIFIER -> {
                advance();
                return new ScriptNode.Identifier(token.lexeme(), token.line());
            }
            case LEFT_PAREN -> {
                advance();
                ScriptNode expr = expression();
                consume(ScriptTokenType.RIGHT_PAREN, "Expected ')' after expression.");
                return expr;
            }
            case LEFT_BRACKET -> {
                advance();
                List<ScriptNode> elements = new ArrayList<>();
                if (!check(ScriptTokenType.RIGHT_BRACKET)) {
                    do {
                        elements.add(expression());
                    } while (match(ScriptTokenType.COMMA));
                }
                consume(ScriptTokenType.RIGHT_BRACKET, "Expected ']' after list literal.");
                return new ScriptNode.ListLiteral(List.copyOf(elements), token.line());
            }
            default -> throw new ScriptError("Expected expression but found '" + token.lexeme() + "'.", token.line(), token.column());
        }
    }

    private boolean match(ScriptTokenType... types) {
        for (ScriptTokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private ScriptToken consume(ScriptTokenType type, String message) {
        if (check(type)) return advance();
        ScriptToken token = peek();
        throw new ScriptError(message + " (got '" + token.lexeme() + "')", token.line(), token.column());
    }

    private boolean check(ScriptTokenType type) {
        if (isAtEnd()) return type == ScriptTokenType.EOF;
        return peek().type() == type;
    }

    private ScriptToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type() == ScriptTokenType.EOF; }
    private ScriptToken peek() { return tokens.get(current); }
    private ScriptToken previous() { return tokens.get(current - 1); }
}
