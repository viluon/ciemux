// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.javadoc

import com.sun.source.doctree.DocTree
import com.sun.source.doctree.TextTree
import com.sun.source.doctree.UnknownInlineTagTree
import com.sun.source.util.DocTreePath
import jdk.javadoc.doclet.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import kotlin.io.path.extension

/**
 * A primitive reimplementation of Java 21's `@snippet` tag. This only supports including external snippets via `file`
 * and `class`, and not the inline body.
 */
class SnippetTaglet : Taglet {
    override fun getName(): String = "snippet"
    override fun isInlineTag(): Boolean = true
    override fun getAllowedLocations(): Set<Taglet.Location> = locations

    private lateinit var env: DocletEnvironment
    private lateinit var reporter: Reporter
    private lateinit var snippetPath: List<File>

    override fun init(env: DocletEnvironment, doclet: Doclet) {
        super.init(env, doclet)
        this.env = env
        reporter = (doclet as StandardDoclet).reporter

        this.snippetPath =
            System.getProperty("cc.snippet-path")?.split(File.pathSeparatorChar)?.map { File(it) } ?: emptyList()
    }

    /** Parse our attributes into a key/value map */
    private fun parseAttributes(contents: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        val attributeMatcher = attribute.matcher(contents)
        var lastIndex = 0
        while (attributeMatcher.find()) {
            val key = attributeMatcher.group(1)
            val value = attributeMatcher.group(2)
            if (attributes.contains(key)) throw SnippetException("Duplicate attribute '$key'")
            attributes[key] = value
            lastIndex = attributeMatcher.end()
        }

        while (lastIndex < contents.length) {
            val c = contents[lastIndex]
            if (c != ' ') throw SnippetException("Unexpected '$c'")
        }

        return attributes
    }

    /** Locate our snippet file within the [snippetPath] */
    private fun findSnippetFile(fileName: String): Path = snippetPath.firstNotNullOfOrNull {
        val found = it.resolve(fileName)
        if (found.exists()) found.toPath() else null
    } ?: throw SnippetException("Cannot find file '$fileName'")

    private fun processInlineTag(tag: UnknownInlineTagTree): String {
        val tagContent = tag.content
        if (tagContent.size != 1 || tagContent[0].kind != DocTree.Kind.TEXT) throw SnippetException("Expected a single text node")
        val attributes = parseAttributes((tagContent[0] as TextTree).body)

        val hasFile = attributes.contains("file")
        val hasClass = attributes.contains("class")
        if (hasFile && hasClass) throw SnippetException("Cannot specify file and class")

        val file = when {
            hasFile -> findSnippetFile(attributes["file"]!!)
            hasClass -> findSnippetFile(attributes["class"]!!.replace('.', '/') + ".java")
            else -> throw SnippetException("Snippet has no contents (must have file or class)")
        }

        // And generate our snippet
        var snippetContents = Files.readString(file)

        val region = attributes["region"]
        if (region != null) {
            val matcher =
                Pattern.compile("// @start region=" + Pattern.quote(region) + "\n(.*)\\s*// @end region=" + Pattern.quote(region), Pattern.DOTALL)
                    .matcher(snippetContents)
            if (!matcher.find()) throw SnippetException("Cannot find region '$region'")
            snippetContents = matcher.group(1).trimIndent()
        }

        return makeSnippet(file.extension, snippetContents)
    }

    override fun toString(tags: List<DocTree>, element: Element): String {
        if (tags.size != 1) throw IllegalArgumentException("Tags should be length 1")
        val tag = tags[0] as UnknownInlineTagTree

        try {
            return processInlineTag(tag)
        } catch (e: SnippetException) {
            reporter.print(
                Diagnostic.Kind.ERROR,
                DocTreePath.getPath(env.docTrees.getPath(element), env.docTrees.getDocCommentTree(element), tag),
                "Invalid @snippet. ${e.message}",
            )
            return "@snippet"
        }
    }

    companion object {
        private val locations = EnumSet.allOf(Taglet.Location::class.java)
        private val attribute = Pattern.compile(" *([a-z]+) *= *([^ ]+)")

        /** Escape our snippet HTML and wrap it into a code block */
        private fun makeSnippet(extension: String, contents: String): String {
            val out = StringBuilder(contents.length + 60)
            out.append("<pre class=\"language language-$extension\"><code>")
            for (element in contents) {
                when (element) {
                    '<' -> out.append("&lt;")
                    '>' -> out.append("&gt;")
                    '&' -> out.append("&amp;")
                    else -> out.append(element)
                }
            }
            out.append("</code></pre>")
            return out.toString()
        }
    }
}

private class SnippetException(message: String) : Exception(message)
