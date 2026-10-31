package com.nibras.nova.core.halal

/** Page-side part of Halal mode (pure string; see HalalImageFilter.TAP_JS). */
object HalalScript {
    val JS = """
(function(){if(window.__novaHalal)return;window.__novaHalal=1;
var B=window.NovaHalal;if(!B||!B.enabled())return;
var st=document.createElement('style');
st.textContent='img:not(.nova-ok),video:not(.nova-ok),picture:not(.nova-ok) img,[data-nova-bg]:not(.nova-ok),svg image{filter:blur(24px)!important;transition:filter .2s}'+
 'img.nova-ok,video.nova-ok,[data-nova-bg].nova-ok{filter:none!important}';
(document.head||document.documentElement).appendChild(st);
function src(el){return el.currentSrc||el.src||''}
function bgUrl(el){try{var b=getComputedStyle(el).backgroundImage;var m=b&&b.match(/url\(["']?([^"')]+)["']?\)/);return m?m[1]:''}catch(e){return ''}}
function abs(u){try{return new URL(u,location.href).href}catch(e){return u}}
function check(el){
 if(el.tagName==='IMG'){
  if(el.complete&&el.naturalWidth&&el.naturalWidth<48&&el.naturalHeight<48){el.classList.add('nova-ok');return}
  var s=src(el);if(!s)return;
  if(B.verdict(s)==='s')el.classList.add('nova-ok');else el.classList.remove('nova-ok');
 }else if(el.tagName==='VIDEO'){/* videos/posters stay blurred until tapped */}
 else{var u=bgUrl(el);if(u&&u.indexOf('data:')!==0){var a=abs(u);el.setAttribute('data-nova-bg',a);if(B.verdict(a)==='s')el.classList.add('nova-ok')}}
}
function scan(root){
 if(!root.querySelectorAll)return;
 var l=root.querySelectorAll('img,video');for(var i=0;i<l.length;i++)check(l[i]);
 var all=root.querySelectorAll('[style*="background"],div,a,span,section,figure,li');
 for(var j=0;j<all.length&&j<1500;j++){if(!all[j].hasAttribute('data-nova-bg'))check(all[j])}
}
document.addEventListener('load',function(e){var t=e.target;if(t&&t.tagName==='IMG')check(t)},true);
new MutationObserver(function(ms){for(var i=0;i<ms.length;i++){var m=ms[i];
 if(m.type==='attributes'){check(m.target)}else for(var k=0;k<m.addedNodes.length;k++){var n=m.addedNodes[k];if(n.nodeType===1){check(n);scan(n)}}}})
 .observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['src','srcset','data-src','style','poster']});
document.addEventListener('DOMContentLoaded',function(){scan(document)});
setInterval(function(){scan(document)},1500);
function target(t){while(t&&t!==document.body){if((t.tagName==='IMG'||t.tagName==='VIDEO'||t.hasAttribute&&t.hasAttribute('data-nova-bg'))&&!t.classList.contains('nova-ok'))return t;t=t.parentElement}return null}
document.addEventListener('click',function(e){var t=target(e.target);if(!t)return;e.preventDefault();e.stopPropagation();
 var s=t.tagName==='IMG'?src(t):t.tagName==='VIDEO'?(t.poster||src(t)||location.href):t.getAttribute('data-nova-bg');
 t.setAttribute('data-nova-want',s);B.requestReveal(s);},true);
window.__novaReveal=function(s){var l=document.querySelectorAll('[data-nova-want]');for(var i=0;i<l.length;i++){var m=l[i];if(m.getAttribute('data-nova-want')!==s)continue;
 m.removeAttribute('data-nova-want');m.classList.add('nova-ok');
 if(m.tagName==='IMG'&&B.isBlurred(s)){m.removeAttribute('srcset');m.src=s+(s.indexOf('?')<0?'?':'&')+'nova_reveal=1'}}};
})();
""".trimIndent()
}
