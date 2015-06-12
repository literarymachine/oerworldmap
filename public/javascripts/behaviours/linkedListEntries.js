Hijax.behaviours.linkedListEntries = {

  attach : function(context) {
    var that = this;

    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("click", "li", function(){
        window.location = $( this ).find("h1 a").attr("href");
      });
    });

    $('[data-behaviour="linkedListEntries"]', context).each(function(){
      $( this ).on("mouseenter", "li", function() {
        var id = this.getAttribute("about");
        var json = JSON.parse( $(this).closest("ul").children('script[type="application/ld+json"]').html() );
        var resource = json.filter(function(resource) {
          return resource['@id'] == id;
        })[0];
        var markers = Hijax.behaviours.map.getMarkers(resource);
        for (var i = 0; i < markers.length; i++) {
          Hijax.behaviours.map.world.getLayers().forEach(function(layer) {
            var marker = layer.getSource().getFeatureById(markers[i]['id']);
            if (marker) {
              var style = marker.getStyle();
              style.getText().setFont('normal 3em FontAwesome');
              marker.setStyle(style);
            }
          });
        }
      });
      $( this ).on("mouseleave", "li", function() {
        var id = this.getAttribute("about");
        var json = JSON.parse( $(this).closest("ul").children('script[type="application/ld+json"]').html() );
        var resource = json.filter(function(resource) {
          return resource['@id'] == id;
        })[0];
        var markers = Hijax.behaviours.map.getMarkers(resource);
        for (var i = 0; i < markers.length; i++) {
          Hijax.behaviours.map.world.getLayers().forEach(function(layer) {
            var marker = layer.getSource().getFeatureById(markers[i]['id']);
            if (marker) {
              var style = marker.getStyle();
              style.getText().setFont('normal 1.5em FontAwesome');
              marker.setStyle(style);
            }
          });
        }
      });
    });
  }

};
