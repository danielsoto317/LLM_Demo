package com.dsv.llm_demo.util

object CodeExtractor {

    /**
     * Extracts HTML/JS/CSS code from standard Markdown code blocks.
     * Returns a fully self-contained HTML string, or null if no runnable web code is found.
     */
    fun extractWebCode(markdownText: String): String? {
        // Regex to match html, htm, xml, js, or javascript code fences
        val codeFenceRegex = Regex(
            "```(?:html|htm|javascript|js|jsx)?\\s*\\n([\\s\\S]*?)```",
            RegexOption.IGNORE_CASE
        )

        val matches = codeFenceRegex.findAll(markdownText).map { it.groupValues[1].trim() }.toList()

        if (matches.isEmpty()) return null

        val combinedCode = matches.joinToString("\n\n")

        // If it's already a full HTML document
        if (combinedCode.contains("<html", ignoreCase = true) || combinedCode.contains("<!DOCTYPE", ignoreCase = true)) {
            return combinedCode
        }

        // If it's snippet HTML or pure JavaScript, wrap it inside a complete HTML boilerplate
        return buildHtmlDocument(combinedCode)
    }

    private fun buildHtmlDocument(codeSnippet: String): String {
        val isJsOnly = !codeSnippet.trim().startsWith("<")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        margin: 0;
                        padding: 16px;
                        background-color: #ffffff;
                        color: #000000;
                    }
                </style>
                <!-- Include React, ReactDOM, and Babel CDNs so React/JSX single-file code works automatically -->
                <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
                <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
                <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body>
                <div id="root"></div>
                ${if (isJsOnly) "<script type=\"text/babel\">$codeSnippet</script>" else codeSnippet}
            </body>
            </html>
        """.trimIndent()
    }
}