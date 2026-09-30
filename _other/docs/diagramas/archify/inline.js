(function(){
  document.documentElement.setAttribute('data-theme','light');
  var svg=document.querySelector('.diagram-container svg');
  var clone=svg.cloneNode(true);
  var props=['fill','fill-opacity','stroke','stroke-width','stroke-dasharray','stroke-opacity','stroke-linecap','stroke-linejoin','opacity','font-family','font-size','font-weight','letter-spacing','text-anchor','dominant-baseline','display','visibility'];
  var o=svg.querySelectorAll('*'), c=clone.querySelectorAll('*');
  for(var i=0;i<o.length;i++){
    var cs=getComputedStyle(o[i]); var st=[];
    props.forEach(function(p){var v=cs.getPropertyValue(p); if(v) st.push(p+':'+v);});
    c[i].setAttribute('style',st.join(';'));
    c[i].removeAttribute('class');
  }
  clone.removeAttribute('class'); clone.setAttribute('xmlns','http://www.w3.org/2000/svg');
  ['style','data-preset','data-quality-profile','aria-labelledby','role','lang'].forEach(function(a){clone.removeAttribute(a);});
  return new XMLSerializer().serializeToString(clone);
})()
