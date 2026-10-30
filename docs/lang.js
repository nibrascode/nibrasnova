(function(){var q=new URLSearchParams(location.search).get('lang');var l=q||localStorage.getItem('nova-lang')||((navigator.language||'az').slice(0,2)==='az'?'az':'en');if(l!=='az'&&l!=='en')l='en';
function set(x){document.body.className=x;localStorage.setItem('nova-lang',x);document.documentElement.lang=x;document.querySelectorAll('.pill[data-set]').forEach(function(b){b.classList.toggle('on',b.dataset.set===x)});}
document.addEventListener('DOMContentLoaded',function(){document.querySelectorAll('.pill[data-set]').forEach(function(b){b.onclick=function(){set(b.dataset.set)}});set(l)});})();
