package com.nibras.nova.engine

/** Pure helpers for "Request desktop site", modelled on Chrome for Android. */
object DesktopMode {
    const val DESKTOP_VIEWPORT_WIDTH = 1024
    private const val FALLBACK_CHROME = "131.0.0.0"
    private val CHROME_VER = Regex("""Chrome/([\d.]+)""")

    fun chromeVersion(mobileUa: String?): String =
        mobileUa?.let { CHROME_VER.find(it)?.groupValues?.get(1) } ?: FALLBACK_CHROME

    /** Same shape Chrome sends in desktop mode: Linux x86_64, no "Mobile", no Android / "wv" / Version tokens. */
    fun desktopUserAgent(mobileUa: String?): String =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${chromeVersion(mobileUa)} Safari/537.36"

    fun isMobileUserAgent(ua: String): Boolean =
        ua.contains("Mobile") || ua.contains("Android") || Regex("""; wv\)""").containsMatchIn(ua)

    /** Runs at document start: makes JS-based detection see a desktop and widens the layout viewport. */
    fun documentStartScript(desktopUa: String): String {
        val ua = desktopUa.replace("\\", "\\\\").replace("'", "\\'")
        val major = chromeVersion(desktopUa).substringBefore('.')
        return """
(function(){try{
var UA='$ua',W=$DESKTOP_VIEWPORT_WIDTH;
function def(o,k,v){try{Object.defineProperty(o,k,{get:function(){return v},configurable:true});}catch(e){}}
var N=Navigator.prototype;
def(N,'userAgent',UA);def(N,'appVersion',UA.replace(/^Mozilla\//,''));def(N,'platform','Linux x86_64');def(N,'maxTouchPoints',0);
var brands=[{brand:'Chromium',version:'$major'},{brand:'Google Chrome',version:'$major'},{brand:'Not?A_Brand',version:'99'}];
var uad={brands:brands,mobile:false,platform:'Linux',
 getHighEntropyValues:function(h){return Promise.resolve({brands:brands,mobile:false,platform:'Linux',platformVersion:'',architecture:'x86',bitness:'64',model:'',uaFullVersion:'${chromeVersion(desktopUa)}',fullVersionList:brands});},
 toJSON:function(){return {brands:brands,mobile:false,platform:'Linux'};}};
def(N,'userAgentData',uad);
try{if(window.matchMedia){var mm=window.matchMedia.bind(window);window.matchMedia=function(q){
 if(/\((any-)?pointer:\s*coarse\)|\((any-)?hover:\s*none\)/.test(q)){var r=mm('(min-width:99999px)');return r;}
 if(/\((any-)?pointer:\s*fine\)|\((any-)?hover:\s*hover\)/.test(q)){return mm('all');}
 return mm(q);};}}catch(e){}
function fix(m){if(m&&m.name&&m.name.toLowerCase()==='viewport'){var c='width='+W;if(m.getAttribute('content')!==c)m.setAttribute('content',c);}}
function scan(){var l=document.querySelectorAll('meta[name=viewport],meta[name=Viewport]');for(var i=0;i<l.length;i++)fix(l[i]);}
new MutationObserver(function(ms){for(var i=0;i<ms.length;i++){var a=ms[i].addedNodes;for(var j=0;j<a.length;j++){if(a[j].tagName==='META')fix(a[j]);}if(ms[i].type==='attributes')fix(ms[i].target);}})
 .observe(document,{childList:true,subtree:true,attributes:true,attributeFilter:['content']});
scan();document.addEventListener('DOMContentLoaded',scan);
}catch(e){}})();
""".trim()
    }
}
