// --- helpers ---

// Returns a random integer between min (included) and max (excluded)
// Using Math.round() will give you a non-uniform distribution!
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}


$(document).ready(function(){
	
  // --- visions ---
  
  $('.vision-statements').slick({
    infinite: true,
    dots: true,
    autoplay: true,
    autoplaySpeed: 8000,
    arrows: false
  });
  
  
  // --- map ---
  
  var table = $('table[about="#users-by-country"]'),
      map = $('#worldmap'),
      json = JSON.parse(table.find('script').html()),
      data = {};

  for (i in json.entries) {
    data[json.entries[i].key.toUpperCase()] = json.entries[i].value;
  }

  map.vectorMap({
    backgroundColor: '#0c75bf',
    zoomButtons: false,
    zoomOnScroll: false,
    series: {
      regions: [{
        values: data,
        scale: ['#cfdfba', '#a1cd3f'],
        normalizeFunction: 'linear'
      }]
    },
    onRegionTipShow: function(e, el, code){
      var country_champion = false;
      var users_registered = false;
      
      if(
        $('ul[about="#country-champions"] li[data-country-code="' + code + '"]').length
      ) {
        country_champion = true;
      }
      
      if(
        typeof data[code] != 'undefined'
      ) {
        users_registered = true;
      }
      
      el.html(
        (
          users_registered
          ?
          '<strong>' + data[code] + '</strong> users registered in ' + el.html() + ' (Click to register ...)<br>'
          :
          'No users registered in ' + el.html() + ' (Click to register ...)<br>'
        ) + (
          country_champion
          ?
          'And we have a country champion!<br>'
          :
          ''
        )
      );
    }
  });
  table.hide()
  
  // --- hijax behavior ---
  hijax($('body'));
	
});

function hijax(element) {

  $('a.hijax.transclude', element).each(function() {
    var a = $(this);
    $.get(a.attr('href'))
      .done(function(data) {
        a.replaceWith(hijax(body(data)));
      })
      .fail(function(jqXHR) {
        a.replaceWith(hijax(body(jqXHR.responseText)));
      });
  });

  $('form', element).submit(function() {
    var form = $(this);
    var action = form.attr('action');
    var method = form.attr('method');
    $.ajax({type: method, url: action, data: form.serialize()})
      .done(function(data) {
        form.replaceWith(hijax(body(data)));
      })
      .fail(function(jqXHR) {
        form.replaceWith(hijax(body(jqXHR.responseText)));
       });
    return false;
  });

  return element;

}

function body(data) {
  return $(data.match(/<\s*body.*>[\s\S]*<\s*\/body\s*>/ig).join(""))
}

