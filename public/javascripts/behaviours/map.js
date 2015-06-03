Hijax.behaviours.map = {

  container : null,
  world : null,
  vector : null,

  attach : function(context) {
    var map = this;

    $('div[data-view="map"]', context).each(function() {

      // Map container
      map.container = $('<div id="map"></div>')[0];
      $(this).prepend(map.container);

      // Vector layer
      map.vector = new ol.layer.Vector({
        source: new ol.source.Vector({
      	  projection : 'EPSG:3857',
          url: '/assets/json/ne_50m_admin_0_countries_topo.json',
          format: new ol.format.TopoJSON(),
          noWrap: true,
          wrapX: false
        })
      });

      // Map object
      map.world = new ol.Map({
        layers: [map.vector],
        target: map.container,
        view: new ol.View({
          center: [0, 0],
          zoom: 2
        })
      });

    });

  },

  setHeatmapData : function(aggregation) {
    var map = this;

    var heat_data = {};

    for(var i = 0; i < aggregation["entries"].length; i++) {
      heat_data[ aggregation["entries"][i].key.toUpperCase() ] = aggregation["entries"][i].value;
    }

    var heats = $.map(heat_data, function(value, index) {
      return [value];
    });

    var color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats, .01), d3.quantile(heats, .99)]);

    map.vector.setStyle(function(feature) {
      return [new ol.style.Style({
        fill: new ol.style.Fill({
          color: heat_data[ feature["$"] ] ? color( heat_data[ feature["$"] ] ) : "#ffffff"
        }),
        stroke: new ol.style.Stroke({
          color: 'olive',
          width: 1
        })
      })];
    });
  },

  addPlacemarks : function(placemarks) {

    var map = this;

    var features = [];

    var iconStyle = new ol.style.Style({
      text: new ol.style.Text({
        text: '\uf041',
        font: 'normal 12px FontAwesome',
        textBaseline: 'Bottom',
        fill: new ol.style.Fill({
          color: 'red',
        })
      })
    });

    for (var i = 0; i < placemarks.length; i++) {
      var lat = placemarks[i].latLng[0];
      var lon = placemarks[i].latLng[1];
      var point = new ol.geom.Point(ol.proj.transform([lon, lat], 'EPSG:4326', 'EPSG:3857'));
      var feature = new ol.Feature({
        geometry: point,
        name: placemarks[i].name,
        url: placemarks[i].url
      });
      features.push(feature);
    }

    var vectorSource = new ol.source.Vector({
      features: features,
      noWrap: true,
      wrapX: false
    });

    var clusterSource = new ol.source.Cluster({
      distance: 40,
      source: vectorSource,
      noWrap: true,
      wrapX: false
    });

    var styleCache = {};
    var clusterLayer = new ol.layer.Vector({
      source: clusterSource,
      style: function(feature, resolution) {
        var size = feature.get('features').length;
        var style = styleCache[size];
        if (!style) {
          style = [new ol.style.Style({
            image: new ol.style.Circle({
              radius: 10,
              stroke: new ol.style.Stroke({
                color: '#fff'
              }),
              fill: new ol.style.Fill({
                color: '#3399CC'
              })
            }),
            text: new ol.style.Text({
              text: size.toString(),
              fill: new ol.style.Fill({
                color: '#fff'
              })
            })
          })];
          styleCache[size] = style;
        }
        return style;
      }
    });

    var vectorLayer = new ol.layer.Vector({
      source: vectorSource,
      style: iconStyle
    });

    map.world.addLayer(vectorLayer);
    //map.world.addLayer(clusterLayer);

  },

  getMarkers : function(resource, labelCallback, origin) {

    origin = origin || resource;
    var that = this;
    var locations = [];
    var markers = [];

    if (resource.location && resource.location instanceof Array) {
      locations = locations.concat(resource.location);
    } else if (resource.location) {
      locations.push(resource.location);
    }

    for (var l in locations) {
      if (geo = locations[l].geo) {
        markers.push({
          latLng: [geo['lat'], geo['lon']],
          name: labelCallback ? labelCallback(origin) : resource['@id'],
          url: "/resource/" + origin['@id']
        })
      }
    }

    if (!markers.length) {
      for (var key in resource) {
        var value = resource[key];
        if (value instanceof Array) {
          for (var i = 0; i < value.length; i++) {
            if (typeof value[i] == 'object') {
              markers = markers.concat(
                that.getMarkers(value[i], labelCallback, origin)
              );
            }
          }
        } else if (typeof value == 'object') {
          markers = markers.concat(
            that.getMarkers(value, labelCallback, origin)
          );
        }
      }
    }

    return markers;

  }

}
