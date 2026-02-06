package top.fpsmaster.ui.devspace;

public class HighlightLexer {
    private static final String KEYWORDS = "\\b(function|local|if|then|else|elseif|end|for|while|do|repeat|until|return|break|in|and|or|not)\\b";
    private static final String COMMENTS = "--.*"; // 单行注释
    private static final String STRINGS = "\"([^\\\"]|\\.)*\"|'([^\\']|\\.)*'"; // 字符串
    private static final String NUMBERS = "\\b\\d+(\\.\\d+)?\\b"; // 数字

    // 高亮方法
    public static String highlight(String code) {
        code = code.replaceAll(COMMENTS, "<{<COMMENT:$0>}>");
        code = code.replaceAll(STRINGS, "<{<STRING:$0>}>");
        code = code.replaceAll(KEYWORDS, "<{<KEYWORD:$0>}>");
        code = code.replaceAll(NUMBERS, "<{<NUMBER:$0>}>");

        return code;
    }
}
