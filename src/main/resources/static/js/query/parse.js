const WORDS = new Set(["AND", "OR", "NOT"]);
const SYMBOLS = ["!=", "!~", ">=", "<=", "=", "~", ">", "<"];

export function parseQuery(input) {
    const parser = new Parser(tokenize(input));
    const query = parser.parseOr();
    if (parser.peek()) {
        throw new Error("Unexpected token '" + parser.peek().value + "'");
    }
    return query;
}

function tokenize(input) {
    const tokens = [];
    let index = 0;
    while (index < input.length) {
        const character = input[index];
        if (/\s/.test(character)) {
            index += 1;
            continue;
        }
        if (character === "(" || character === ")" || character === ",") {
            tokens.push({ kind: character, value: character });
            index += 1;
            continue;
        }
        if (character === "'" || character === "\"") {
            const quote = character;
            let value = "";
            index += 1;
            while (index < input.length && input[index] !== quote) {
                value += input[index];
                index += 1;
            }
            if (index >= input.length) {
                throw new Error("Unclosed quote");
            }
            tokens.push({ kind: "value", value });
            index += 1;
            continue;
        }
        const pair = input.slice(index, index + 2);
        if (SYMBOLS.includes(pair)) {
            tokens.push({ kind: "op", value: pair });
            index += 2;
            continue;
        }
        if (SYMBOLS.includes(character)) {
            tokens.push({ kind: "op", value: character });
            index += 1;
            continue;
        }
        const match = /^[A-Za-z0-9_.:@+-]+/.exec(input.slice(index));
        if (!match) {
            throw new Error("Unexpected character '" + character + "'");
        }
        const word = match[0];
        const upper = word.toUpperCase();
        if (WORDS.has(upper)) {
            tokens.push({ kind: "kw", value: upper });
        } else if (upper === "IN") {
            tokens.push({ kind: "op", value: "IN" });
        } else {
            tokens.push({ kind: "value", value: word });
        }
        index += word.length;
    }
    return tokens;
}

class Parser {
    constructor(tokens) {
        this.tokens = tokens;
        this.index = 0;
    }

    peek() {
        return this.tokens[this.index];
    }

    take() {
        const token = this.peek();
        this.index += 1;
        return token;
    }

    parseOr() {
        let left = this.parseAnd();
        const children = [left];
        while (this.peek()?.kind === "kw" && this.peek().value === "OR") {
            this.take();
            children.push(this.parseAnd());
        }
        return children.length === 1 ? left : { type: "or", children };
    }

    parseAnd() {
        let left = this.parseNot();
        const children = [left];
        while (this.peek()?.kind === "kw" && this.peek().value === "AND") {
            this.take();
            children.push(this.parseNot());
        }
        return children.length === 1 ? left : { type: "and", children };
    }

    parseNot() {
        if (this.peek()?.kind === "kw" && this.peek().value === "NOT") {
            this.take();
            return { type: "not", child: this.parseNot() };
        }
        return this.parsePrimary();
    }

    parsePrimary() {
        if (this.peek()?.kind === "(") {
            this.take();
            const query = this.parseOr();
            if (this.peek()?.kind !== ")") {
                throw new Error("Expected ')'");
            }
            this.take();
            return query;
        }
        return this.parseClause();
    }

    parseClause() {
        const field = this.take();
        const operator = this.take();
        if (!field || field.kind !== "value") {
            throw new Error("Expected a field name");
        }
        if (!operator || operator.kind !== "op") {
            throw new Error("Expected an operator");
        }
        return {
            type: "clause",
            field: field.value,
            operator: operator.value,
            value: operator.value === "IN" ? this.parseList() : this.parseValue()
        };
    }

    parseList() {
        if (this.peek()?.kind !== "(") {
            return this.parseValue();
        }
        this.take();
        const values = [];
        while (this.peek() && this.peek().kind !== ")") {
            if (this.peek().kind === ",") {
                this.take();
                continue;
            }
            values.push(this.parseValue());
        }
        if (this.peek()?.kind !== ")") {
            throw new Error("Expected ')'");
        }
        this.take();
        if (values.length === 0) {
            throw new Error("IN requires a value");
        }
        return values;
    }

    parseValue() {
        const token = this.take();
        if (!token || token.kind !== "value") {
            throw new Error("Expected a value");
        }
        return token.value;
    }
}
