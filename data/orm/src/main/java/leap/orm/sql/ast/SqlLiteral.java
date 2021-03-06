/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package leap.orm.sql.ast;

import leap.orm.sql.parser.Token;

public class SqlLiteral extends SqlToken {

    private final String literal;

    public SqlLiteral(Token token, String text, String literal) {
        super(token, text);

        this.literal = literal;
    }

    public String getLiteral() {
        return literal;
    }

    public Object getValue() {

        if(token == Token.LITERAL_CHARS) {
            return literal;
        }

        if(token == Token.LITERAL_INT) {
            return Integer.parseInt(literal);
        }

        if(token == Token.LITERAL_FLOAT) {
            return Float.parseFloat(literal);
        }

        throw new IllegalStateException("Unsupported literal '" + literal + "'");
    }
}
