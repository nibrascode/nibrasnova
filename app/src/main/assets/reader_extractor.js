(function() {
    try {
        var title = document.title || "";
        var metaAuthor = document.querySelector('meta[name="author"], meta[property="article:author"], meta[name="byl"]');
        var byline = metaAuthor ? metaAuthor.getAttribute("content") : "";
        var siteNameMeta = document.querySelector('meta[property="og:site_name"]');
        var siteName = siteNameMeta ? siteNameMeta.getAttribute("content") : window.location.hostname;

        var candidates = document.querySelectorAll("article, [role='main'], main, .post-content, .article-body, .entry-content, #content");
        var rootNode = null;
        var maxTextLen = 0;

        for (var i = 0; i < candidates.length; i++) {
            var len = (candidates[i].innerText || "").trim().length;
            if (len > maxTextLen) {
                maxTextLen = len;
                rootNode = candidates[i];
            }
        }
        if (!rootNode || maxTextLen < 250) {
            rootNode = document.body;
        }

        var paragraphs = rootNode ? rootNode.querySelectorAll("h1, h2, h3, p, blockquote, pre, li") : [];
        var blocks = [];
        var plainBuilder = [];

        for (var j = 0; j < paragraphs.length; j++) {
            var el = paragraphs[j];
            var tag = el.tagName.toLowerCase();
            var text = (el.innerText || "").replace(/\s+/g, " ").trim();
            if (text.length < 25 && tag === "p") continue;
            if (text.length === 0) continue;
            blocks.push({ tag: tag, text: text });
            plainBuilder.push(text);
        }

        var fullText = plainBuilder.join("\n\n");
        var wordCount = fullText.split(/\s+/).filter(Boolean).length;
        var estimatedMinutes = Math.max(1, Math.round(wordCount / 200));

        return JSON.stringify({
            isReadable: blocks.length >= 3 && wordCount >= 80,
            title: title,
            byline: byline || siteName,
            siteName: siteName,
            url: window.location.href,
            wordCount: wordCount,
            estimatedReadMinutes: estimatedMinutes,
            blocks: blocks,
            plainText: fullText
        });
    } catch (e) {
        return JSON.stringify({
            isReadable: false,
            title: document.title || "",
            byline: "",
            siteName: window.location.hostname || "",
            url: window.location.href || "",
            wordCount: 0,
            estimatedReadMinutes: 1,
            blocks: [],
            plainText: ""
        });
    }
})();
