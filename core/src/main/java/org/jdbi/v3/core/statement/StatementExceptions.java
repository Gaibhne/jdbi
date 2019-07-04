/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.statement;

import java.util.function.Function;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

/**
 * Configuration for {@link StatementException} and subclasses behavior.
 */
@Beta
public class StatementExceptions implements JdbiConfig<StatementExceptions> {

    private MessageRendering messageRendering;
    private int lengthLimit;

    public StatementExceptions() {
        messageRendering = MessageRendering.SHORT_STATEMENT;
        lengthLimit = 1024;
    }

    private StatementExceptions(StatementExceptions other) {
        messageRendering = other.messageRendering;
        lengthLimit = other.lengthLimit;
    }

    /**
     * @return the limit hint to use to shorten strings
     */
    public int getLengthLimit() {
        return lengthLimit;
    }

    /**
     * Set a hint on how long you'd like to shorten various variable-length strings to.
     * @param lengthLimit the limit hint
     * @return this
     */
    public StatementExceptions setLengthLimit(int lengthLimit) {
        this.lengthLimit = lengthLimit;
        return this;
    }

    /**
     * @return the statement exception message rendering strategy
     */
    public MessageRendering getMessageRendering() {
        return messageRendering;
    }

    /**
     * Configure exception statement message generation.
     * @param messageRendering the message rendering strategy to use
     * @return this
     */
    public StatementExceptions setMessageRendering(MessageRendering messageRendering) {
        this.messageRendering = messageRendering;
        return this;
    }

    @Override
    public StatementExceptions createCopy() {
        return new StatementExceptions(this);
    }

    /**
     * Control exception message generation.
     */
    public enum MessageRendering implements Function<StatementException, String> {
        /**
         * Do not include SQL or parameter information.
         */
        NONE {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return exc.getShortMessage();
            }
        },
        /**
         * Include bound parameters but not the SQL.
         */
        PARAMETERS {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return String.format("%s [arguments:%s]", exc.getShortMessage(), ctx.getBinding());
            }
        },
        /**
         * Include a length-limited SQL statement and parameter information.
         */
        SHORT_STATEMENT {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                final int limit = ctx.getConfig(StatementExceptions.class).getLengthLimit();
                return String.format("%s [statement:\"%s\", arguments:%s]",
                            exc.getShortMessage(),
                            limit(ctx.getRenderedSql(), limit),
                            limit(ctx.getBinding().toString(), limit));
            }

        },
        /**
         * Include all detail.
         */
        DETAIL {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return String.format("%s [statement:\"%s\", rewritten:\"%s\", parsed:\"%s\", arguments:%s]",
                            exc.getShortMessage(),
                            ctx.getRawSql(),
                            ctx.getRenderedSql(),
                            ctx.getParsedSql(),
                            ctx.getBinding());
            }
        };

        @Override
        public String apply(StatementException exc) {
            final StatementContext ctx = exc.getStatementContext();
            if (ctx == null) {
                return NONE.render(exc, null);
            }
            return render(exc, ctx);
        }

        protected abstract String render(StatementException exc, StatementContext ctx);
    }

    private static String limit(String s, int len) {
        return s.substring(0, Math.min(len, s.length()));
    }
}
